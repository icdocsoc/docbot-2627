package docsoc

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

class DucksTests extends munit.FunSuite:

  /** A trimmed real response from Commons (see `Ducks.RequestUrl`): only the fields we use are kept, and the
    * extmetadata of "Pile of Ducks.jpg" was removed by hand. It holds, in order: a jpg with an HTML artist, an ogg, a
    * jpg with a plain artist, an xcf, a png, a jpg without extmetadata, and a file with no imageinfo at all (the real
    * response has four of those, because of the `continue` marker).
    */
  private val fixture = """{
  "continue": {
    "iicontinue": "Yellow_rubber_duck.jpg|20170412090628",
    "continue": "||"
  },
  "limits": {
    "categorymembers": 500
  },
  "query": {
    "pages": {
      "126369618": {
        "pageid": 126369618,
        "ns": 6,
        "title": "File:1 Yellow rubber duck.jpg",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://upload.wikimedia.org/wikipedia/commons/e/e0/1_Yellow_rubber_duck.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail_unscaled",
            "url": "https://upload.wikimedia.org/wikipedia/commons/e/e0/1_Yellow_rubber_duck.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:1_Yellow_rubber_duck.jpg",
            "extmetadata": {
              "Artist": {
                "value": "<a href=\"//commons.wikimedia.org/w/index.php?title=User:Pieter_de_derde&amp;action=edit&amp;redlink=1\" class=\"new\" title=\"User:Pieter de derde (page does not exist)\">Pieter de derde</a>"
              },
              "LicenseShortName": {
                "value": "CC BY-SA 4.0"
              }
            },
            "mime": "image/jpeg"
          }
        ]
      },
      "98097055": {
        "pageid": 98097055,
        "ns": 6,
        "title": "File:De-Badeente.ogg",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://commons.wikimedia.org/w/resources/assets/file-type-icons/fileicon-ogg.png",
            "url": "https://upload.wikimedia.org/wikipedia/commons/6/64/De-Badeente.ogg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:De-Badeente.ogg",
            "extmetadata": {
              "Artist": {
                "value": "<a href=\"//commons.wikimedia.org/wiki/User:Jeuwre\" title=\"User:Jeuwre\">Jeuwre</a>"
              },
              "LicenseShortName": {
                "value": "CC BY-SA 4.0"
              }
            },
            "mime": "application/ogg"
          }
        ]
      },
      "77356710": {
        "pageid": 77356710,
        "ns": 6,
        "title": "File:Four Flushed Rubbers by Jack Lawrence.jpg",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/Four_Flushed_Rubbers_by_Jack_Lawrence.jpg/960px-Four_Flushed_Rubbers_by_Jack_Lawrence.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
            "url": "https://upload.wikimedia.org/wikipedia/commons/1/1d/Four_Flushed_Rubbers_by_Jack_Lawrence.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:Four_Flushed_Rubbers_by_Jack_Lawrence.jpg",
            "extmetadata": {
              "Artist": {
                "value": "Jack Lawrence"
              },
              "LicenseShortName": {
                "value": "CC BY 2.0"
              }
            },
            "mime": "image/jpeg"
          }
        ]
      },
      "156485803": {
        "pageid": 156485803,
        "ns": 6,
        "title": "File:Gay pride rubber duck (GIMP file).xcf",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/66/Gay_pride_rubber_duck_%28GIMP_file%29.xcf/500px-Gay_pride_rubber_duck_%28GIMP_file%29.xcf.png?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
            "url": "https://upload.wikimedia.org/wikipedia/commons/6/66/Gay_pride_rubber_duck_%28GIMP_file%29.xcf?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:Gay_pride_rubber_duck_(GIMP_file).xcf",
            "extmetadata": {
              "Artist": {
                "value": "<a href=\"//commons.wikimedia.org/w/index.php?title=User:Okterakt&amp;action=edit&amp;redlink=1\" class=\"new\" title=\"User:Okterakt (page does not exist)\">Okterakt</a>"
              },
              "LicenseShortName": {
                "value": "MIT"
              }
            },
            "mime": "image/x-xcf"
          }
        ]
      },
      "76370290": {
        "pageid": 76370290,
        "ns": 6,
        "title": "File:NIND dustyrubberduck ISO200.png",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/58/NIND_dustyrubberduck_ISO200.png/960px-NIND_dustyrubberduck_ISO200.png?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
            "url": "https://upload.wikimedia.org/wikipedia/commons/5/58/NIND_dustyrubberduck_ISO200.png?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:NIND_dustyrubberduck_ISO200.png",
            "extmetadata": {
              "Artist": {
                "value": "<a href=\"//commons.wikimedia.org/wiki/User:Trougnouf\" title=\"User:Trougnouf\">Trougnouf (Benoit Brummer)</a>"
              },
              "LicenseShortName": {
                "value": "CC BY 4.0"
              }
            },
            "mime": "image/png"
          }
        ]
      },
      "84509581": {
        "pageid": 84509581,
        "ns": 6,
        "title": "File:Pile of Ducks.jpg",
        "imagerepository": "local",
        "imageinfo": [
          {
            "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/53/Pile_of_Ducks.jpg/960px-Pile_of_Ducks.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
            "url": "https://upload.wikimedia.org/wikipedia/commons/5/53/Pile_of_Ducks.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original",
            "descriptionurl": "https://commons.wikimedia.org/wiki/File:Pile_of_Ducks.jpg",
            "mime": "image/jpeg"
          }
        ]
      },
      "57950157": {
        "pageid": 57950157,
        "ns": 6,
        "title": "File:Yellow rubber duck.jpg"
      }
    }
  }
}"""

  private def duck(n: Int) = Duck(s"Duck $n", s"https://img/$n.jpg", s"https://page/$n", "Someone, CC0")
  private def ducks(n: Int) = (1 to n).map(duck).toVector

  private def page(title: String, mime: String, info: String = "") =
    s"""{"title": "$title", "imageinfo": [{"mime": "$mime", "url": "https://u/x", "descriptionurl": "https://d/$title"$info}]}"""
  private def response(pages: String*) =
    pages.zipWithIndex.map((p, i) => s""""$i": $p""").mkString("""{"query": {"pages": {""", ",", "}}}")

  // --- parse ---

  test("parse keeps the image files of the real fixture, in order") {
    val parsed = Ducks.parse(fixture).toOption.get
    assertEquals(
      parsed.map(_.title),
      Vector(
        "1 Yellow rubber duck",
        "Four Flushed Rubbers by Jack Lawrence",
        "NIND dustyrubberduck ISO200",
        "Pile of Ducks"
      )
    )
  }
  test("parse drops non-image mime types and entries without imageinfo") {
    val titles = Ducks.parse(fixture).toOption.get.map(_.title)
    assert(!titles.contains("De-Badeente"), titles)
    assert(!titles.contains("Gay pride rubber duck (GIMP file)"), titles)
    assert(!titles.contains("Yellow rubber duck"), titles)
  }
  test("parse maps the fields of a duck") {
    val d = Ducks.parse(fixture).toOption.get.find(_.title == "Four Flushed Rubbers by Jack Lawrence").get
    assert(d.imageUrl.startsWith("https://thumb.wikimedia.org/"), d.imageUrl)
    assert(d.imageUrl.contains("960px-Four_Flushed_Rubbers_by_Jack_Lawrence.jpg"), d.imageUrl)
    assertEquals(d.pageUrl, "https://commons.wikimedia.org/wiki/File:Four_Flushed_Rubbers_by_Jack_Lawrence.jpg")
    assertEquals(d.credit, "Jack Lawrence, CC BY 2.0")
  }
  test("parse strips the HTML of an artist link") {
    val ducks = Ducks.parse(fixture).toOption.get
    assertEquals(ducks.find(_.title == "1 Yellow rubber duck").get.credit, "Pieter de derde, CC BY-SA 4.0")
    assertEquals(
      ducks.find(_.title == "NIND dustyrubberduck ISO200").get.credit,
      "Trougnouf (Benoit Brummer), CC BY 4.0"
    )
  }
  test("parse falls back to a generic credit without extmetadata") {
    val d = Ducks.parse(fixture).toOption.get.find(_.title == "Pile of Ducks").get
    assertEquals(d.credit, "Wikimedia Commons contributors")
  }
  test("parse uses the original url when there is no thumbnail") {
    val d = Ducks.parse(response(page("File:A_b.png", "image/png"))).toOption.get.head
    assertEquals(d, Duck("A b", "https://u/x", "https://d/File:A_b.png", "Wikimedia Commons contributors"))
  }
  test("parse prefers the thumbnail over the original") {
    val d = Ducks.parse(response(page("File:A.gif", "image/gif", """, "thumburl": "https://t/x""""))).toOption.get.head
    assertEquals(d.imageUrl, "https://t/x")
  }
  test("parse accepts exactly jpeg, png, gif and webp") {
    val mimes = Vector(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp",
      "image/svg+xml",
      "image/tiff",
      "application/pdf",
      "video/webm",
      "audio/mpeg",
      "application/ogg"
    )
    val kept = Ducks.parse(response(mimes.map(m => page("File:X.bin", m))*)).toOption.get
    assertEquals(kept.size, 4)
  }
  test("parse skips entries missing a title, mime, image url or page url") {
    val noTitle = """{"imageinfo": [{"mime": "image/png", "url": "https://u", "descriptionurl": "https://d"}]}"""
    val noMime = """{"title": "File:A.png", "imageinfo": [{"url": "https://u", "descriptionurl": "https://d"}]}"""
    val noUrl = """{"title": "File:B.png", "imageinfo": [{"mime": "image/png", "descriptionurl": "https://d"}]}"""
    val blankUrl =
      """{"title": "File:C.png", "imageinfo": [{"mime": "image/png", "url": " ", "descriptionurl": "https://d"}]}"""
    val noPage = """{"title": "File:D.png", "imageinfo": [{"mime": "image/png", "url": "https://u"}]}"""
    val emptyInfo = """{"title": "File:E.png", "imageinfo": []}"""
    val good = page("File:Good.png", "image/png")
    val parsed = Ducks.parse(response(noTitle, noMime, noUrl, blankUrl, noPage, emptyInfo, good))
    assertEquals(parsed.toOption.get.map(_.title), Vector("Good"))
  }
  test("parse tolerates odd extmetadata shapes") {
    val odd =
      page("File:A.png", "image/png", """, "extmetadata": {"Artist": "text", "LicenseShortName": {"value": 3}}""")
    assertEquals(Ducks.parse(response(odd)).toOption.get.head.credit, "Wikimedia Commons contributors")
  }
  test("parse reports malformed JSON") {
    val msg = Ducks.parse("{not json").left.toOption.get
    assert(msg.startsWith("Could not parse the duck list as JSON"), msg)
    assert(Ducks.parse("").isLeft)
  }
  test("parse rejects JSON that is not an object") {
    assertEquals(Ducks.parse("[1, 2]"), Left("Unexpected duck list: expected a JSON object"))
    assertEquals(Ducks.parse("null"), Left("Unexpected duck list: expected a JSON object"))
  }
  test("parse reports an API error object") {
    val err = """{"error": {"code": "badvalue", "info": "Unrecognized value for parameter."}, "servedby": "mw1"}"""
    assertEquals(Ducks.parse(err), Left("Wikimedia API error (badvalue): Unrecognized value for parameter."))
  }
  test("parse reports an API error without details") {
    assertEquals(Ducks.parse("""{"error": "boom"}"""), Left("Wikimedia API error (unknown): no details"))
  }
  test("parse reports an empty result") {
    val empty = Left("The duck list contained no usable images")
    assertEquals(Ducks.parse("""{"batchcomplete": ""}"""), empty)
    assertEquals(Ducks.parse("""{"query": {"pages": {}}}"""), empty)
    assertEquals(Ducks.parse("""{"query": {"pages": []}}"""), empty)
    assertEquals(Ducks.parse(response(page("File:A.svg", "image/svg+xml"))), empty)
  }
  test("the request url and user agent are set") {
    assert(Ducks.RequestUrl.startsWith("https://commons.wikimedia.org/w/api.php?"))
    assert(Ducks.RequestUrl.contains("gcmtitle=Category:Rubber_ducks"))
    assert(Ducks.RequestUrl.contains("iiurlwidth=800"))
    assert(!Ducks.RequestUrl.contains("|"), "java.net.URI rejects a raw pipe")
    assert(Ducks.RequestUrl.contains("iiprop=url%7Cmime%7Cextmetadata"))
    java.net.URI.create(Ducks.RequestUrl) // must be a valid URI
    assert(Ducks.UserAgent.startsWith("DoCBoT/"))
  }

  // --- continuation, mergePages ---

  test("continuation turns the continue object into query parameters") {
    assertEquals(
      Ducks.continuation("""{"continue": {"iicontinue": "A_b.jpg|20130612180058", "continue": "||"}, "query": {}}"""),
      Some("iicontinue=A_b.jpg%7C20130612180058&continue=%7C%7C")
    )
  }
  test("continuation is None for a complete, odd or invalid response") {
    assertEquals(Ducks.continuation(response(page("File:A.png", "image/png"))), None)
    assertEquals(Ducks.continuation("""{"continue": {}}"""), None)
    assertEquals(Ducks.continuation("""{"continue": "x"}"""), None)
    assertEquals(Ducks.continuation("[1]"), None)
    assertEquals(Ducks.continuation("{nope"), None)
  }
  test("continuation sees the continue marker of the real fixture") {
    assertEquals(
      Ducks.continuation(fixture),
      Some("iicontinue=Yellow_rubber_duck.jpg%7C20170412090628&continue=%7C%7C")
    )
  }

  private def pagesBody(continue: Option[String], pages: (String, String)*) =
    val c = continue.fold("")(v => s""""continue": {"iicontinue": "$v", "continue": "||"}, """)
    s"""{$c"query": {"pages": {${pages.map((id, p) => s""""$id": $p""").mkString(",")}}}}"""
  private val noInfo = (t: String) => s"""{"title": "$t"}"""

  test("mergePages takes the entry with imageinfo for each page, keeping the first's order") {
    val first = pagesBody(
      Some("x"),
      "1" -> page("File:One.jpg", "image/jpeg"),
      "2" -> noInfo("File:Two.jpg"),
      "3" -> noInfo("File:Three.jpg")
    )
    val second = pagesBody(None, "2" -> page("File:Two.jpg", "image/jpeg"), "3" -> page("File:Three.jpg", "image/png"))
    val merged = Ducks.mergePages(first, second).toOption.get
    assertEquals(Ducks.parse(merged).toOption.get.map(_.title), Vector("One", "Two", "Three"))
    assertEquals(Ducks.continuation(merged), None)
  }
  test("mergePages keeps the first entry when the second has none, and adds pages only in the second") {
    val first = pagesBody(Some("x"), "1" -> page("File:One.jpg", "image/jpeg"))
    val second = pagesBody(Some("y"), "1" -> noInfo("File:One.jpg"), "4" -> page("File:Four.jpg", "image/jpeg"))
    val merged = Ducks.mergePages(first, second).toOption.get
    assertEquals(Ducks.parse(merged).toOption.get.map(_.title), Vector("One", "Four"))
    assertEquals(Ducks.continuation(merged), Some("iicontinue=y&continue=%7C%7C"))
  }
  test("mergePages rejects a next page that is an API error or not JSON") {
    val first = pagesBody(Some("x"), "1" -> page("File:One.jpg", "image/jpeg"))
    assert(Ducks.mergePages(first, """{"error": {"code": "ratelimited"}}""").isLeft)
    assert(Ducks.mergePages(first, "nope").isLeft)
    assert(Ducks.mergePages("[1]", first).isLeft)
  }
  test("mergePages copes with a first page that has no query") {
    val second = pagesBody(None, "1" -> page("File:One.jpg", "image/jpeg"))
    assertEquals(Ducks.parse(Ducks.mergePages("""{"batchcomplete": ""}""", second).toOption.get).map(_.size), Right(1))
  }

  // --- cleanTitle, stripHtml, credit ---

  test("cleanTitle strips the prefix and the extension and replaces underscores") {
    assertEquals(Ducks.cleanTitle("File:Rubber_duck_in glass bowl.jpg"), "Rubber duck in glass bowl")
    assertEquals(Ducks.cleanTitle("File:Ducks (38295221176).JPG"), "Ducks (38295221176)")
    assertEquals(Ducks.cleanTitle("File:  Spaced__out  .png "), "Spaced out")
  }
  test("cleanTitle only removes the last extension, and only once") {
    assertEquals(Ducks.cleanTitle("File:a.b.c.jpg"), "a.b.c")
    assertEquals(Ducks.cleanTitle("File:Dr. Duck"), "Dr. Duck")
    assertEquals(Ducks.cleanTitle("File:no extension"), "no extension")
    assertEquals(Ducks.cleanTitle("Plain_name.webp"), "Plain name")
    assertEquals(Ducks.cleanTitle("File:Long.extension1"), "Long.extension1")
  }
  test("stripHtml removes tags") {
    assertEquals(Ducks.stripHtml("""<a href="//x?a=1&amp;b=2" class="new">Jane Doe</a>"""), "Jane Doe")
    assertEquals(Ducks.stripHtml("plain"), "plain")
    assertEquals(Ducks.stripHtml("<b>bold</b>text"), "boldtext")
    assertEquals(Ducks.stripHtml(""), "")
    assertEquals(Ducks.stripHtml("<a>"), "")
  }
  test("stripHtml turns block tags into spaces and collapses whitespace") {
    assertEquals(Ducks.stripHtml("<p>One</p><p>Two</p>"), "One Two")
    assertEquals(Ducks.stripHtml("One<br/>Two<BR>Three"), "One Two Three")
    assertEquals(Ducks.stripHtml("  a \n\t b  "), "a b")
  }
  test("stripHtml decodes basic entities") {
    assertEquals(
      Ducks.stripHtml("Tom &amp; Jerry &lt;3 &quot;ducks&quot; &#39;n&apos; more&nbsp;ducks &gt;"),
      "Tom & Jerry <3 \"ducks\" 'n' more ducks >"
    )
  }
  test("stripHtml decodes numeric entities") {
    val e = Character.toString(0xe9)
    assertEquals(Ducks.stripHtml("caf&#233; &#xE9; &#XE9;"), s"caf$e $e $e")
    assertEquals(Ducks.stripHtml("&#x41; &#41;"), "A )") // hex, then decimal
    assertEquals(Ducks.stripHtml("&#128038;"), Character.toString(0x1f426))
  }
  test("stripHtml leaves unknown or invalid entities alone") {
    val s = "&foo; &#0; &#99999999999; &#x110000; &#xZZ; & &;"
    assertEquals(Ducks.stripHtml(s), s)
  }
  test("stripHtml decodes only once") {
    assertEquals(Ducks.stripHtml("&amp;lt;b&amp;gt;"), "&lt;b&gt;")
    assertEquals(Ducks.stripHtml("&lt;b&gt;x&lt;/b&gt;"), "<b>x</b>")
  }
  test("stripHtml keeps dollar signs and backslashes") {
    assertEquals(Ducks.stripHtml("<i>$1 \\ &amp; $2</i>"), "$1 \\ & $2")
  }
  test("credit combines artist and licence, with fallbacks") {
    assertEquals(Ducks.credit(Some("<b>Ann</b>"), Some("CC0")), "Ann, CC0")
    assertEquals(Ducks.credit(Some("Ann"), None), "Ann")
    assertEquals(Ducks.credit(Some("Ann"), Some(" <i></i> ")), "Ann")
    assertEquals(Ducks.credit(None, Some("CC0")), "Wikimedia Commons contributors, CC0")
    assertEquals(Ducks.credit(Some("<a></a>"), Some("CC0")), "Wikimedia Commons contributors, CC0")
    assertEquals(Ducks.credit(None, None), "Wikimedia Commons contributors")
  }

  // --- pick ---

  test("pick never returns the same duck twice in a row") {
    val pool = ducks(7)
    for seed <- 1 to 5 do
      val rng = Random(seed)
      val picks = (1 to 3000).map(_ => Ducks.pick(pool, rng))
      assert(picks.zip(picks.tail).forall(_ != _))
  }
  test("pick reaches every duck") {
    val pool = ducks(10)
    val rng = Random(42)
    assertEquals((1 to 5000).map(_ => Ducks.pick(pool, rng)).toSet, pool.toSet)
  }
  test("pick with two ducks alternates") {
    val rng = Random(3)
    val picks = (1 to 50).map(_ => Ducks.pick(ducks(2), rng))
    assert(picks.zip(picks.tail).forall(_ != _))
  }
  test("pick works with a pool of one") {
    val rng = Random(1)
    assertEquals((1 to 5).map(_ => Ducks.pick(ducks(1), rng)).toSet, ducks(1).toSet)
  }
  test("pick copes with a pool that no longer holds the previous duck") {
    val rng = Random(9)
    Ducks.pick(ducks(5), rng)
    val other = (11 to 13).map(duck).toVector
    assert(other.contains(Ducks.pick(other, rng)))
  }
  test("pick defaults to the global random") {
    assert(ducks(3).contains(Ducks.pick(ducks(3))))
  }
  test("pick rejects an empty pool") {
    intercept[IllegalArgumentException](Ducks.pick(Vector.empty, Random(1)))
  }

  // --- DuckSource ---

  private val ttl = 10.minutes
  private val t0 = Instant.parse("2026-01-01T00:00:00Z")
  private def body(n: Int) = response((1 to n).map(i => page(s"File:Duck_$i.jpg", "image/jpeg"))*)

  /** A fake clock and a fake fetch that counts its calls and can be told to fail. */
  private class Harness(var result: Either[String, String] = Right(body(3))):
    var time: Instant = t0
    val calls = AtomicInteger(0)
    private def fetch(): Either[String, String] =
      calls.incrementAndGet()
      result
    val source = DuckSource(() => fetch(), ttl, () => time, Random(1))

  test("DuckSource fetches lazily, then reuses the list within the ttl") {
    val h = Harness()
    assertEquals(h.calls.get, 0)
    val titles = (1 to 20).map(_ => h.source.next().toOption.get.title).toSet
    assertEquals(h.calls.get, 1)
    assertEquals(titles, Set("Duck 1", "Duck 2", "Duck 3"))
    h.time = t0.plusSeconds(599)
    assert(h.source.next().isRight)
    assertEquals(h.calls.get, 1)
  }
  test("DuckSource fetches again once the ttl has passed") {
    val h = Harness()
    h.source.next()
    h.time = t0.plus(ttl.toMillis, ChronoUnit.MILLIS)
    h.result = Right(response(page("File:New_duck.jpg", "image/jpeg")))
    assertEquals(h.source.next().map(_.title), Right("New duck"))
    assertEquals(h.calls.get, 2)
    h.source.next()
    assertEquals(h.calls.get, 2)
  }
  test("DuckSource serves the stale list when a refresh fails") {
    val h = Harness()
    h.source.next()
    h.time = t0.plus(ttl.toMillis + 1, ChronoUnit.MILLIS)
    h.result = Left("HTTP 503")
    assert(h.source.next().isRight)
    assert(h.source.next().isRight)
    assertEquals(h.calls.get, 3) // retried on each call while stale
    h.result = Right(body(1))
    assertEquals(h.source.next().map(_.title), Right("Duck 1")) // recovered
  }
  test("DuckSource serves the stale list when a refresh returns an API error") {
    val h = Harness()
    h.source.next()
    h.time = t0.plusSeconds(3600)
    h.result = Right("""{"error": {"code": "ratelimited", "info": "slow down"}}""")
    assert(h.source.next().isRight)
  }
  test("DuckSource turns a throwing fetch into an error, or the stale list") {
    var fail = false
    var time = t0
    val source = DuckSource(
      () => if fail then throw RuntimeException("boom") else Right(body(2)),
      ttl,
      () => time,
      Random(1)
    )
    fail = true
    assertEquals(source.next(), Left("Could not fetch the duck list: boom"))
    fail = false
    assert(source.next().isRight)
    fail = true
    time = t0.plusSeconds(3600)
    assert(source.next().isRight)
  }
  test("DuckSource returns the error when it fails with nothing cached") {
    val h = Harness(Left("connection refused"))
    assertEquals(h.source.next(), Left("connection refused"))
    h.result = Right("not json")
    assert(h.source.next().left.toOption.get.startsWith("Could not parse the duck list as JSON"))
    h.result = Right("""{"query": {"pages": {}}}""")
    assertEquals(h.source.next(), Left("The duck list contained no usable images"))
    assertEquals(h.calls.get, 3) // failures aren't cached
    h.result = Right(body(2))
    assert(h.source.next().isRight)
  }
  test("DuckSource never repeats a duck in a row") {
    val h = Harness()
    val picks = (1 to 200).map(_ => h.source.next().toOption.get)
    assert(picks.zip(picks.tail).forall(_ != _))
  }
  test("DuckSource is safe under concurrent calls") {
    val h = Harness(Right(body(5)))
    val results = Await.result(Future.sequence((1 to 64).map(_ => Future(h.source.next()))), 30.seconds)
    assert(results.forall(_.isRight))
    assertEquals(h.calls.get, 1)
  }
  test("DuckSource defaults to the real clock and randomness") {
    val source = DuckSource(() => Right(body(2)), 1.hour)
    assert(source.next().isRight)
  }
