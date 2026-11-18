package docsoc

import scala.util.matching.Regex

/** A name that must be spelt exactly `correct` (or exactly one of `alsoAccepted`). `pattern` finds every spelling of
  * it, right or wrong. Replies always correct to `correct`.
  *
  * This is the generic mechanism; the concrete corrections the bot enforces live in `Corrections`.
  */
final case class Correction(correct: String, pattern: Regex, alsoAccepted: Set[String] = Set.empty):

  /** True if the text (ignoring URLs and invisible characters) contains a match that is neither `correct` nor one of
    * `alsoAccepted`, character for character.
    */
  def misspelt(text: String): Boolean =
    pattern.findAllIn(Correction.normalise(text)).exists(m => m != correct && !alsoAccepted.contains(m))

  /** A random snarky reply (never the same twice in a row) correcting to **correct**, mentioning the offender. */
  def reply(mention: String): String =
    Quotes.pick(s"**$correct**", mention)

/** Pure logic shared by every correction: text normalisation and lookup. No Discord, no concrete corrections. */
object Correction:

  // URLs (http(s)://... or www....) are ignored. \S+ also swallows Discord's `<https://...>` wrapper.
  private val Url = "(?i)\\b(?:https?://|www\\.)\\S+".r

  // Invisible/zero-width/bidi-control characters that can be slipped into a word. A Set[Char] is itself a Char => Boolean,
  // so normalise can use it directly as a filter predicate.
  private[docsoc] val Invisibles: Set[Char] =
    Set(
      '\u200B', '\u200C', '\u200D', '\u2060', // zero-width space, non-joiner, joiner, word joiner
      '\uFEFF', // BOM / zero-width no-break space
      '\u00AD', // soft hyphen
      '\u200E', '\u200F', // LRM, RLM
      '\u180E' // Mongolian vowel separator
    ) ++ ('\u202A' to '\u202E') // bidi embeddings/overrides
      ++ ('\u2066' to '\u2069') // bidi isolates

  /** Strip invisible characters first (so they can't split a word or hide inside a URL), then URLs. */
  private[docsoc] def normalise(text: String): String =
    Url.replaceAllIn(text.filterNot(Invisibles), " ")

  /** The first of `corrections` (in list order) that the text breaks, if any. */
  def firstMisspelt(corrections: Seq[Correction], text: String): Option[Correction] =
    corrections.find(_.misspelt(text))
