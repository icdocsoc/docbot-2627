package docsoc.ducks

import scala.util.Random

import docsoc.Picks

/** The snarky captions that go with every duck, in the spirit of the spelling-correction `Quotes` pool.
  *
  * Placeholder: `{user}` is a mention of whoever ran the command (used by a minority of captions). It is the only
  * placeholder. Captions are short, contain no `@everyone`/`@here` and no unbalanced markdown, so they are safe as
  * plain Discord message content; the bot sends them with no allowed mentions, so `{user}` shows as a mention without
  * pinging anyone.
  *
  * Keep the sections roughly balanced: no single franchise or genre should dominate the pool. The pool is separate from
  * `Quotes` because it has a different placeholder set and its own "previous pick" (a caption never follows itself, but
  * a correction may follow a caption); both use [[docsoc.Picks]] for the draw.
  */
object DuckCaptions:

  /** The longest a caption may be after filling in the mention. Discord's message limit is 2000; the pool itself stays
    * well under 200 per template.
    */
  val MaxLength = 2000

  val templates: Vector[String] = Vector(
    // Monty Python
    "A duck weighs the same as a witch, {user}. Therefore, this duck is a witch. Burn it! ...Actually, it is only a rubber duck.",
    "'Tis but a scratch, said the duck. It was a rubber duck, so it was telling the truth.",
    "And now for something completely different: a duck.",
    "What is the airspeed velocity of an unladen duck? African or European? Nobody knows, but this one floats.",
    "Bring out your dead! ...This one is a duck. It is not dead, it is just rubber. It never lived.",
    "Nobody expects the Spanish Inquisition! Nor a duck, {user}. Yet here we are.",
    "This duck is no more. It has ceased to be. It's bleedin' demised... Oh no, wait, it is rubber. It was never alive.",

    // Rubber duck debugging
    "Explain your code to the duck, {user}. By the third sentence you will find the bug. The duck will pretend not to have known.",
    "Rubber duck debugging: the only code review where the reviewer never says 'works on my machine'.",
    "Have you tried explaining it to the duck? The duck has seen things. The duck knows about your off-by-one.",
    "Step one: describe the problem to the duck, line by line. Step two: spot the bug. Step three: tell nobody the duck solved it.",
    "The duck does not judge your variable names. The duck is silently judging your variable names.",
    "This duck has reviewed more pull requests than most of your team, and never once asked you to rebase.",
    "It works, and nobody knows why, {user}. Ask the duck. The duck knows. The duck is not telling.",
    "Walk the duck through your function slowly. If the duck frowns, you wrote a bug. If the duck smiles, you probably still did.",

    // Shakespeare
    "To quack, or not to quack: that is the question.",
    "All the world's a pond, and all the ducks and ducklings merely players.",
    "Is this a duck which I see before me, the squeak toward my hand? Come, let me clutch thee.",
    "Something is rotten in the state of Denmark, {user}, and it is not this duck. The duck is spotless.",
    "Cry havoc, and let slip the ducks of war!",
    "If you prick us, do we not squeak? If you tickle us, do we not float?",

    // History and politics
    "Duck and cover, {user}! ...No, wait, wrong kind of duck.",
    "Veni, vidi, quaci. I came, I saw, I quacked.",
    "We shall fight on the beaches, we shall fight in the bathtubs, we shall never surrender the duck.",
    "Ask not what your duck can do for you, {user}. Ask what you can do for your duck.",
    "Magna Carta, 1215: the barons demanded rights, the ducks demanded bread. Only one of them got what they wanted.",
    "One small step for duck, one giant leap for ducks-kind.",
    "Keep calm and quack on.",

    // James Bond
    "The name's Duck. Rubber Duck.",
    "A bath, shaken, not stirred, {user}. The duck is already in it.",
    "Do you expect me to squeak, Mr Bond? No, Mr Bond, I expect you to float.",
    "Goldfinger had gold. Dr No had an island. This villain has a rubber duck and a very smug look.",

    // Stargate
    "Indeed. A duck.",
    "SG-1 has faced the Goa'uld, the Replicators and the Ori, {user}. None of them have beaten the rubber duck.",
    "Chevron seven locked. The duck has arrived on the other side of the wormhole, and it is somehow still dry.",
    "Daniel Jackson has translated the duck. It says 'quack'. The translation took three seasons.",

    // Star Trek
    "Make it so, Number One. Release the duck.",
    "Dammit, Jim, I'm a doctor, not a duck whisperer!",
    "Resistance is futile, {user}. You will be assimilated into the duck collective.",
    "Captain's log: we have found a rubber duck drifting in space. It is entirely unharmed, and appears to be judging us.",
    "Space: the final frontier. These are the voyages of the starship Duckerprise.",

    // MCU
    "I am inevitable. So is this duck.",
    "Avengers, assemble! ...Okay, the duck is here. The others are in the bath.",
    "I can do this all day, {user}. So can the duck. It is rubber.",
    "Another! ...duck, cried Thor, smashing his mug.",
    "Dormammu, I've come to bargain. The duck's price: one bath, no questions asked.",

    // Musicals
    "Do you hear the ducks sing? Not really. It is rubber.",
    "I am not throwing away my shot, but you are throwing away your duck, {user}.",
    "The hills are alive with the sound of quacking.",
    "It's the circle of life, and it moves us all through the bath, past the duck.",

    // Brooklyn Nine-Nine
    "Cool cool cool cool cool. No doubt no doubt. The duck is judging us.",
    "Nine-Nine! Also the number of times this duck will be mentioned today. It is already over.",
    "Captain Holt would like to inform you that this duck is acceptable. The duck was not seeking his approval.",

    // Original duck puns
    "Fowl play is not suspected. This duck is entirely innocent.",
    "Duck, duck, goose? No, duck, duck, duck. The goose has been fired.",
    "Life is like a rubber duck: it bobs along, squeaks when squeezed, and has no idea what it is doing.",
    "Waddle you do without this duck, {user}? Nothing. Nothing at all.",
    "Don't worry, {user}. Everything is going swimmingly. Mostly because of the duck."
  )

  private val User = "{user}"

  private val lock = new Object
  private var last = -1 // index of the previous pick, guarded by `lock`

  /** A caption with the mention filled in, cut to [[MaxLength]] characters if that mention was absurdly long. The
    * mention is inserted as it is (a Discord mention such as `<@123>` must stay intact), in one pass, so a mention that
    * itself contains `{user}` is not expanded again.
    */
  def fill(template: String, mention: String): String =
    val filled = template.replace(User, mention)
    if filled.length <= MaxLength then filled
    else
      val ellipsis = "..."
      var end = MaxLength - ellipsis.length
      if Character.isHighSurrogate(filled.charAt(end - 1)) then end -= 1 // never split a surrogate pair
      filled.substring(0, end) + ellipsis

  /** A uniformly random caption (never the same one twice in a row), with the mention filled in. Thread-safe. */
  def pick(mention: String, rng: Random = Random): String =
    val i = lock.synchronized {
      val next = Picks.indexAvoiding(templates.size, last, rng)
      last = next
      next
    }
    fill(templates(i), mention)
