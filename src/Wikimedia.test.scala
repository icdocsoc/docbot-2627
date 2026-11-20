package docsoc

import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration._

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

class WikimediaTests extends munit.FunSuite:

  @volatile private var stopping = CountDownLatch(1)

  /** A handler that never answers: waits until the test is over. */
  private def stall(): Unit =
    try stopping.await(10, TimeUnit.SECONDS)
    catch case _: InterruptedException => ()

  /** Runs `body` with a local server on an ephemeral port whose `/` handler is `handler`, and always stops it. */
  private def withServer[A](handler: HttpExchange => Unit)(body: String => A): A =
    stopping = CountDownLatch(1)
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress, 0), 0)
    server.createContext(
      "/",
      exchange =>
        try handler(exchange)
        finally exchange.close()
    )
    server.start()
    try body(s"http://localhost:${server.getAddress.getPort}/api")
    finally
      stopping.countDown() // lets stalled handlers go, so stop() doesn't wait for them
      server.stop(0)

  private def respond(exchange: HttpExchange, status: Int, text: String): Unit =
    val bytes = text.getBytes("UTF-8")
    exchange.sendResponseHeaders(status, if bytes.isEmpty then -1 else bytes.length.toLong)
    if bytes.nonEmpty then exchange.getResponseBody.write(bytes)

  private val Quick = 2.seconds

  test("fetch sends the user agent and accept headers, and requests the given path and query") {
    val seen = AtomicReference[(String, String, String)]()
    withServer { ex =>
      seen.set(
        (
          ex.getRequestHeaders.getFirst("User-Agent"),
          ex.getRequestHeaders.getFirst("Accept"),
          ex.getRequestMethod + " " + ex.getRequestURI
        )
      )
      respond(ex, 200, "{}")
    } { base =>
      assert(Wikimedia(s"$base?a=1%7C2", Quick, Quick).fetch().isRight)
    }
    val (agent, accept, request) = seen.get
    assertEquals(agent, Ducks.UserAgent)
    assertEquals(accept, "application/json")
    assertEquals(request, "GET /api?a=1%7C2")
  }
  test("fetch returns the body on 200") {
    withServer(respond(_, 200, """{"query": "ok"}""")) { base =>
      assertEquals(Wikimedia(base, Quick, Quick).fetch(), Right("""{"query": "ok"}"""))
    }
  }
  test("fetch decodes a UTF-8 body") {
    val text = "caf" + Character.toString(0xe9) + " " + Character.toString(0x1f986)
    withServer(respond(_, 200, text)) { base =>
      assertEquals(Wikimedia(base, Quick, Quick).fetch(), Right(text))
    }
  }
  test("fetch returns Left for 500, 429, 404 and other non-200 statuses") {
    for status <- Seq(500, 429, 404, 503, 204, 400) do
      withServer(respond(_, status, if status == 204 then "" else "nope")) { base =>
        val result = Wikimedia(base, Quick, Quick).fetch()
        assertEquals(result, Left(s"Wikimedia Commons answered with HTTP $status"))
      }
  }
  test("fetch returns Left when the server is too slow") {
    withServer { ex =>
      stall()
      respond(ex, 200, "late")
    } { base =>
      val started = System.nanoTime()
      val result = Wikimedia(base, Quick, 300.millis).fetch()
      val tookMs = (System.nanoTime() - started) / 1000000
      assertEquals(result, Left("Wikimedia Commons took too long to answer"))
      assert(tookMs < 2000, s"took $tookMs ms")
    }
  }
  test("fetch returns Left when the body stalls after the headers") {
    withServer { ex =>
      ex.sendResponseHeaders(200, 1000) // promises 1000 bytes, then sends nothing
      ex.getResponseBody.flush()
      stall()
    } { base =>
      val started = System.nanoTime()
      val result = Wikimedia(base, Quick, 300.millis).fetch()
      assert(result.isLeft, result)
      assert((System.nanoTime() - started) / 1000000 < 2000)
    }
  }
  test("fetch returns Left when the connection is refused") {
    val closedPort = withServer(respond(_, 200, "")) { base =>
      base.stripPrefix("http://localhost:").takeWhile(_.isDigit)
    }
    val result = Wikimedia(s"http://localhost:$closedPort/api", Quick, Quick).fetch()
    assert(result.left.toOption.exists(_.startsWith("Could not reach Wikimedia Commons")), result)
  }
  test("fetch returns Left for a malformed url instead of throwing") {
    assert(Wikimedia("not a url", Quick, Quick).fetch().isLeft)
    assert(Wikimedia("ftp://localhost/x", Quick, Quick).fetch().isLeft)
  }
  test("fetch restores the interrupt flag and returns Left when interrupted") {
    withServer { ex =>
      stall()
      respond(ex, 200, "late")
    } { base =>
      val result = AtomicReference[Either[String, String]]()
      val flag = AtomicReference[Boolean](false)
      val worker = Thread(() =>
        result.set(Wikimedia(base, Quick, 10.seconds).fetch())
        flag.set(Thread.currentThread().isInterrupted)
      )
      worker.start()
      Thread.sleep(300)
      worker.interrupt()
      worker.join(5000)
      assert(!worker.isAlive)
      assertEquals(result.get, Left("The request to Wikimedia Commons was interrupted"))
      assert(flag.get, "interrupt flag should be restored")
    }
  }
  private def pageBody(ids: Seq[Int], continue: Option[String], withInfo: Boolean) =
    val c = continue.fold("")(v => s""""continue": {"iicontinue": "$v", "continue": "||"},""")
    val pages = ids.map { i =>
      val info =
        if withInfo then
          s""", "imageinfo": [{"mime": "image/jpeg", "url": "https://u/$i", "descriptionurl": "https://d/$i"}]"""
        else ""
      s""""$i": {"title": "File:Duck_$i.jpg"$info}"""
    }
    s"""{$c"query": {"pages": {${pages.mkString(",")}}}}"""

  test("fetch follows the continue marker and merges the pages") {
    val queries = java.util.concurrent.CopyOnWriteArrayList[String]()
    withServer { ex =>
      val q = Option(ex.getRequestURI.getRawQuery).getOrElse("")
      queries.add(q)
      val body =
        if q.contains("iicontinue=two") then pageBody(1 to 5, None, withInfo = true)
        else if q.contains("iicontinue=one") then pageBody(1 to 5, Some("two"), withInfo = true)
        else pageBody(1 to 5, Some("one"), withInfo = true)
      respond(ex, 200, body)
    } { base =>
      val body = Wikimedia(s"$base?action=query", Quick, Quick).fetch().toOption.get
      assertEquals(queries.size, 3)
      assertEquals(queries.get(0), "action=query")
      assertEquals(queries.get(1), "action=query&iicontinue=one&continue=%7C%7C")
      assertEquals(queries.get(2), "action=query&iicontinue=two&continue=%7C%7C")
      assertEquals(Ducks.continuation(body), None)
      assertEquals(Ducks.parse(body).toOption.get.size, 5)
    }
  }
  test("fetch fills in the files that lacked imageinfo from the continued request") {
    withServer { ex =>
      val q = Option(ex.getRequestURI.getRawQuery).getOrElse("")
      val first = pageBody(1 to 3, Some("more"), withInfo = true).replace(
        """"3": {"title": "File:Duck_3.jpg", "imageinfo"""",
        """"3": {"title": "File:Duck_3.jpg", "x""""
      )
      respond(ex, 200, if q.contains("iicontinue") then pageBody(3 to 3, None, withInfo = true) else first)
    } { base =>
      val ducks = Ducks.parse(Wikimedia(s"$base?a=1", Quick, Quick).fetch().toOption.get).toOption.get
      assertEquals(ducks.map(_.title).toSet, Set("Duck 1", "Duck 2", "Duck 3"))
    }
  }
  test("fetch fails if a continued request fails, rather than returning part of the list") {
    withServer { ex =>
      if Option(ex.getRequestURI.getRawQuery).exists(_.contains("iicontinue")) then respond(ex, 429, "slow down")
      else respond(ex, 200, pageBody(1 to 2, Some("more"), withInfo = true))
    } { base =>
      assertEquals(Wikimedia(s"$base?a=1", Quick, Quick).fetch(), Left("Wikimedia Commons answered with HTTP 429"))
    }
  }
  test("fetch gives up if the server keeps continuing forever") {
    val count = java.util.concurrent.atomic.AtomicInteger(0)
    withServer { ex =>
      count.incrementAndGet()
      respond(ex, 200, pageBody(1 to 2, Some("again"), withInfo = true))
    } { base =>
      val result = Wikimedia(s"$base?a=1", Quick, Quick).fetch()
      assert(result.left.toOption.exists(_.contains("kept continuing")), result)
      assertEquals(count.get, Wikimedia.MaxRequests)
    }
  }
  test("fetch adds the continue parameters with ? if the url has no query") {
    val queries = java.util.concurrent.CopyOnWriteArrayList[String]()
    withServer { ex =>
      val q = Option(ex.getRequestURI.getRawQuery).getOrElse("")
      queries.add(q)
      respond(ex, 200, pageBody(1 to 2, if q.isEmpty then Some("k") else None, withInfo = true))
    } { base =>
      assert(Wikimedia(base, Quick, Quick).fetch().isRight)
      assertEquals(queries.get(1), "iicontinue=k&continue=%7C%7C")
    }
  }
  test("the default timeouts are 5 s to connect and 10 s for a request") {
    assertEquals(Wikimedia.ConnectTimeout, 5.seconds)
    assertEquals(Wikimedia.RequestTimeout, 10.seconds)
  }
