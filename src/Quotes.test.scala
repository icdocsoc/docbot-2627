package docsoc

class QuotesTests extends munit.FunSuite:
  test("no template hard-codes a name") {
    assert(Quotes.templates.forall(!_.contains("DoCSoc")))
    assert(Quotes.templates.forall(t => !t.contains("ICHack") && !t.contains("IC Hack")))
  }
  test("has plenty of quotes, all with a {correct} placeholder") {
    assert(Quotes.templates.size >= 80)
    assert(Quotes.templates.forall(_.contains("{correct}")))
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
