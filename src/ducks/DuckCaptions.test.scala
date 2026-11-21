package docsoc.ducks

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

class DuckCaptionsTests extends munit.FunSuite:

  private val mention = "<@123456789012345678>"
  private val placeholder = "\\{[^{}]*\\}".r

  test("the pool has plenty of captions, with no duplicates") {
    assert(DuckCaptions.templates.size >= 40, DuckCaptions.templates.size)
    assert(DuckCaptions.templates.size <= 60, DuckCaptions.templates.size)
    assertEquals(DuckCaptions.templates.distinct.size, DuckCaptions.templates.size)
  }
  test("every caption is short and not blank") {
    for t <- DuckCaptions.templates do
      assert(t.trim.nonEmpty, t)
      assert(t.length <= 200, s"${t.length}: $t")
  }
  test("a minority of captions mention the requester") {
    val withUser = DuckCaptions.templates.count(_.contains("{user}"))
    assert(withUser >= 5, withUser)
    assert(withUser * 2 < DuckCaptions.templates.size, withUser)
  }
  test("{user} is the only placeholder, and braces are used for nothing else") {
    for t <- DuckCaptions.templates do
      assert(placeholder.findAllIn(t).forall(_ == "{user}"), t)
      assertEquals(t.count(_ == '{'), t.count(_ == '}'), t)
      assertEquals(t.count(_ == '{'), "\\{user\\}".r.findAllIn(t).size, t)
  }
  test("filling in a mention leaves no braces and puts the mention in verbatim") {
    for t <- DuckCaptions.templates do
      val filled = DuckCaptions.fill(t, mention)
      assert(!filled.contains("{") && !filled.contains("}"), filled)
      assertEquals(filled.contains(mention), t.contains("{user}"), filled)
      assertEquals(filled.sliding(mention.length).count(_ == mention), "\\{user\\}".r.findAllIn(t).size)
  }
  test("a mention that itself looks like a placeholder is not expanded again") {
    assertEquals(DuckCaptions.fill("Hi {user}!", "{user}{user}"), "Hi {user}{user}!")
  }
  test("no caption can ping everyone or a role, or contain a stray mention") {
    for t <- DuckCaptions.templates do
      val lower = t.toLowerCase
      assert(!lower.contains("@everyone") && !lower.contains("@here"), t)
      assert(!t.contains("@") && !t.contains("<"), t)
  }
  test("captions have no markdown that could break the message") {
    for t <- DuckCaptions.templates do assert(!t.exists(c => "*_~`|>[]\\".indexOf(c) >= 0), t)
  }
  test("captions are plain ASCII") {
    for t <- DuckCaptions.templates do assert(t.forall(_ < 128), t)
  }
  test("an absurdly long mention is cut to the message limit, with an ellipsis") {
    val filled = DuckCaptions.fill("Hi {user}!", "x" * 5000)
    assertEquals(filled.length, DuckCaptions.MaxLength)
    assert(filled.endsWith("..."))
  }
  test("cutting never splits a surrogate pair") {
    val emoji = Character.toString(0x1f986)
    for pad <- 0 to 1 do
      val filled = DuckCaptions.fill("{user}", "x" * pad + emoji * 2000)
      assert(filled.length <= DuckCaptions.MaxLength)
      assert(!Character.isHighSurrogate(filled.dropRight(3).last), filled)
  }
  test("a short mention is left alone") {
    assertEquals(DuckCaptions.fill("Hi {user}!", "<@1>"), "Hi <@1>!")
  }

  test("pick fills in the mention and returns pool captions") {
    val rng = Random(1)
    val pool = DuckCaptions.templates.map(DuckCaptions.fill(_, mention)).toSet
    for _ <- 1 to 2000 do
      val c = DuckCaptions.pick(mention, rng)
      assert(pool.contains(c), c)
      assert(!c.contains("{"), c)
  }
  test("never the same caption twice in a row, for several seeds") {
    for seed <- 1 to 5 do
      val rng = Random(seed)
      val picks = (1 to 5000).map(_ => DuckCaptions.pick(mention, rng))
      assert(picks.zip(picks.tail).forall(_ != _), s"seed $seed")
  }
  test("every caption is reachable") {
    val rng = Random(42)
    val seen = (1 to 20000).map(_ => DuckCaptions.pick(mention, rng)).toSet
    assertEquals(seen, DuckCaptions.templates.map(DuckCaptions.fill(_, mention)).toSet)
  }
  test("concurrent picks are safe and always return a pool caption") {
    val pool = Executors.newFixedThreadPool(8)
    given ExecutionContext = ExecutionContext.fromExecutor(pool)
    try
      val valid = DuckCaptions.templates.map(DuckCaptions.fill(_, mention)).toSet
      val all = Future.sequence(
        (1 to 8).map(i => Future { val rng = Random(i); (1 to 2000).map(_ => DuckCaptions.pick(mention, rng)) })
      )
      val picks = Await.result(all, 30.seconds).flatten
      assertEquals(picks.size, 16000)
      assert(picks.forall(valid.contains))
    finally
      pool.shutdown()
      pool.awaitTermination(5, TimeUnit.SECONDS)
  }

class PicksTests extends munit.FunSuite:

  test("a pool of one always gives index 0, even when it is the one to avoid") {
    val rng = Random(1)
    for avoid <- Seq(-1, 0, 5) do assertEquals(docsoc.Picks.indexAvoiding(1, avoid, rng), 0)
  }
  test("the avoided index is never drawn, and every other index is") {
    val rng = Random(7)
    val drawn = (1 to 5000).map(_ => docsoc.Picks.indexAvoiding(4, 2, rng)).toSet
    assertEquals(drawn, Set(0, 1, 3))
  }
  test("with nothing to avoid every index is possible") {
    val rng = Random(7)
    for avoid <- Seq(-1, 4, 99) do
      assertEquals((1 to 2000).map(_ => docsoc.Picks.indexAvoiding(4, avoid, rng)).toSet, Set(0, 1, 2, 3))
  }
  test("an empty pool is rejected") {
    intercept[IllegalArgumentException](docsoc.Picks.indexAvoiding(0, -1, Random(1)))
  }
