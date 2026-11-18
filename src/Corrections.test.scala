package docsoc

/** Behaviour of the concrete corrections (DoCSoc, IC Hack) and their ordering in `Corrections.all`. */
class CorrectionsTests extends munit.FunSuite:
  test("flags every spelling of IC Hack except exactly IC Hack") {
    for s <- List(
        "ICHack",
        "ichack",
        "ICHACK",
        "ic hack",
        "Ic Hack",
        "IC  Hack",
        "IC-Hack",
        "ic-hack",
        "IC_Hack",
        "I.C. Hack",
        "ICHack26",
        "ICHack27",
        "IC hack",
        "IC HACK",
        "come to ichack!",
        "ICHack and ichack",
        "see you at IC\tHack",
        "IC Hack and ichack"
      )
    do assert(Corrections.ICHack.misspelt(s), s)
  }
  test("leaves exactly IC Hack and unrelated text alone") {
    for s <- List("IC Hack", "IC Hack 26", "IC Hack, ", "hello IC Hack!", "IC Hack 2026", "nothing to see", "") do
      assert(!Corrections.ICHack.misspelt(s), s)
  }
  test("IC Hack is not flagged inside other words") {
    for s <- List("chic hack", "epic hack", "Epic Hack", "ichacks", "ICHacks", "ichacker", "topic-hack", "logic hack")
    do assert(!Corrections.ICHack.misspelt(s), s)
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
  test("DoCSoc flags misspellings") {
    for s <- List("docsoc", "DOCSOC", "DoCSoC", "Docsoc", "join docsoc today!", "DoCSoc and docsoc") do
      assert(Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc leaves correct spelling and unrelated text alone") {
    for s <- List("DoCSoc", "hello DoCSoc!", "nothing to see", "") do assert(!Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc ignores misspellings inside URLs") {
    for s <- List(
        "https://github.com/icdocsoc",
        "http://x/docsoc",
        "www.docsoc.co.uk/path",
        "<https://github.com/icdocsoc>",
        "see HTTPS://DOCSOC.example/x for details"
      )
    do assert(!Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc still flags a misspelling outside a URL") {
    for s <- List("docsoc https://github.com/icdocsoc", "<https://x/docsoc> join docsoc", "www.docsoc.co.uk DOCSOC") do
      assert(Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc flags misspellings split by invisible characters") {
    for s <- List("doc\u200Bsoc", "d\u200Docsoc", "doc\u00ADsoc", "DOC\uFEFFSOC", "d\u2060o\u200Cc\u200Es\u202Eoc") do
      assert(Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc does not flag a correct DoCSoc containing invisible characters") {
    for s <- List("Do\u200BCSoc", "D\u200Do\u200CC\u2060S\u00ADoc", "\uFEFFDoCSoc", "DoCSoc\u200B") do
      assert(!Corrections.DoCSoc.misspelt(s), s)
  }
  test("DoCSoc still ignores URLs containing invisible characters") {
    for s <- List("https://github.com/ic\u200Bdocsoc", "ht\u200Btps://x/docsoc", "www.doc\u200Bsoc.co.uk") do
      assert(!Corrections.DoCSoc.misspelt(s), s)
  }
  test("every invisible character is stripped when checking DoCSoc") {
    for c <- Correction.Invisibles do assert(Corrections.DoCSoc.misspelt(s"doc${c}soc"), f"U+${c.toInt}%04X")
  }
  test("DoCSoc and IC Hack corrections are independent") {
    assert(!Corrections.ICHack.misspelt("docsoc"))
    assert(!Corrections.DoCSoc.misspelt("ichack"))
  }
  test("all lists DoCSoc then IC Hack") {
    assertEquals(Corrections.all, List(Corrections.DoCSoc, Corrections.ICHack))
  }
  test("firstMisspelt over all finds the first matching correction") {
    val first = Correction.firstMisspelt(Corrections.all, _)
    assertEquals(first("DoCSoc and IC Hack"), None)
    assertEquals(first("ic hack"), Some(Corrections.ICHack))
    assertEquals(first("ICHack26"), Some(Corrections.ICHack))
    assertEquals(first("docsoc"), Some(Corrections.DoCSoc))
    assertEquals(first("ic hack and docsoc"), Some(Corrections.DoCSoc))
  }
  test("ICHack replies contain **IC Hack** and never DoCSoc") {
    val rng = scala.util.Random(7)
    for _ <- 1 to 2000 do
      val r = Quotes.pick(s"**${Corrections.ICHack.correct}**", "<@1>", rng)
      assert(r.contains("**IC Hack**"), r)
      assert(!r.contains("DoCSoc"), r)
  }
  test("IC Hack reply uses the correction's own spelling") {
    for _ <- 1 to 200 do
      assert(Corrections.ICHack.reply("<@1>").contains("**IC Hack**"))
      assert(!Corrections.ICHack.reply("<@1>").contains("DoCSoc"))
  }
  test("DoCSoc reply contains the correct spelling") {
    for _ <- 1 to 200 do assert(Corrections.DoCSoc.reply("<@1>").contains("**DoCSoc**"))
  }
