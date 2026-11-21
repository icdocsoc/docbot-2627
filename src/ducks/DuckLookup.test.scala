package docsoc.ducks

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.util.Random

class DuckLookupTests extends munit.FunSuite:

  private val body =
    """{"query":{"pages":{"1":{"title":"File:Yellow duck.jpg","imageinfo":[{
      |"thumburl":"https://img.example/t.jpg","url":"https://img.example/o.jpg",
      |"descriptionurl":"https://commons.example/File:Yellow_duck.jpg","mime":"image/jpeg",
      |"extmetadata":{"Artist":{"value":"Jane"},"LicenseShortName":{"value":"CC BY 4.0"}}}]}}}}""".stripMargin

  private val direct: Executor = (r: Runnable) => r.run()
  private val rejecting: Executor = _ => throw RejectedExecutionException("full")
  private def source(fetch: () => Either[String, String]) = DuckSource(fetch, 1.hour)
  private val failure = Left(DuckReply.failureMessage)

  private def results(lookup: DuckLookup): List[Either[String, DuckReply]] =
    val seen = ListBuffer.empty[Either[String, DuckReply]]
    lookup.random(seen += _)
    seen.toList

  test("a duck becomes a reply") {
    val got = results(DuckLookup(source(() => Right(body)), direct))
    assertEquals(got.size, 1)
    assertEquals(got.head.map(_.title), Right("Yellow duck"))
  }
  test("a source that reports a problem gives the failure message") {
    assertEquals(results(DuckLookup(source(() => Left("boom")), direct)), List(failure))
  }
  test("a fetch that throws gives the failure message") {
    assertEquals(results(DuckLookup(source(() => throw RuntimeException("boom")), direct)), List(failure))
  }
  test("a source that throws gives the failure message") {
    val broken = DuckSource(() => Right(body), 1.hour, () => throw RuntimeException("clock"), Random)
    assertEquals(results(DuckLookup(broken, direct)), List(failure))
  }
  test("an executor that rejects the task gives the failure message, once") {
    assertEquals(results(DuckLookup(source(() => Right(body)), rejecting)), List(failure))
  }
  test("a callback that throws is not called again and does not reach the caller") {
    var calls = 0
    DuckLookup(source(() => Right(body)), direct).random { _ =>
      calls += 1
      throw RuntimeException("callback")
    }
    assertEquals(calls, 1)
    calls = 0
    DuckLookup(source(() => Right(body)), rejecting).random { _ =>
      calls += 1
      throw RuntimeException("callback")
    }
    assertEquals(calls, 1)
  }
  test("the lookup runs on the executor's thread, not the caller's") {
    val pool = Executors.newSingleThreadExecutor()
    try
      val latch = CountDownLatch(1)
      val got = AtomicReference[Either[String, DuckReply]]()
      val thread = AtomicReference[Thread]()
      val release = CountDownLatch(1)
      val slow = source { () =>
        release.await(5, TimeUnit.SECONDS)
        Right(body)
      }
      DuckLookup(slow, pool).random { r =>
        thread.set(Thread.currentThread())
        got.set(r)
        latch.countDown()
      }
      assertEquals(latch.getCount, 1L) // still waiting: the caller was not blocked
      release.countDown()
      assert(latch.await(5, TimeUnit.SECONDS))
      assertNotEquals(thread.get(), Thread.currentThread())
      assert(got.get().isRight)
    finally pool.shutdownNow()
  }
