package docsoc

import scala.util.Random

/** A pool of snarky correction templates, shared by every "you spelt it wrong" feature.
  *
  * Placeholders:
  *   - `{correct}`: the correct spelling, already formatted by the caller (e.g. `**DoCSoc**`). Every template has it.
  *   - `{user}`: a mention of the offender (used by a minority of templates).
  *
  * Keep the sections roughly balanced: no single franchise or genre should dominate the pool.
  */
object Quotes:

  val templates: Vector[String] = Vector(
    // Monty Python
    "'Tis but a typo! ...No wait, it's a full-on misspelling. It's {correct}.",
    "This spelling is no more. It has ceased to be. It's expired and gone to meet its maker. It's an ex-spelling. It's {correct}.",
    "We are the Knights Who Say {correct}! ...and we demand you spell it correctly, or we shall demand another shrubbery. It's {correct}.",

    // Pop culture, across the eras
    "Et tu, {user}? Then fall, Caesar. It's {correct}.",
    "I'm sorry, {user}. I'm afraid I can't let that spelling stand. It's {correct}.",
    "Frankly, my dear, I do give a damn. It's {correct}.",
    "YOU SHALL NOT PASS... with that spelling. It's {correct}.",

    // MCU
    "I am inevitable. And so is the correct spelling: {correct}.",
    "I can do this all day, {user}. It's {correct}.",
    "Avengers... assemble. Around the correct spelling: {correct}.",
    "Dormammu, I've come to bargain. Spell it {correct}, {user}, and I'll stop correcting you. Dormammu, I've come to bargain...",
    "That's my secret, {user}: I'm always correcting. It's {correct}.",
    "Wakanda forever. And so is {correct}.",
    "Hulk smash! ...the typo. It's {correct}.",
    "On your left. And so is the right spelling: {correct}.",
    "I understood that reference, and I understand that spelling is wrong. It's {correct}.",
    "We have a Hulk. We also have a dictionary. It says {correct}.",

    // DCU
    "I'm Batman. And Batman spells it {correct}.",
    "Why so serious, {user}? It's {correct}.",
    "It's not who you are underneath, it's what you spell that defines you. It's {correct}.",
    "Holy typo, Batman! It's {correct}.",
    "Are you not entertained? By {correct}? You should be.",
    "Some men just want to watch the world spell it {correct}.",
    "This is Sparta! ...and Sparta spells it {correct}.",
    "In brightest day, in blackest night, no typo shall escape my sight. It's {correct}.",
    "Even the Flash couldn't outrun that typo, {user}. It's {correct}.",
    "The Joker would call this a joke. The correct punchline is {correct}.",

    // Star Wars
    "These aren't the capitals you're looking for. *waves hand* It's {correct}.",
    "I find your lack of capitals disturbing. It's {correct}.",
    "Do or do not. There is no 'try' with spelling. It's {correct}.",
    "Help me, {user}, you're my only hope. Spell it {correct}.",
    "That's no moon. That's a typo. It's {correct}.",
    "I have a bad feeling about this spelling. It's {correct}.",
    "Never tell me the odds of spelling it right. It's {correct}.",
    "It's a trap! ...and the trap is spelling it wrong. It's {correct}.",
    "Hello there! ...General {user}. It's {correct}.",
    "This is the way. {correct} is the way.",
    "Fear is the path to the dark side. So is misspelling. It's {correct}.",
    "Execute Order 66 on that typo. It's {correct}.",
    "Chewie, we're home. And home is spelt {correct}.",
    "So uncivilised. Spell it {correct}, {user}.",

    // Star Trek
    "Make it so, {user}. {correct}.",
    "Resistance is futile. You will be assimilated into spelling it {correct}.",
    "Dammit, Jim, I'm a doctor, not a spellchecker! It's {correct}.",
    "Live long and prosper, {user}. And spell it {correct}.",
    "Highly illogical, {user}. It's {correct}.",
    "Fascinating. The correct spelling is {correct}.",
    "Engage. The correct spelling, that is: {correct}.",
    "Shaka, when the walls fell... and so did your spelling. It's {correct}.",
    "Tea, Earl Grey, hot. And {correct}, spelt correctly.",
    "The needs of the many outweigh the needs of the few, {user}. Spell it {correct}.",
    "Space: the final frontier. Spelling: the first. It's {correct}.",
    "Captain's log: a crew member has spelt it wrong again. It's {correct}.",
    "Set phasers to correct. It's {correct}.",
    "Things are only impossible until they're not. Like you spelling it right, {user}. It's {correct}.",

    // Brooklyn Nine-Nine
    "Cool cool cool cool cool. No doubt no doubt. Except it's spelt {correct}.",
    "Noice. Smort. ...Not you though, {user}. It's {correct}.",
    "Nine-Nine! Which is also how many typos I'll tolerate. Minus nine. It's {correct}.",

    // Musicals
    "Do you hear the people sing? Not when they spell it like that, {user}. It's {correct}.",
    "The Phantom of the Opera is there, inside your keyboard. Sing it right: {correct}.",
    "I am not throwing away my shot, {user}. It's {correct}.",
    "Hakuna Matata means no worries, but that spelling does not. It's {correct}.",
    "It's the circle of life, and it moves us all towards {correct}.",
    "Grease is the word, and the word is {correct}.",
    "Mamma mia, here I go again, correcting people. It's {correct}.",
    "All that jazz, and none of that spelling. It's {correct}.",
    "Five hundred twenty-five thousand six hundred minutes, and you still can't spell it? It's {correct}.",
    "Please, sir, I want some more... correct spelling. It's {correct}.",
    "A spoonful of sugar helps the medicine go down, {user}. The medicine is {correct}.",

    // Shakespeare
    "To spell, or not to spell: that is the question. The answer is {correct}.",
    "Double, double, toil and trouble; fix that spelling on the double. It's {correct}.",
    "Out, damned spot! Out, damned typo! It's {correct}.",
    "What's in a name, {user}? Quite a lot of capital letters, actually. It's {correct}.",
    "Friends, Romans, countrymen, lend me your ears: it's {correct}.",
    "A spelling! A spelling! My kingdom for a correct spelling! It's {correct}.",
    "Some are born great, some achieve greatness, and some have {correct} thrust upon them.",
    "O, beware, {user}, of the green-eyed monster; it is jealous of correct spelling. It's {correct}.",

    // History and politics
    "Veni, vidi, vici. I came, I saw, I corrected: {correct}.",
    "We shall fight on the beaches, we shall fight on the landing grounds, and we shall never surrender {correct}.",
    "Alea iacta est, {user}. The die is cast, and it spells {correct}.",
    "I have nothing to offer but blood, toil, tears and sweat, {user}. And the spelling: {correct}.",
    "Never in the field of human conflict was so much owed by so many to so few capital letters. It's {correct}.",
    "England expects that every man will do his duty, {user}. Spelling {correct} included.",
    "I have the heart and stomach of a king, said Elizabeth I at Tilbury, and I have the spelling of one too. It's {correct}.",
    "Will no one rid me of this meddlesome typo? (Henry II, allegedly, about a priest.) It's {correct}.",    
    "Ask not what {correct} can do for you, {user}; ask what you can do for {correct}. Start with the capitals.",
    "Peace for our time? Not with that spelling, {user}. It's {correct}.",
    "Eureka! I have found it: {correct}.",

    // Stargate
    "Indeed. It is {correct}.",
    "Jaffa, kree! Spell it {correct}!",
    "Shol'va! You have betrayed the capitals, {user}. It's {correct}.",
    "\"Daniel, you're translating again.\" Sorry, Jack. The translation is {correct}.",
    "Kel no'reem, {user}: meditate on {correct}.",

    // James Bond
    "A spelling, shaken, not stirred: {correct}.",
    "Do you expect me to talk, {user}? No, I expect you to spell it {correct}.",
    "Pay attention, 007: it's {correct}.",
    "You only live twice, {user}. The second time, spell it {correct}.",
    "A licence to kill? No, just a licence to correct. It's {correct}.",
    "Ah, {user}, I've been expecting you. It's {correct}.",
    "Diamonds are forever, but typos need not be: it's spelt {correct}.",
    "Never say never again, {user}: {correct}."
  )

  private val Correct = "{correct}"
  private val User = "{user}"

  private val lock = new Object
  private var last = -1 // index of the previous pick, guarded by `lock`

  /** A uniformly random template (never the same one twice in a row), with placeholders filled in. */
  def pick(correct: String, mention: String, rng: Random = Random): String =
    val i = lock.synchronized {
      val n = templates.size
      val next =
        if last < 0 || n < 2 then rng.nextInt(n)
        else
          val j = rng.nextInt(n - 1) // uniform over every index except `last`
          if j >= last then j + 1 else j
      last = next
      next
    }
    templates(i).replace(Correct, correct).replace(User, mention)
