package docsoc

import java.time.Instant

import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.util.control.NonFatal

/** One rubber duck photo from Wikimedia Commons, ready to show.
  *
  * @param title
  *   a readable name (the file name without `File:`, the extension and underscores)
  * @param imageUrl
  *   a direct link to the picture (the 800px-wide thumbnail Commons made, or the original if there is none)
  * @param pageUrl
  *   the Commons file page, which carries the full attribution and licence (use it as the link)
  * @param credit
  *   a short plain-text line such as `Jane Doe, CC BY-SA 4.0`
  */
final case class Duck(title: String, imageUrl: String, pageUrl: String, credit: String)

/** Rubber duck photos from the Wikimedia Commons category `Category:Rubber_ducks` (no API key needed).
  *
  * Design: this file knows nothing about Discord, JDA or HTTP, so everything here can be unit tested. The HTTP call is
  * injected into [[DuckSource]] as a plain `() => Either[String, String]` that returns the response body (and
  * [[Ducks.RequestUrl]] and [[Ducks.UserAgent]] are what it should use).
  *
  * Things worth knowing about the API response (`generator=categorymembers`, `prop=imageinfo`):
  *   - `query.pages` is an object keyed by page id. Each page has a `title` (`File:Name with spaces.jpg`) and an
  *     `imageinfo` array whose first element has `thumburl`, `url`, `descriptionurl`, `mime` and `extmetadata`.
  *   - The category also holds audio, GIMP files and so on, so entries are filtered by mime type.
  *   - Commons only returns `imageinfo` (with `extmetadata`) for the first 50 files per request and sets a top-level
  *     `continue` marker for the rest. Pages beyond that come back with a title only. [[Ducks.parse]] skips them, so
  *     the pool is those first 50 files minus the non-images.
  *   - Only direct files are used, not the subcategories.
  */
object Ducks:

  /** The request that lists the ducks: files directly in the category, with 800px thumbnails and licence metadata. */
  val RequestUrl: String =
    "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=categorymembers" +
      "&gcmtitle=Category:Rubber_ducks&gcmtype=file&gcmlimit=max&prop=imageinfo" +
      "&iiprop=url|mime|extmetadata&iiurlwidth=800"

  /** Wikimedia asks automated clients to identify themselves, otherwise they may be throttled or blocked. */
  val UserAgent: String = "DoCBoT/1.0 (https://github.com/icdocsoc/docbot-2627)"

  /** Used when Commons doesn't say who made a picture. */
  val DefaultArtist: String = "Wikimedia Commons contributors"

  /** The picture formats Discord can show in an embed. Everything else (svg, tiff, pdf, video, audio, ...) is dropped.
    */
  val ImageMimeTypes: Set[String] = Set("image/jpeg", "image/png", "image/gif", "image/webp")

  private val FilePrefix = "File:"
  private val Extension = "\\.[A-Za-z0-9]{1,5}$".r
  private val LineBreakTag = "(?i)</?(?:br|p|div|li)\\b[^>]*>".r
  private val AnyTag = "<[^>]*>".r
  private val Entity = "&(#[0-9]+|#[xX][0-9A-Fa-f]+|[A-Za-z]+);".r
  private val Whitespace = "\\s+".r

  private val NamedEntities: Map[String, String] =
    Map("amp" -> "&", "lt" -> "<", "gt" -> ">", "quot" -> "\"", "apos" -> "'", "nbsp" -> " ")

  /** A readable name for a Commons file title: `File:Rubber_duck in bowl.jpg` becomes `Rubber duck in bowl`. */
  def cleanTitle(raw: String): String =
    val noPrefix = if raw.startsWith(FilePrefix) then raw.drop(FilePrefix.length) else raw
    Whitespace.replaceAllIn(Extension.replaceFirstIn(noPrefix.trim, "").replace('_', ' '), " ").trim

  /** Plain text from a snippet of HTML: tags removed, basic entities decoded, whitespace collapsed.
    *
    * Commons artist names are often links like `<a href="...">Jane Doe</a>`. Entities are decoded once (so `&amp;lt;`
    * becomes `&lt;`, not `<`), unknown ones are left as they are, and numeric ones (`&#233;`, `&#xE9;`) work too.
    */
  def stripHtml(html: String): String =
    val noTags = AnyTag.replaceAllIn(LineBreakTag.replaceAllIn(html, " "), "")
    val decoded =
      Entity.replaceAllIn(noTags, m => java.util.regex.Matcher.quoteReplacement(decodeEntity(m.group(0), m.group(1))))
    Whitespace.replaceAllIn(decoded, " ").trim

  private def decodeEntity(whole: String, name: String): String =
    val codePoint =
      if name.startsWith("#x") || name.startsWith("#X") then parseHex(name.drop(2))
      else if name.startsWith("#") then name.drop(1).toIntOption
      else None
    codePoint match
      case Some(cp) if Character.isValidCodePoint(cp) && cp > 0 => String(Character.toChars(cp))
      case Some(_)                                              => whole
      case None                                                 => NamedEntities.getOrElse(name, whole)

  private def parseHex(s: String): Option[Int] = scala.util.Try(Integer.parseInt(s, 16)).toOption

  /** The credit line: `Artist, LICENSE`, with fallbacks when Commons has no (or an empty) artist or licence. */
  def credit(artist: Option[String], license: Option[String]): String =
    val who = artist.map(stripHtml).filter(_.nonEmpty).getOrElse(DefaultArtist)
    license.map(stripHtml).filter(_.nonEmpty).fold(who)(l => s"$who, $l")

  /** Every usable duck in an API response.
    *
    * Keeps direct image files with a mime type in [[ImageMimeTypes]] and with an image link and a file page link.
    * Entries without `imageinfo` are skipped (see the note on the first 50 files above).
    *
    * @return
    *   `Left` with a readable message if the body isn't JSON, is an API error, or contains no usable duck.
    */
  def parse(json: String): Either[String, Vector[Duck]] =
    val root =
      try Right(ujson.read(json))
      catch case NonFatal(e) => Left(s"Could not parse the duck list as JSON: ${e.getMessage}")
    root.flatMap { value =>
      value.objOpt match
        case None      => Left("Unexpected duck list: expected a JSON object")
        case Some(obj) =>
          obj.get("error") match
            case Some(err) =>
              val code = err.objOpt.flatMap(_.get("code")).flatMap(_.strOpt).getOrElse("unknown")
              val info = err.objOpt.flatMap(_.get("info")).flatMap(_.strOpt).getOrElse("no details")
              Left(s"Wikimedia API error ($code): $info")
            case None =>
              val pages = obj.get("query").flatMap(_.objOpt).flatMap(_.get("pages")).flatMap(_.objOpt)
              val ducks = pages.toVector.flatMap(_.values).flatMap(toDuck)
              Either.cond(ducks.nonEmpty, ducks, "The duck list contained no usable images")
    }

  private def toDuck(page: ujson.Value): Option[Duck] =
    for
      title <- page.objOpt.flatMap(_.get("title")).flatMap(_.strOpt)
      info <- page.obj.get("imageinfo").flatMap(_.arrOpt).flatMap(_.headOption).flatMap(_.objOpt)
      mime <- info.get("mime").flatMap(_.strOpt) if ImageMimeTypes.contains(mime)
      imageUrl <- nonBlank(info, "thumburl").orElse(nonBlank(info, "url"))
      pageUrl <- nonBlank(info, "descriptionurl")
    yield
      val meta = info.get("extmetadata").flatMap(_.objOpt)
      def metaValue(key: String) = meta.flatMap(_.get(key)).flatMap(_.objOpt).flatMap(_.get("value")).flatMap(_.strOpt)
      Duck(cleanTitle(title), imageUrl, pageUrl, credit(metaValue("Artist"), metaValue("LicenseShortName")))

  private def nonBlank(obj: scala.collection.mutable.Map[String, ujson.Value], key: String): Option[String] =
    obj.get(key).flatMap(_.strOpt).map(_.trim).filter(_.nonEmpty)

  private val lock = new Object
  private var last: Option[String] = None // page URL of the previous pick, guarded by `lock`

  /** A uniformly random duck, never the same one twice in a row (as long as the pool has at least two).
    *
    * The previous pick is remembered by its page URL, so this stays correct when the pool is refreshed.
    *
    * @throws IllegalArgumentException
    *   if `ducks` is empty
    */
  def pick(ducks: Vector[Duck], rng: Random = Random): Duck =
    require(ducks.nonEmpty, "no ducks to pick from")
    lock.synchronized {
      val n = ducks.size
      val lastIndex = last.map(url => ducks.indexWhere(_.pageUrl == url)).getOrElse(-1)
      val i =
        if lastIndex < 0 || n < 2 then rng.nextInt(n)
        else
          val j = rng.nextInt(n - 1) // uniform over every index except `lastIndex`
          if j >= lastIndex then j + 1 else j
      val duck = ducks(i)
      last = Some(duck.pageUrl)
      duck
    }

/** Serves random ducks from a cached copy of the Commons list.
  *
  * The list is fetched lazily on the first [[next]], then reused for `ttl`. If a refresh fails, the stale list keeps
  * being served (and the fetch is retried on the next call); with no list at all, [[next]] returns the error. Safe to
  * call from several threads: calls are serialised, so concurrent callers share one fetch instead of each making their
  * own (which also means the injected `fetch` should have its own timeout).
  *
  * @param fetch
  *   returns the response body for [[Ducks.RequestUrl]], or a message saying why it couldn't (the real HTTP client is
  *   supplied by the bot)
  * @param ttl
  *   how long a successfully parsed list is trusted
  * @param now
  *   the clock, injectable for tests
  * @param rng
  *   the randomness, injectable for tests
  */
final class DuckSource(
    fetch: () => Either[String, String],
    ttl: FiniteDuration,
    now: () => Instant = () => Instant.now(),
    rng: Random = Random
):

  private final case class Cached(ducks: Vector[Duck], fetchedAt: Instant)

  private val lock = new Object
  private var cache: Option[Cached] = None // guarded by `lock`

  /** A random duck: from the cache while it is fresh, otherwise after a refresh (falling back to the stale cache). */
  def next(): Either[String, Duck] = lock.synchronized {
    val t = now()
    val pool = cache.filter(c => t.isBefore(c.fetchedAt.plusMillis(ttl.toMillis))) match
      case Some(fresh) => Right(fresh.ducks)
      case None        => refresh(t)
    pool.map(Ducks.pick(_, rng))
  }

  private def refresh(t: Instant): Either[String, Vector[Duck]] =
    val loaded =
      try fetch().flatMap(Ducks.parse)
      catch case NonFatal(e) => Left(s"Could not fetch the duck list: ${e.getMessage}")
    loaded match
      case Right(ducks) =>
        cache = Some(Cached(ducks, t))
        loaded
      case Left(_) => cache.fold(loaded)(stale => Right(stale.ducks))
