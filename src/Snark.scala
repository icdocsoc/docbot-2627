package docsoc

/** Pure logic: spotting a misspelt "DoCSoc" and picking a snarky reply. No Discord in here. */
object Snark:

  val Correct = "DoCSoc"

  // Any case-insensitive "docsoc" occurrence. Trailing letters (e.g. "docsocs") are left alone.
  private val Pattern = "(?i)docsoc".r

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
  private def normalise(text: String): String =
    Url.replaceAllIn(text.filterNot(Invisibles), " ")

  /** True if the text (ignoring URLs and invisible characters) contains a "docsoc" that isn't exactly "DoCSoc". */
  def misspelt(text: String): Boolean =
    Pattern.findAllIn(normalise(text)).exists(_ != Correct)

  /** The reply templates (see `Quotes`). */
  def replies: Vector[String] = Quotes.templates

  /** A random reply (never the same twice in a row) correcting to **DoCSoc**, mentioning the offender. */
  def nextReply(mention: String): String =
    Quotes.pick(s"**$Correct**", mention)
