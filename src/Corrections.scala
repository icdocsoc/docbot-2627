package docsoc

import scala.util.matching.Regex

/** A name that must be spelt exactly `correct`. `pattern` finds every spelling of it, right or wrong. */
final case class Correction(correct: String, pattern: Regex):

  /** True if the text (ignoring URLs and invisible characters) contains a match that isn't exactly `correct`. */
  def misspelt(text: String): Boolean =
    pattern.findAllIn(Corrections.normalise(text)).exists(_ != correct)

  /** A random snarky reply (never the same twice in a row) correcting to **correct**, mentioning the offender. */
  def reply(mention: String): String =
    Quotes.pick(s"**$correct**", mention)

/** Pure logic: every spelling correction the bot enforces. No Discord in here. */
object Corrections:

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

  // Any case-insensitive "docsoc" occurrence. Trailing letters (e.g. "docsocs") are left alone.
  val DoCSoc: Correction = Correction("DoCSoc", "(?i)docsoc".r)

  // The only accepted spelling is exactly "IC Hack". The pattern finds it in any case, with any run of spaces, dots,
  // hyphens or underscores (or none) between the parts (ICHack, ic-hack, I.C. Hack, IC  Hack), so variants get flagged.
  // Not inside another word: no letter before ("epic hack") or after ("ichacks"). Digits may follow: "ICHack26" is flagged.
  val ICHack: Correction = Correction("IC Hack", "(?i)(?<!\\p{L})i[\\s._-]*c[\\s._-]*hack(?!\\p{L})".r)

  val all: List[Correction] = List(DoCSoc, ICHack)

  /** The first correction the text breaks, if any. */
  def firstMisspelt(text: String): Option[Correction] = all.find(_.misspelt(text))
