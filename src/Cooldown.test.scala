package docsoc

import java.time.Instant
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._

class CooldownTests extends munit.FunSuite:
  private val start = Instant.parse("2026-01-01T00:00:00Z")

  private final class Clock:
    @volatile var t: Instant = start
    def advance(d: FiniteDuration): Unit = t = t.plusMillis(d.toMillis)
    val now: () => Instant = () => t

  test("the first acquire is allowed") {
    val c = Cooldown(15.seconds, Clock().now)
    assertEquals(c.tryAcquire("a"), Right(()))
  }

  test("a second acquire inside the window reports the remaining time") {
    val clock = Clock()
    val c = Cooldown(15.seconds, clock.now)
    c.tryAcquire("a")
    clock.advance(4.seconds)
    assertEquals(c.tryAcquire("a"), Left(11.seconds))
  }

  test("a rejected attempt does not extend the cooldown") {
    val clock = Clock()
    val c = Cooldown(15.seconds, clock.now)
    c.tryAcquire("a")
    for _ <- 1 to 10 do
      clock.advance(1.second)
      assert(c.tryAcquire("a").isLeft)
    clock.advance(5.seconds) // 15 seconds since the allowed one
    assertEquals(c.tryAcquire("a"), Right(()))
  }

  test("allowed again exactly at the window, not a millisecond before") {
    val clock = Clock()
    val c = Cooldown(15.seconds, clock.now)
    c.tryAcquire("a")
    clock.advance(15.seconds - 1.millis)
    assertEquals(c.tryAcquire("a"), Left(1.millis))
    clock.advance(1.millis)
    assertEquals(c.tryAcquire("a"), Right(()))
  }

  test("after being allowed again the window restarts") {
    val clock = Clock()
    val c = Cooldown(10.seconds, clock.now)
    c.tryAcquire("a")
    clock.advance(30.seconds)
    assertEquals(c.tryAcquire("a"), Right(()))
    assertEquals(c.tryAcquire("a"), Left(10.seconds))
  }

  test("keys are independent") {
    val clock = Clock()
    val c = Cooldown(15.seconds, clock.now)
    assertEquals(c.tryAcquire("randomduck:1"), Right(()))
    assertEquals(c.tryAcquire("randomduck:2"), Right(()))
    assertEquals(c.tryAcquire("ping:1"), Right(()))
    assert(c.tryAcquire("randomduck:1").isLeft)
  }

  test("the remaining time is always positive when refused") {
    val clock = Clock()
    val c = Cooldown(3.seconds, clock.now)
    c.tryAcquire("a")
    for _ <- 1 to 2999 do
      clock.advance(1.milli)
      c.tryAcquire("a") match
        case Left(rest) => assert(rest > Duration.Zero && rest <= 3.seconds, rest.toString)
        case Right(_)   => fail("should still be cooling down")
  }

  test("expired entries are purged, so memory follows the active keys") {
    val clock = Clock()
    val c = Cooldown(10.seconds, clock.now)
    for i <- 1 to 100 do c.tryAcquire(s"k$i")
    assertEquals(c.size, 100)
    clock.advance(10.seconds)
    c.tryAcquire("fresh")
    assertEquals(c.size, 1)
  }

  test("purging keeps entries that have not expired yet") {
    val clock = Clock()
    val c = Cooldown(10.seconds, clock.now)
    c.tryAcquire("old")
    clock.advance(6.seconds)
    c.tryAcquire("new")
    clock.advance(5.seconds) // old expired, new has 9 seconds left
    assertEquals(c.tryAcquire("other"), Right(()))
    assertEquals(c.size, 2)
    assertEquals(c.tryAcquire("new"), Left(5.seconds))
    assertEquals(c.tryAcquire("old"), Right(()))
  }

  test("the map never exceeds maxEntries, even when every entry is live") {
    val clock = Clock()
    val c = Cooldown(1.hour, clock.now, maxEntries = 50)
    for i <- 1 to 500 do
      c.tryAcquire(s"k$i")
      assert(c.size <= 50)
    assertEquals(c.size, 50)
    // the most recent keys are still cooling down; the oldest were evicted to make room
    assert(c.tryAcquire("k500").isLeft)
    assertEquals(c.tryAcquire("k1"), Right(()))
  }

  test("a key that is allowed again moves to the back of the eviction order") {
    val clock = Clock()
    val c = Cooldown(10.seconds, clock.now, maxEntries = 2)
    c.tryAcquire("a")
    clock.advance(11.seconds)
    c.tryAcquire("b")
    c.tryAcquire("a") // a expired, so it is allowed again and re-recorded after b
    c.tryAcquire("c") // full: evicts b, the front
    assert(c.tryAcquire("a").isLeft)
    assert(c.tryAcquire("c").isLeft)
  }

  test("rejects a non-positive window or maxEntries") {
    intercept[IllegalArgumentException](Cooldown(Duration.Zero))
    intercept[IllegalArgumentException](Cooldown(1.second, maxEntries = 0))
  }

  test("concurrent acquires for one key let exactly one through") {
    val threads = 16
    val pool = Executors.newFixedThreadPool(threads)
    try
      for round <- 1 to 20 do
        val c = Cooldown(1.hour)
        val barrier = CyclicBarrier(threads)
        val allowed = AtomicInteger(0)
        val futures = (1 to threads).map { _ =>
          pool.submit(new Runnable:
            def run(): Unit =
              barrier.await(10, TimeUnit.SECONDS)
              if c.tryAcquire(s"user$round").isRight then allowed.incrementAndGet())
        }
        futures.foreach(_.get(10, TimeUnit.SECONDS))
        assertEquals(allowed.get, 1)
    finally pool.shutdownNow()
  }

  test("concurrent acquires for different keys all get through") {
    val threads = 16
    val pool = Executors.newFixedThreadPool(threads)
    try
      val c = Cooldown(1.hour)
      val barrier = CyclicBarrier(threads)
      val allowed = AtomicInteger(0)
      val futures = (1 to threads).map { i =>
        pool.submit(new Runnable:
          def run(): Unit =
            barrier.await(10, TimeUnit.SECONDS)
            if c.tryAcquire(s"user$i").isRight then allowed.incrementAndGet())
      }
      futures.foreach(_.get(10, TimeUnit.SECONDS))
      assertEquals(allowed.get, threads)
    finally pool.shutdownNow()
  }

  test("describe rounds up and never says 0 seconds") {
    assertEquals(Cooldown.describe(Duration.Zero), "1 second")
    assertEquals(Cooldown.describe(1.milli), "1 second")
    assertEquals(Cooldown.describe(200.millis), "1 second")
    assertEquals(Cooldown.describe(1.second), "1 second")
    assertEquals(Cooldown.describe(1001.millis), "2 seconds")
    assertEquals(Cooldown.describe(1100.millis), "2 seconds")
    assertEquals(Cooldown.describe(2.seconds), "2 seconds")
    assertEquals(Cooldown.describe(14500.millis), "15 seconds")
    assertEquals(Cooldown.describe(15.seconds), "15 seconds")
    assertEquals(Cooldown.describe(59.seconds), "59 seconds")
    assertEquals(Cooldown.describe(61.seconds), "61 seconds")
  }

  test("the too-soon message names the wait") {
    assertEquals(
      Cooldown.tooSoonMessage(11.seconds),
      "Easy there! The ducks need a moment. Try again in 11 seconds."
    )
    assertEquals(
      Cooldown.tooSoonMessage(1.milli),
      "Easy there! The ducks need a moment. Try again in 1 second."
    )
  }
