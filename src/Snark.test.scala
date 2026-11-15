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
  test("has at least 10 replies that all contain the correct spelling") {
    assert(Snark.replies.size >= 10)
    assert(Snark.replies.forall(_.contains(Snark.Correct)))
  }
  test("rotates through all replies") {
    val n = Snark.replies.size
    val seen = (1 to n).map(_ => Snark.nextReply("@x")).toSet
    assertEquals(seen.size, n)
  }
