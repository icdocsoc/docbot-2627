package docsoc

/** The concrete corrections the bot enforces: pure configuration, no logic (that's in `Correction`).
  *
  * To police another name, define a `Correction` below and add it to `all`. Order matters: when a message breaks
  * several, only the first one in `all` is replied to.
  */
object Corrections:

  // Any case-insensitive "docsoc" occurrence. Trailing letters (e.g. "docsocs") are left alone.
  val DoCSoc: Correction = Correction("DoCSoc", "(?i)docsoc".r)

  // The only accepted spelling is exactly "IC Hack". The pattern finds it in any case, with any run of spaces, dots,
  // hyphens or underscores (or none) between the parts (ICHack, ic-hack, I.C. Hack, IC  Hack), so variants get flagged.
  // Not inside another word: no letter before ("epic hack") or after ("ichacks"). Digits may follow: "ICHack26" is flagged.
  val ICHack: Correction = Correction("IC Hack", "(?i)(?<!\\p{L})i[\\s._-]*c[\\s._-]*hack(?!\\p{L})".r)

  /** Every correction the bot enforces, in priority order. */
  val all: List[Correction] = List(DoCSoc, ICHack)
