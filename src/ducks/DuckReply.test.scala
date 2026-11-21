package docsoc.ducks

class DuckReplyTests extends munit.FunSuite:

  private val zws = Character.toString(0x200b)
  private def duck(title: String = "Rubber duck", credit: String = "Jane Doe, CC BY 4.0") =
    Duck(title, "https://img.example/duck.jpg", "https://commons.wikimedia.org/wiki/File:Duck.jpg", credit)

  private def build(d: Duck, caption: String = "") = DuckReply.of(d, caption)

  test("a duck becomes a reply with the title, image, page and a credit line") {
    assertEquals(
      build(duck()),
      DuckReply(
        "Rubber duck",
        "https://img.example/duck.jpg",
        "[Rubber duck](https://commons.wikimedia.org/wiki/File:Duck.jpg) by Jane Doe, CC BY 4.0, via Wikimedia Commons",
        "https://commons.wikimedia.org/wiki/File:Duck.jpg",
        ""
      )
    )
  }
  test("markdown characters in the title and credit are escaped") {
    val reply = build(duck("a*b_c~d`e|f>g[h]i(j)k#l<m\\n", "**bold** [x](http://evil) `code`"))
    assertEquals(reply.title, "a\\*b\\_c\\~d\\`e\\|f\\>g\\[h\\]i\\(j\\)k\\#l\\<m\\\\n")
    assert(reply.description.contains("by \\*\\*bold\\*\\* \\[x\\]\\(http://evil\\) \\`code\\`,"), reply.description)
    // exactly one real link: the one we built
    assertEquals("""(?<!\\)\]\(""".r.findAllIn(reply.description).size, 1)
  }
  test("a closing bracket in the title can't break out of the link") {
    val d = build(duck("x](http://evil.example) y")).description
    assert(d.startsWith("[x\\]\\(http://evil.example\\) y](https://commons"), d)
  }
  test("mentions are neutralised in the title and credit") {
    val reply = build(duck("@everyone @here <@123> <@&456> <#789>", "@everyone, <@!1>"))
    for text <- Seq(reply.title, reply.description) do
      assert(!text.contains("@everyone"), text)
      assert(!text.contains("@here"), text)
      assert("(?<!\\\\)<[@#]".r.findFirstIn(text).isEmpty, text) // no unescaped mention syntax
    assert(reply.title.contains("@" + zws + "everyone"), reply.title)
  }
  test("the page url is made safe inside the link") {
    val d = Duck("Pride", "https://i/x.png", "https://commons.wikimedia.org/wiki/File:A_(GIMP file).xcf", "Me")
    assertEquals(
      build(d).description,
      "[Pride](https://commons.wikimedia.org/wiki/File:A_%28GIMP%20file%29.xcf) by Me, via Wikimedia Commons"
    )
    assertEquals(build(d).pageUrl, "https://commons.wikimedia.org/wiki/File:A_(GIMP file).xcf") // for setTitle
  }
  test("an absurdly long page url is shown as plain text rather than a broken link") {
    val d = Duck("T", "https://i/x.png", "https://x/" + "a" * 3000, "Me")
    assertEquals(build(d).description, "T by Me, via Wikimedia Commons")
  }
  test("whitespace and control characters are collapsed") {
    val reply = build(duck("  Two\n\nlines\t here" + Character.toString(7) + " ", "A\r\nB"))
    assertEquals(reply.title, "Two lines here")
    assert(reply.description.endsWith("by A B, via Wikimedia Commons"), reply.description)
  }
  test("a blank title falls back to a default") {
    assertEquals(build(duck("  ")).title, "Rubber duck")
    assert(build(duck("")).description.startsWith("[Rubber duck]("))
  }
  test("a long title is cut to the embed title limit, with an ellipsis") {
    val reply = build(duck("x" * 1000))
    assertEquals(reply.title.length, DuckReply.MaxTitle)
    assert(reply.title.endsWith("..."))
  }
  test("a title that fits is left alone, at the exact limit too") {
    assertEquals(DuckReply.sanitize("x" * 256, 256), "x" * 256)
    assertEquals(DuckReply.sanitize("x" * 257, 256).length, 256)
  }
  test("truncation never splits an escape sequence or a surrogate pair") {
    val escaped = DuckReply.sanitize("*" * 300, 11) // each star becomes two characters
    assertEquals(escaped, "\\*" * 4 + "...")
    val emoji = Character.toString(0x1f986)
    val cut = DuckReply.sanitize(emoji * 50, 10)
    assertEquals(cut, emoji * 3 + "...")
    assert(cut.codePoints().toArray.forall(cp => cp != 0xfffd))
    assert(
      !Character.isHighSurrogate(cut.charAt(cut.length - 4)) || Character.isLowSurrogate(cut.charAt(cut.length - 3))
    )
  }
  test("the description stays within Discord's limit even with huge, markdown-heavy input") {
    val reply = build(duck("*" * 5000, "[" * 20000))
    assert(reply.description.length <= DuckReply.MaxDescription, reply.description.length)
    assert(reply.title.length <= DuckReply.MaxTitle)
    assert(reply.description.endsWith(", via Wikimedia Commons"))
    val worst = Duck("*" * 5000, "https://i/x", "https://x/" + "(" * 600, "*" * 5000)
    assert(build(worst).description.length <= DuckReply.MaxDescription)
  }
  test("the failure message is friendly and fits a message") {
    assertEquals(DuckReply.failureMessage, "The ducks are on strike. Try again later.")
  }

  test("the caption is carried as it is: a mention stays a mention and the description has none of it") {
    val reply = build(duck(), "Quack, <@123>! *Squeak*")
    assertEquals(reply.caption, "Quack, <@123>! *Squeak*")
    assert(!reply.description.contains("Quack"), reply.description)
  }
  test("the caption is trimmed") {
    assertEquals(build(duck(), "  hi \n").caption, "hi")
  }
  test("a caption at the limit is left alone and a longer one is cut to it, with an ellipsis") {
    assertEquals(build(duck(), "x" * DuckReply.MaxCaption).caption, "x" * DuckReply.MaxCaption)
    val cut = build(duck(), "x" * 5000).caption
    assertEquals(cut.length, DuckReply.MaxCaption)
    assert(cut.endsWith("..."))
  }
  test("cutting a caption never splits a surrogate pair") {
    val emoji = Character.toString(0x1f986)
    for pad <- 0 to 1 do
      val cut = build(duck(), "x" * pad + emoji * 2000).caption
      assert(cut.length <= DuckReply.MaxCaption)
      assert(cut.endsWith("..."))
      val body = cut.dropRight(3)
      assert(!Character.isHighSurrogate(body.last), cut)
  }
