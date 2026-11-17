package docsoc

import java.util.concurrent.atomic.AtomicInteger

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

  /** Replies, round-robin. `{user}` is replaced with a mention of the offender. */
  val replies: Vector[String] = Vector(
    // Monty Python
    "'Tis but a typo! ...No wait, it's a full-on misspelling. It's **DoCSoc**.",
    "This spelling is no more. It has ceased to be. It's expired and gone to meet its maker. It's an ex-spelling. It's **DoCSoc**.",
    "We are the Knights Who Say \"DoCSoc\"! ...and we demand you spell it correctly, or we shall demand another shrubbery.",
    "NOBODY expects the Spelling Inquisition! Our chief weapon is correction... correction and fear... It's **DoCSoc**. Our *two* weapons...",
    "\"Romanes eunt domus\"? No! **DoCSoc**. Now write it out a hundred times before sunrise, or I'll cut your tongue out. Hmm? Well? Off you go.",
    // Pop culture, across the eras
    "Et tu, {user}? Then fall, Caesar. It's **DoCSoc**.",
    "These aren't the capitals you're looking for. *waves hand* It's **DoCSoc**.",
    "I'm sorry, {user}. I'm afraid I can't let that spelling stand. It's **DoCSoc**.",
    "Frankly, my dear, I do give a damn. It's **DoCSoc**.",
    "YOU SHALL NOT PASS... with that spelling. It's **DoCSoc**.",
    // MCU
    "I am inevitable. And so is the correct spelling: **DoCSoc**.",
    "I can do this all day, {user}. It's **DoCSoc**.",
    "Avengers... assemble. Around the correct spelling: **DoCSoc**.",
    // Star Trek
    "Make it so, {user}. **DoCSoc**.",
    "Resistance is futile. You will be assimilated into spelling it **DoCSoc**.",
    "Dammit, Jim, I'm a doctor, not a spellchecker! It's **DoCSoc**.",
    // Brooklyn Nine-Nine
    "Cool cool cool cool cool. No doubt no doubt. Except it's spelt **DoCSoc**.",
    "Noice. Smort. ...Not you though, {user}. It's **DoCSoc**.",
    "Nine-Nine! Which is also how many typos I'll tolerate. Minus nine. It's **DoCSoc**."
  )

  private val counter = AtomicInteger(0)

  def nextReply(mention: String): String =
    val i = Math.floorMod(counter.getAndIncrement(), replies.size)
    replies(i).replace("{user}", mention)
