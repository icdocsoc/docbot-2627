package docsoc.ducks

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._
import scala.util.control.NonFatal

/** Looks up a random duck reply without blocking the caller. This is the whole duck feature as far as a front end (such
  * as the Discord command) is concerned: it knows nothing about Discord or JDA, and it hands back either a
  * [[DuckReply]] or the user-facing failure message.
  *
  * The lookup runs on `executor`, so a slow Commons never blocks the calling thread (JDA's event thread). Whatever goes
  * wrong (the source reports a problem or throws, the executor rejects the task), `random` never throws and `onDone` is
  * called exactly once.
  *
  * @param source
  *   the cached duck list
  * @param executor
  *   where lookups run
  */
final class DuckLookup(source: DuckSource, executor: Executor):

  private val log = org.slf4j.LoggerFactory.getLogger("docsoc.ducks")

  /** Look a duck up on the executor and pass the result to `onDone`: `Right` with the reply to show, or `Left` with the
    * message to show instead. `onDone` runs on the executor's thread (or on the caller's if the executor runs tasks
    * inline) and is never called twice: if it throws, that is logged and swallowed.
    */
  def random(onDone: Either[String, DuckReply] => Unit): Unit =
    def finish(result: Either[String, DuckReply]): Unit =
      try onDone(result)
      catch case NonFatal(e) => log.error("The duck callback failed", e)
    try executor.execute(() => finish(lookup()))
    catch
      case NonFatal(e) =>
        log.error("Could not start the duck lookup", e)
        finish(Left(DuckReply.failureMessage))

  private def lookup(): Either[String, DuckReply] =
    try
      source.next() match
        case Right(duck)   => Right(DuckReply.of(duck))
        case Left(problem) =>
          log.warn(s"No duck available: $problem")
          Left(DuckReply.failureMessage)
    catch
      case NonFatal(e) =>
        log.error("Duck lookup failed", e)
        Left(DuckReply.failureMessage)

object DuckLookup:

  /** The lookup the bot uses: one shared duck list, fetched from Wikimedia Commons on first use and then reused for 6
    * hours, looked up on a small daemon thread pool. Plain daemon threads rather than virtual ones: `DuckSource` holds
    * a lock (`synchronized`) while it fetches, and on JDK 21 blocking inside `synchronized` pins a virtual thread's
    * carrier. Waiting callers are bounded by the fetch timeouts anyway. Daemon threads also never keep the JVM alive on
    * shutdown.
    */
  lazy val Default: DuckLookup =
    val threads = Executors.newFixedThreadPool(
      4,
      new ThreadFactory:
        private val counter = AtomicInteger(0)
        def newThread(r: Runnable): Thread =
          val t = Thread(r, s"randomduck-${counter.incrementAndGet()}")
          t.setDaemon(true)
          t
    )
    DuckLookup(DuckSource(() => Wikimedia().fetch(), 6.hours), threads)
