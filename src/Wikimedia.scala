package docsoc

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import scala.concurrent.duration._
import scala.util.control.NonFatal

/** The HTTP side of the duck feature: one GET request to Wikimedia Commons, using the JDK's built-in client (no extra
  * dependency).
  *
  * Design: [[fetch]] has the shape `DuckSource` wants (`() => Either[String, String]`). It never throws: a non-200
  * status, an I/O error, a timeout or an interrupt all become a `Left` with a short message (an interrupt also restores
  * the thread's interrupt flag). Because `DuckSource` calls its fetch while holding a lock, the whole exchange, body
  * included, is capped by `requestTimeout`, so a slow server can delay other callers by at most that long.
  *
  * @param url
  *   what to request, injectable so tests can point at a local server
  * @param connectTimeout
  *   how long to wait for the TCP/TLS connection
  * @param requestTimeout
  *   how long the whole request (headers and body) may take
  */
final class Wikimedia(
    url: String = Ducks.RequestUrl,
    connectTimeout: FiniteDuration = Wikimedia.ConnectTimeout,
    requestTimeout: FiniteDuration = Wikimedia.RequestTimeout
):

  private val client = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofMillis(connectTimeout.toMillis))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  /** The duck list as one JSON body, or why there isn't one.
    *
    * Commons returns `imageinfo` for only part of the category per request, so this follows the response's `continue`
    * marker (at most [[Wikimedia.MaxRequests]] requests) and merges the pages (see [[Ducks.continuation]]). If any
    * request fails, the whole fetch fails, so `DuckSource` keeps serving its previous list instead of a partial one.
    */
  def fetch(): Either[String, String] =
    def loop(current: String, requests: Int): Either[String, String] =
      Ducks.continuation(current) match
        case None                                         => Right(current)
        case Some(_) if requests >= Wikimedia.MaxRequests =>
          Left(s"The duck list kept continuing after ${Wikimedia.MaxRequests} requests")
        case Some(more) =>
          val separator = if url.contains("?") then "&" else "?"
          for
            body <- get(s"$url$separator$more")
            merged <- Ducks.mergePages(current, body)
            done <- loop(merged, requests + 1)
          yield done
    get(url).flatMap(loop(_, 1))

  /** One GET: the response body, or why there isn't one. Only HTTP 200 counts as success. */
  private def get(target: String): Either[String, String] =
    try
      val request = HttpRequest
        .newBuilder(URI.create(target))
        .timeout(Duration.ofMillis(requestTimeout.toMillis))
        .header("User-Agent", Ducks.UserAgent)
        .header("Accept", "application/json")
        .GET()
        .build()
      val pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      try
        val response = pending.get(requestTimeout.toMillis, TimeUnit.MILLISECONDS)
        if response.statusCode() == 200 then Right(response.body())
        else Left(s"Wikimedia Commons answered with HTTP ${response.statusCode()}")
      catch
        case _: TimeoutException =>
          pending.cancel(true)
          Left("Wikimedia Commons took too long to answer")
        case _: InterruptedException =>
          pending.cancel(true)
          Thread.currentThread().interrupt()
          Left("The request to Wikimedia Commons was interrupted")
        case e: ExecutionException    => Left(describe(Option(e.getCause).getOrElse(e)))
        case _: CancellationException => Left("The request to Wikimedia Commons was cancelled")
    catch case NonFatal(e) => Left(describe(e))

  private def describe(e: Throwable): String = e match
    case _: HttpTimeoutException => "Wikimedia Commons took too long to answer"
    case _: IOException          => s"Could not reach Wikimedia Commons (${e.getClass.getSimpleName}: ${e.getMessage})"
    case _ => s"Could not fetch from Wikimedia Commons (${e.getClass.getSimpleName}: ${e.getMessage})"

object Wikimedia:
  val ConnectTimeout: FiniteDuration = 5.seconds
  val RequestTimeout: FiniteDuration = 10.seconds

  /** A safety cap on how many requests one [[Wikimedia.fetch]] makes while following `continue`. */
  val MaxRequests = 10
