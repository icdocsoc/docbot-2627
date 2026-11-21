//> using scala 3.9.0
//> using dep org.scoverage::scalac-scoverage-reporter:2.5.2

// Turns the scoverage data written by `scalac -coverage-out:<dir>` into reports. The Scala 3 compiler only emits the
// raw data (scoverage.coverage + scoverage.measurements.*), so this converts it to Cobertura XML (for the CI summary
// and PR comment) and a browsable HTML report.
//
// Lives under .github/ (a hidden directory) so that `scala-cli test|fmt|fix .` never treat it as project source.
// Usage: scala-cli run .github/coverage/Report.scala -- <data-dir> <source-root> <output-dir>

import java.io.File
import java.nio.file.Files

import scoverage.reporter.{CoberturaXmlWriter, CoverageAggregator, ScoverageHtmlWriter}

@main def report(dataDir: String, sourceRoot: String, outDir: String): Unit =
  val root = File(sourceRoot).getCanonicalFile
  val out = File(outDir)
  Files.createDirectories(File(out, "html").toPath)
  val coverage = CoverageAggregator
    .aggregate(Seq(File(dataDir)), root)
    .getOrElse(sys.error(s"no scoverage data in $dataDir"))
  CoberturaXmlWriter(Seq(root), out, None).write(coverage)
  ScoverageHtmlWriter(Seq(root), File(out, "html"), None).write(coverage)
  println(
    f"Statements: ${coverage.invokedStatementCount}/${coverage.statementCount} (${coverage.statementCoveragePercent}%.2f%%)"
  )
  println(f"Branches:   ${coverage.branchCoveragePercent}%.2f%%")
  println(s"Wrote ${File(out, "cobertura.xml")} and ${File(out, "html")}")
