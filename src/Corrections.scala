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

  // Invisible/zero-width/bidi-control characters that can be slipped into a word: ZWSP, ZWNJ, ZWJ, word joiner,
  // BOM, soft hyphen, LRM, RLM, U+202A-U+202E, U+2066-U+2069, Mongolian vowel separator.
  private val Invisibles = "[\\u200B\\u200C\\u200D\\u2060\\uFEFF\\u00AD\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\u180E]".r

  /** Strip invisible characters first (so they can't split a word or hide inside a URL), then URLs. */
  private[docsoc] def normalise(text: String): String =
    Url.replaceAllIn(Invisibles.replaceAllIn(text, ""), " ")

  // Any case-insensitive "docsoc" occurrence. Trailing letters (e.g. "docsocs") are left alone.
  val DoCSoc: Correction = Correction("DoCSoc", "(?i)docsoc".r)

  // The only accepted spelling is exactly "IC Hack". The pattern finds it in any case, with any run of spaces, dots,
  // hyphens or underscores (or none) between the parts (ICHack, ic-hack, I.C. Hack, IC  Hack), so variants get flagged.
  // Not inside another word: no letter before ("epic hack") or after ("ichacks"). Digits may follow: "ICHack26" is flagged.
  val ICHack: Correction = Correction("IC Hack", "(?i)(?<!\\p{L})i[\\s._-]*c[\\s._-]*hack(?!\\p{L})".r)

  val all: List[Correction] = List(DoCSoc, ICHack)

  /** The first correction the text breaks, if any. */
  def firstMisspelt(text: String): Option[Correction] = all.find(_.misspelt(text))
