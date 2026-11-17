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
  test("has at least 10 replies that all contain the correct spelling") {
    assert(Snark.replies.size >= 10)
    assert(Snark.replies.forall(_.contains(Snark.Correct)))
  }
  test("rotates through all replies") {
    val n = Snark.replies.size
    val seen = (1 to n).map(_ => Snark.nextReply("@x")).toSet
    assertEquals(seen.size, n)
  }
