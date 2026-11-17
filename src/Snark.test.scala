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
  test("has at least 10 replies that all contain the correct spelling") {
    assert(Snark.replies.size >= 10)
    assert(Snark.replies.forall(_.contains(Snark.Correct)))
  }
  test("rotates through all replies") {
    val n = Snark.replies.size
    val seen = (1 to n).map(_ => Snark.nextReply("@x")).toSet
    assertEquals(seen.size, n)
  }
