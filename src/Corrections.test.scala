package docsoc

class CorrectionsTests extends munit.FunSuite:
  test("flags every spelling of IC Hack except exactly IC Hack") {
    for s <- List(
        "ICHack", "ichack", "ICHACK", "ic hack", "Ic Hack", "IC  Hack", "IC-Hack", "ic-hack", "IC_Hack", "I.C. Hack",
        "ICHack26", "ICHack27", "IC hack", "IC HACK", "come to ichack!", "ICHack and ichack", "see you at IC\tHack",
        "IC Hack and ichack"
      )
    do assert(Corrections.ICHack.misspelt(s), s)
  }
  test("leaves exactly IC Hack and unrelated text alone") {
    for s <- List("IC Hack", "IC Hack 26", "IC Hack, ", "hello IC Hack!", "IC Hack 2026", "nothing to see", "") do
      assert(!Corrections.ICHack.misspelt(s), s)
  }
  test("IC Hack is not flagged inside other words") {
    for s <- List("chic hack", "epic hack", "Epic Hack", "ichacks", "ICHacks", "ichacker", "topic-hack", "logic hack") do
      assert(!Corrections.ICHack.misspelt(s), s)
  }
  test("IC Hack misspellings inside URLs are ignored, outside are not") {
    for s <- List("https://ichack.org", "<https://ichack.org/ic-hack>", "www.ichack.org", "HTTP://IC-HACK.example") do
      assert(!Corrections.ICHack.misspelt(s), s)
    for s <- List("ichack https://ichack.org", "<https://ichack.org> join ic hack") do
      assert(Corrections.ICHack.misspelt(s), s)
  }
  test("IC Hack invisible characters are stripped first") {
    assert(!Corrections.ICHack.misspelt("IC\u200B Hack"))
    assert(Corrections.ICHack.misspelt("ic\u200Bhack"))
    assert(Corrections.ICHack.misspelt("I\u00ADC-Hack"))
    assert(Corrections.ICHack.misspelt("IC\u200BHack"))
    assert(!Corrections.ICHack.misspelt("https://ic\u200Bhack.org"))
  }
  test("corrections are independent") {
    assert(!Corrections.ICHack.misspelt("docsoc"))
    assert(!Corrections.DoCSoc.misspelt("ichack"))
  }
  test("firstMisspelt finds the first matching correction") {
    assertEquals(Corrections.firstMisspelt("DoCSoc and IC Hack"), None)
    assertEquals(Corrections.firstMisspelt("ic hack"), Some(Corrections.ICHack))
    assertEquals(Corrections.firstMisspelt("ICHack26"), Some(Corrections.ICHack))
    assertEquals(Corrections.firstMisspelt("docsoc"), Some(Corrections.DoCSoc))
    assertEquals(Corrections.firstMisspelt("ic hack and docsoc"), Some(Corrections.DoCSoc))
  }
  test("ICHack replies contain **IC Hack** and never DoCSoc") {
    val rng = scala.util.Random(7)
    for _ <- 1 to 2000 do
      val r = Quotes.pick(s"**${Corrections.ICHack.correct}**", "<@1>", rng)
      assert(r.contains("**IC Hack**"), r)
      assert(!r.contains("DoCSoc"), r)
  }
  test("reply uses the correction's own spelling") {
    for _ <- 1 to 200 do
      assert(Corrections.ICHack.reply("<@1>").contains("**IC Hack**"))
      assert(!Corrections.ICHack.reply("<@1>").contains("DoCSoc"))
  }
  test("no template hard-codes a name") {
    assert(Quotes.templates.forall(!_.contains("DoCSoc")))
    assert(Quotes.templates.forall(!_.contains("ICHack") && !_.contains("IC Hack")))
  }
