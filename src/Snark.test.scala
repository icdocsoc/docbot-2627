package docsoc

class SnarkTests extends munit.FunSuite:
  test("flags misspellings") {
    for s <- List("docsoc", "DOCSOC", "DoCSoC", "Docsoc", "join docsoc today!", "DoCSoc and docsoc") do
      assert(Snark.misspelt(s), s)
  }
  test("leaves correct spelling and unrelated text alone") {
    for s <- List("DoCSoc", "hello DoCSoc!", "nothing to see", "") do
      assert(!Snark.misspelt(s), s)
  }
  test("ignores misspellings inside URLs") {
    for s <- List(
        "https://github.com/icdocsoc",
        "http://x/docsoc",
        "www.docsoc.co.uk/path",
        "<https://github.com/icdocsoc>",
        "see HTTPS://DOCSOC.example/x for details"
      )
    do assert(!Snark.misspelt(s), s)
  }
  test("still flags a misspelling outside a URL") {
    for s <- List("docsoc https://github.com/icdocsoc", "<https://x/docsoc> join docsoc", "www.docsoc.co.uk DOCSOC") do
      assert(Snark.misspelt(s), s)
  }
  test("flags misspellings split by invisible characters") {
    for s <- List("doc\u200Bsoc", "Do\u200BCSoc", "d\u200Docsoc", "doc\u00ADsoc", "DOC\uFEFFSOC", "d\u2060o\u200Cc\u200Es\u202Eoc") do
      assert(Snark.misspelt(s), s)
  }
  test("does not flag a correct DoCSoc containing invisible characters") {
    for s <- List("Do\u200BCSoc", "D\u200Do\u200CC\u2060S\u00ADoc", "\uFEFFDoCSoc", "DoCSoc\u200B") do
      assert(!Snark.misspelt(s), s)
  }
  test("still ignores URLs containing invisible characters") {
    for s <- List("https://github.com/ic\u200Bdocsoc", "ht\u200Btps://x/docsoc", "www.doc\u200Bsoc.co.uk") do
      assert(!Snark.misspelt(s), s)
  }
  test("every character in Invisibles is stripped") {
    assertEquals(Snark.Invisibles.size, 18)
    for c <- Snark.Invisibles do assert(Snark.misspelt(s"doc${c}soc"), f"U+${c.toInt}%04X")
  }
  test("has plenty of quotes, all with a {correct} placeholder") {
    assert(Snark.replies.size >= 80)
    assert(Snark.replies.forall(_.contains("{correct}")))
  }
  test("no duplicate templates") {
    assertEquals(Quotes.templates.distinct.size, Quotes.templates.size)
  }
  test("pick fills in every placeholder") {
    val rng = scala.util.Random(1)
    for _ <- 1 to 2000 do
      val r = Quotes.pick("**DoCSoc**", "<@123>", rng)
      assert(!r.contains("{"), r)
      assert(!r.contains("}"), r)
      assert(r.contains("**DoCSoc**"), r)
  }
  test("nextReply contains the correct spelling") {
    for _ <- 1 to 200 do assert(Snark.nextReply("<@1>").contains("**DoCSoc**"))
  }
  test("never the same quote twice in a row") {
    for seed <- 1 to 5 do
      val rng = scala.util.Random(seed)
      val picks = (1 to 5000).map(_ => Quotes.pick("{correct}", "{user}", rng))
      assert(picks.zip(picks.tail).forall(_ != _))
  }
  test("every quote is reachable") {
    val rng = scala.util.Random(42)
    val seen = (1 to 20000).map(_ => Quotes.pick("{correct}", "{user}", rng)).toSet
    assertEquals(seen, Quotes.templates.toSet)
  }
