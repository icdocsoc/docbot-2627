package docsoc

/** DoCSoc-specific entry points, kept for convenience. The general logic lives in `Corrections`. */
object Snark:

  val Correct: String = Corrections.DoCSoc.correct

  /** True if the text (ignoring URLs and invisible characters) contains a "docsoc" that isn't exactly "DoCSoc". */
  def misspelt(text: String): Boolean = Corrections.DoCSoc.misspelt(text)

  /** The reply templates (see `Quotes`). */
  def replies: Vector[String] = Quotes.templates

  /** A random reply (never the same twice in a row) correcting to **DoCSoc**, mentioning the offender. */
  def nextReply(mention: String): String = Corrections.DoCSoc.reply(mention)
