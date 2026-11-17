package docsoc

class CorrectionsTests extends munit.FunSuite:
  test("flags misspellings of ICHack") {
    for s <- List(
        "IC Hack", "ic hack", "IC-Hack", "IC_Hack", "ichack", "ICHACK", "Ic Hack", "I.C. Hack", "IC  Hack",
        "IC Hack 26", "ic-hack26", "come to ichack!", "ICHack and ichack", "see you at IC\tHack"
      )
    do assert(Corrections.ICHack.misspelt(s), s)
  }
  test("leaves correct ICHack and unrelated text alone") {
    for s <- List("ICHack", "ICHack26", "hello ICHack!", "ICHack 2026", "nothing to see", "") do
      assert(!Corrections.ICHack.misspelt(s), s)
  }
  test("ICHack is not flagged inside other words") {
    for s <- List("chic hack", "epic hack", "Epic Hack", "ichacks", "ICHacks", "ichacker", "topic-hack", "logic hack") do
      assert(!Corrections.ICHack.misspelt(s), s)
  }
  test("ICHack misspellings inside URLs are ignored, outside are not") {
    for s <- List("https://ichack.org", "<https://ichack.org/ic-hack>", "www.ichack.org", "HTTP://IC-HACK.example") do
      assert(!Corrections.ICHack.misspelt(s), s)
    for s <- List("ichack https://ichack.org", "<https://ichack.org> join ic hack") do
      assert(Corrections.ICHack.misspelt(s), s)
  }
  test("ICHack invisible characters are stripped first") {
    assert(Corrections.ICHack.misspelt("IC​ Hack"))
    assert(Corrections.ICHack.misspelt("ic​hack"))
    assert(Corrections.ICHack.misspelt("I­C-Hack"))
    assert(!Corrections.ICHack.misspelt("IC​Hack"))
    assert(!Corrections.ICHack.misspelt("https://ic​hack.org"))
  }
  test("corrections are independent") {
    assert(!Corrections.ICHack.misspelt("docsoc"))
    assert(!Corrections.DoCSoc.misspelt("ichack"))
  }
  test("firstMisspelt finds the first matching correction") {
    assertEquals(Corrections.firstMisspelt("DoCSoc and ICHack"), None)
    assertEquals(Corrections.firstMisspelt("ic hack"), Some(Corrections.ICHack))
    assertEquals(Corrections.firstMisspelt("docsoc"), Some(Corrections.DoCSoc))
    assertEquals(Corrections.firstMisspelt("ic hack and docsoc"), Some(Corrections.DoCSoc))
  }
  test("ICHack replies contain **ICHack** and never DoCSoc") {
    val rng = scala.util.Random(7)
    for _ <- 1 to 2000 do
      val r = Quotes.pick(s"**${Corrections.ICHack.correct}**", "<@1>", rng)
      assert(r.contains("**ICHack**"), r)
      assert(!r.contains("DoCSoc"), r)
  }
  test("reply uses the correction's own spelling") {
    for _ <- 1 to 200 do
      assert(Corrections.ICHack.reply("<@1>").contains("**ICHack**"))
      assert(!Corrections.ICHack.reply("<@1>").contains("DoCSoc"))
  }
  test("no template hard-codes a name") {
    assert(Quotes.templates.forall(!_.contains("DoCSoc")))
    assert(Quotes.templates.forall(!_.contains("ICHack")))
  }
