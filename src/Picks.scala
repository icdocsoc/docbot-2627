package docsoc

import scala.util.Random

/** The one place that knows how to draw a random index without repeating the previous one. */
object Picks:

  /** A uniform random index in `0 until n`, never `avoid` (when `avoid` is a valid index and `n >= 2`).
    *
    * With nothing to avoid (`avoid` out of range, such as `-1`) or a pool of one, every index is allowed. Callers keep
    * the "previous index" themselves, under their own lock.
    */
  def indexAvoiding(n: Int, avoid: Int, rng: Random): Int =
    require(n > 0, "nothing to pick from")
    if avoid < 0 || avoid >= n || n < 2 then rng.nextInt(n)
    else
      val j = rng.nextInt(n - 1) // uniform over every index except `avoid`
      if j >= avoid then j + 1 else j
