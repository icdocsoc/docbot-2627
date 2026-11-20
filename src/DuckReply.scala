package docsoc

/** What the bot shows for a duck: plain strings that fit Discord's embed limits, with no JDA types so it can be unit
  * tested (`Bot.scala` turns it into an embed).
  *
  * Titles and credits come from Wikimedia contributors, so they are untrusted. Markdown characters are escaped (a title
  * can't turn into a link, bold text or a code block) and `@` and `<` are defused so a name like `@everyone` or
  * `<@123>` can't ping anyone. The bot also sends the reply with no allowed mentions, as a second line of defence.
  */
final case class DuckReply(title: String, imageUrl: String, description: String, pageUrl: String)

object DuckReply:

  /** Discord's limits for an embed title and description. */
  val MaxTitle = 256
  val MaxDescription = 4096

  /** The friendly message for when no duck could be fetched. */
  val failureMessage: String = "The ducks are on strike. Try again later."

  private val FallbackTitle = "Rubber duck"
  private val Ellipsis = "..."
  private val ZeroWidthSpace = Character.toString(0x200b)
  private val MaxCredit = 1000
  private val MaxTitleInDescription = 200
  private val MaxLinkLength = 1800
  private val Whitespace = "[\\s\\p{Cntrl}]+".r

  /** Characters Discord treats as markdown (`<` is escaped so that `<@123>`, `<#123>` and `<https://x>` stay text). */
  private val MarkdownChars = "\\*_~`|>[]()#<"

  /** One code point as it should appear in Discord text. */
  private def escapeCodePoint(cp: Int): String =
    if cp == '@' then "@" + ZeroWidthSpace
    else if MarkdownChars.indexOf(cp) >= 0 then "\\" + Character.toString(cp)
    else Character.toString(cp)

  /** Makes untrusted text safe to show: whitespace collapsed, markdown and mentions neutralised, and cut to at most
    * `max` characters (with `...` when something was cut). Never cuts in the middle of an escape or a surrogate pair.
    */
  def sanitize(raw: String, max: Int): String =
    val units = Whitespace.replaceAllIn(raw, " ").trim.codePoints().toArray.toVector.map(escapeCodePoint)
    if units.map(_.length).sum <= max then units.mkString
    else
      val budget = max - Ellipsis.length
      val kept = units.scanLeft(0)(_ + _.length).takeWhile(_ <= budget).size - 1
      units.take(kept).mkString.stripTrailing() + Ellipsis

  /** A URL that is safe inside `[text](url)`: spaces and brackets are percent-encoded so they can't end the link. */
  private def linkSafe(url: String): String =
    url.trim.flatMap {
      case ' '  => "%20"
      case '('  => "%28"
      case ')'  => "%29"
      case '<'  => "%3C"
      case '>'  => "%3E"
      case '\n' => "%0A"
      case c    => c.toString
    }

  /** The reply for a duck: the title (linking to the Commons page, set by the bot), the picture, and a description
    * crediting the author, in the form `[Title](page) by Credit, via Wikimedia Commons`.
    */
  def of(duck: Duck): DuckReply =
    val title = Option(sanitize(duck.title, MaxTitle)).filter(_.nonEmpty).getOrElse(FallbackTitle)
    val credit = sanitize(duck.credit, MaxCredit)
    val link = linkSafe(duck.pageUrl)
    val shownTitle = Option(sanitize(duck.title, MaxTitleInDescription)).filter(_.nonEmpty).getOrElse(FallbackTitle)
    val linked =
      if link.length <= MaxLinkLength then s"[$shownTitle]($link)" else shownTitle
    val description = s"$linked by $credit, via Wikimedia Commons"
    DuckReply(title, duck.imageUrl.trim, description, duck.pageUrl.trim)
