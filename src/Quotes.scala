package docsoc

import scala.util.Random

/** A pool of snarky correction templates, shared by every "you spelt it wrong" feature.
  *
  * Placeholders:
  *   - `{correct}`: the correct spelling, already formatted by the caller (e.g. `**DoCSoc**`). Every template has it.
  *   - `{user}`: a mention of the offender (used by a minority of templates).
  */
object Quotes:

  val templates: Vector[String] = Vector(
    // Monty Python
    "'Tis but a typo! ...No wait, it's a full-on misspelling. It's {correct}.",
    "This spelling is no more. It has ceased to be. It's expired and gone to meet its maker. It's an ex-spelling. It's {correct}.",
    "We are the Knights Who Say {correct}! ...and we demand you spell it correctly, or we shall demand another shrubbery. It's {correct}.",
    "NOBODY expects the Spelling Inquisition! Our chief weapon is correction... correction and fear... It's {correct}. Our *two* weapons...",
    "\"Romanes eunt domus\"? No! {correct}. Now write it out a hundred times before sunrise, or I'll cut your tongue out. Hmm? Well? Off you go.",
    // Pop culture, across the eras
    "Et tu, {user}? Then fall, Caesar. It's {correct}.",
    "These aren't the capitals you're looking for. *waves hand* It's {correct}.",
    "I'm sorry, {user}. I'm afraid I can't let that spelling stand. It's {correct}.",
    "Frankly, my dear, I do give a damn. It's {correct}.",
    "YOU SHALL NOT PASS... with that spelling. It's {correct}.",
    // MCU
    "I am inevitable. And so is the correct spelling: {correct}.",
    "I can do this all day, {user}. It's {correct}.",
    "Avengers... assemble. Around the correct spelling: {correct}.",
    // Star Trek
    "Make it so, {user}. {correct}.",
    "Resistance is futile. You will be assimilated into spelling it {correct}.",
    "Dammit, Jim, I'm a doctor, not a spellchecker! It's {correct}.",
    // Brooklyn Nine-Nine
    "Cool cool cool cool cool. No doubt no doubt. Except it's spelt {correct}.",
    "Noice. Smort. ...Not you though, {user}. It's {correct}.",
    "Nine-Nine! Which is also how many typos I'll tolerate. Minus nine. It's {correct}.",

    // Musicals
    "Do you hear the people sing? Not when they spell it like that, {user}. It's {correct}.",
    "The Phantom of the Opera is there, inside your keyboard. Sing it right: {correct}.",
    "Because I knew you, I have been changed for good. And you will be changed for good by learning {correct}.",
    "I am not throwing away my shot, {user}. It's {correct}.",
    "Hakuna Matata means no worries, but that spelling does not. It's {correct}.",
    "It's the circle of life, and it moves us all towards {correct}.",
    "Grease is the word, and the word is {correct}.",
    "How do you solve a problem like {user}? Capitalise it properly: {correct}.",
    "Mamma mia, here I go again, correcting people. It's {correct}.",
    "Are you a Jellicle spelling? No. It's {correct}.",
    "All that jazz, and none of that spelling. It's {correct}.",
    "Five hundred twenty-five thousand six hundred minutes, and you still can't spell it? It's {correct}.",
    "Please, sir, I want some more... correct spelling. It's {correct}.",
    "A spoonful of sugar helps the medicine go down, {user}. The medicine is {correct}.",
    "Any dream will do, but only one spelling will: {correct}.",

    // Shakespeare
    "To spell, or not to spell: that is the question. The answer is {correct}.",
    "Something is rotten in the state of your spelling, {user}. It's {correct}.",
    "Double, double, toil and trouble; fix that spelling on the double. It's {correct}.",
    "Out, damned spot! Out, damned typo! It's {correct}.",
    "What's in a name, {user}? Quite a lot of capital letters, actually. It's {correct}.",
    "Friends, Romans, countrymen, lend me your ears: it's {correct}.",
    "Beware the Ides of March, and the wrong capitals. It's {correct}.",
    "Once more unto the breach, dear friends, once more, to correct this spelling: {correct}.",
    "A spelling! A spelling! My kingdom for a correct spelling! It's {correct}.",
    "Some are born great, some achieve greatness, and some have {correct} thrust upon them.",
    "Hell is empty and all the typos are here. It's {correct}.",
    "If you prick us, do we not bleed? If you misspell us, shall we not correct you? It's {correct}.",
    "O, beware, {user}, of the green-eyed monster; it is jealous of correct spelling. It's {correct}.",
    "Lord, what fools these mortals be! It's {correct}.",
    "All the world's a stage, and you are playing the wrong spelling. It's {correct}.",
    "Much ado about nothing? No, much ado about {correct}.",
    "Brevity is the soul of wit: {correct}.",

    // History and politics
    "Veni, vidi, vici. I came, I saw, I corrected: {correct}.",
    "Alea iacta est, {user}. The die is cast, and it spells {correct}.",
    "We shall fight on the beaches, we shall fight on the landing grounds, and we shall never surrender {correct}.",
    "I have nothing to offer but blood, toil, tears and sweat, {user}. And the spelling: {correct}.",
    "Never in the field of human conflict was so much owed by so many to so few capital letters. It's {correct}.",
    "England expects that every man will do his duty, {user}. Spelling {correct} included.",
    "\"The nearest run thing you ever saw in your life,\" said Wellington of Waterloo. Your spelling missed by more. It's {correct}.",
    "I have the heart and stomach of a king, said Elizabeth I at Tilbury, and I have the spelling of one too. It's {correct}.",
    "Will no one rid me of this meddlesome typo? (Henry II, allegedly, about a priest.) It's {correct}.",
    "\"Let them eat cake\" was almost certainly never said by Marie Antoinette. You, {user}, definitely wrote that spelling. It's {correct}.",
    "Eureka! I have found it: {correct}.",
    "Carthago delenda est! And so is your spelling. It's {correct}.",
    "\"Eppur si muove,\" Galileo supposedly muttered. Say what you like, it's still {correct}.",
    "From the sublime to the ridiculous there is but one step, {user}. It's {correct}.",
    "Four score and seven years ago, and still nobody can spell {correct}.",
    "Ask not what {correct} can do for you, {user}; ask what you can do for {correct}. Start with the capitals.",
    "I have a dream that one day everyone will spell it {correct}.",
    "That's one small step for man, one giant leap for correct spelling. It's {correct}.",
    "The lady's not for turning, and neither is this spelling. It's {correct}.",
    "O tempora, o mores! The times, the manners, the capitals! It's {correct}.",
    "Here I stand, I can do no other, said Luther (probably). Here I stand, and it's {correct}.",
    "To no one will we deny or delay right or justice, says Magna Carta. Nor the right spelling: {correct}.",
    "Peace for our time? Not with that spelling, {user}. It's {correct}.",

    // Stargate
    "Indeed. It is {correct}.",
    "Jaffa, kree! Spell it {correct}!",
    "Shol'va! You have betrayed the capitals, {user}. It's {correct}.",
    "Chevron seven, locked. Spelling: {correct}.",
    "Oh, c'mon. It's {correct}.",
    "\"Daniel, you're translating again.\" Sorry, Jack. The translation is {correct}.",
    "The Goa'uld would have you executed for this. I'll just say it's {correct}.",
    "Kel no'reem, {user}: meditate on {correct}.",
    "The Replicators are less relentless than I am about this. It's {correct}.",
    "The Ancients ascended to avoid this spelling. It's {correct}.",
    "Rodney McKay would already have fixed it, and told you about it at length. It's {correct}.",
    "Even Destiny, an ancient ship at the edge of the universe, spells it {correct}.",

    // James Bond
    "The name's Bond. James Bond. And the society's {correct}.",
    "A spelling, shaken, not stirred: {correct}.",
    "Do you expect me to talk, {user}? No, I expect you to spell it {correct}.",
    "Pay attention, 007: it's {correct}.",
    "You only live twice, {user}. The second time, spell it {correct}.",
    "Nobody does it better than {correct}.",
    "A licence to kill? No, just a licence to correct. It's {correct}.",
    "Ah, {user}, I've been expecting you. It's {correct}.",
    "Why do they always play the same record? Why do people always spell it wrong? It's {correct}.",
    "Diamonds are forever, but typos need not be: {correct}.",
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
