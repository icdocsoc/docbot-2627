package docsoc

/** Tests for the generic mechanism, using small local corrections rather than the real ones. */
class CorrectionTests extends munit.FunSuite:
  private val Foo = Correction("Foo", "(?i)foo".r)
  private val Bar = Correction("Bar", "(?i)bar".r)

  test("misspelt flags a wrong spelling and accepts the correct one") {
    assert(Foo.misspelt("foo"))
    assert(Foo.misspelt("say FOO now"))
    assert(Foo.misspelt("Foo and foo"))
    assert(!Foo.misspelt("Foo"))
    assert(!Foo.misspelt("nothing to see"))
    assert(!Foo.misspelt(""))
  }
  test("URLs are ignored, text outside them is not") {
    for s <- List("https://x/foo", "http://x/foo", "www.foo.com/path", "<https://x/foo>", "see HTTPS://FOO.example/x") do
      assert(!Foo.misspelt(s), s)
    for s <- List("foo https://x/foo", "<https://x/foo> join foo", "www.foo.com FOO") do
      assert(Foo.misspelt(s), s)
  }
  test("invisible characters split nothing and hide nothing") {
    for s <- List("fo\u200Bo", "f\u200Do\u200Co", "fo\u00ADo", "FO\uFEFFO", "f\u2060o\u200Eo") do
      assert(Foo.misspelt(s), s)
    for s <- List("F\u200Boo", "\uFEFFFoo", "Foo\u200B") do assert(!Foo.misspelt(s), s)
    for s <- List("https://x/fo\u200Bo", "ht\u200Btps://x/foo", "www.fo\u200Bo.com") do assert(!Foo.misspelt(s), s)
  }
  test("every character in Invisibles is stripped") {
    assertEquals(Correction.Invisibles.size, 18)
    for c <- Correction.Invisibles do assert(Foo.misspelt(s"fo${c}o"), f"U+${c.toInt}%04X")
  }
  test("normalise removes invisible characters and replaces URLs with a space") {
    assertEquals(Correction.normalise("a\u200Bb"), "ab")
    assertEquals(Correction.normalise("a https://x/y b"), "a   b")
    assertEquals(Correction.normalise("ht\u200Btps://x/y"), " ")
    assertEquals(Correction.normalise("plain"), "plain")
  }
  test("corrections are independent") {
    assert(!Foo.misspelt("bar"))
    assert(!Bar.misspelt("foo"))
  }
  test("firstMisspelt finds nothing when nothing is misspelt") {
    assertEquals(Correction.firstMisspelt(List(Foo, Bar), "Foo and Bar"), None)
    assertEquals(Correction.firstMisspelt(Nil, "foo"), None)
  }
  test("firstMisspelt finds the first matching correction in list order") {
    assertEquals(Correction.firstMisspelt(List(Foo, Bar), "bar"), Some(Bar))
    assertEquals(Correction.firstMisspelt(List(Foo, Bar), "foo"), Some(Foo))
    assertEquals(Correction.firstMisspelt(List(Foo, Bar), "bar and foo"), Some(Foo))
    assertEquals(Correction.firstMisspelt(List(Bar, Foo), "bar and foo"), Some(Bar))
  }
  test("reply uses the correction's own spelling") {
    for _ <- 1 to 200 do
      val r = Foo.reply("<@1>")
      assert(r.contains("**Foo**"), r)
  }
