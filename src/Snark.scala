package docsoc

/** Pure logic: spotting a misspelt "DoCSoc" and picking a snarky reply. No Discord in here. */
object Snark:

  val Correct = "DoCSoc"

  // Any case-insensitive "docsoc" occurrence. Trailing letters (e.g. "docsocs") are left alone.
  private val Pattern = "(?i)docsoc".r

  // URLs (http(s)://... or www....) are ignored. \S+ also swallows Discord's `<https://...>` wrapper.
  private val Url = "(?i)\\b(?:https?://|www\\.)\\S+".r

  // Invisible/zero-width/bidi-control characters that can be slipped into a word: ZWSP, ZWNJ, ZWJ, word joiner,
  // BOM, soft hyphen, LRM, RLM, U+202A-U+202E, U+2066-U+2069, Mongolian vowel separator.
  private val Invisibles = "[\\u200B\\u200C\\u200D\\u2060\\uFEFF\\u00AD\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\u180E]".r

  /** Strip invisible characters first (so they can't split a word or hide inside a URL), then URLs. */
  private def normalise(text: String): String =
    Url.replaceAllIn(Invisibles.replaceAllIn(text, ""), " ")

  /** True if the text (ignoring URLs and invisible characters) contains a "docsoc" that isn't exactly "DoCSoc". */
  def misspelt(text: String): Boolean =
    Pattern.findAllIn(normalise(text)).exists(_ != Correct)

  /** The reply templates (see `Quotes`). */
  def replies: Vector[String] = Quotes.templates

  /** A random reply (never the same twice in a row) correcting to **DoCSoc**, mentioning the offender. */
  def nextReply(mention: String): String =
    Quotes.pick(s"**$Correct**", mention)
