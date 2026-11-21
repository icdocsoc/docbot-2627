package docsoc

import java.time.Instant

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.MILLISECONDS

/** A per-key cooldown: after a key is let through, the same key is refused until `window` has passed.
  *
  * Design: no Discord types, the clock is injected, and every operation is serialised by one lock (each is O(1), so
  * contention is not a concern), so for any one key exactly one of several concurrent callers gets through. Keys are
  * plain strings; the bot uses one instance per command with the user id as the key, but a caller could just as well
  * share an instance and use keys like `command:userId`.
  *
  * A refused attempt is not recorded, so hammering a command never extends the wait.
  *
  * Memory policy: the cooldown is an in-memory map from key to the instant its cooldown ends, kept in the order those
  * instants occur (a key that is let through again moves to the back). On every call the expired entries at the front
  * are dropped, so memory follows the number of keys active within one window. As a hard bound, if the map still holds
  * `maxEntries` live keys when a new key arrives, the entry at the front (the one that expires first, so it is nearly
  * over anyway) is dropped to make room: in that extreme case that one key may get through slightly early, which beats
  * an unbounded map. The size therefore never exceeds `maxEntries`.
  *
  * @param window
  *   how long a key stays blocked after it was let through (at least 1 millisecond)
  * @param now
  *   the clock, injectable for tests
  * @param maxEntries
  *   the most keys remembered at once (at least 1)
  */
final class Cooldown(
    window: FiniteDuration,
    now: () => Instant = () => Instant.now(),
    maxEntries: Int = 10000
):
  require(window.toMillis >= 1, "window must be at least 1 millisecond")
  require(maxEntries >= 1, "maxEntries must be at least 1")

  private val lock = new Object
  // key -> instant the cooldown ends; insertion order is (approximately) expiry order. Guarded by `lock`.
  private val until = new java.util.LinkedHashMap[String, Instant]()

  /** Right if `key` may go ahead (and it is now recorded as having done so); Left with the time still to wait (always
    * positive) if it is cooling down, in which case nothing changes.
    */
  def tryAcquire(key: String): Either[FiniteDuration, Unit] = lock.synchronized {
    val t = now()
    purgeExpired(t)
    val existing = until.get(key)
    if existing != null then Left(FiniteDuration(java.time.Duration.between(t, existing).toMillis.max(1), MILLISECONDS))
    else
      if until.size >= maxEntries then until.remove(until.keySet.iterator.next())
      until.put(key, t.plusMillis(window.toMillis))
      Right(())
  }

  /** How many keys are currently remembered (live or not yet purged). For tests. */
  private[docsoc] def size: Int = lock.synchronized(until.size)

  /** Drops entries from the front while they have expired; stops at the first live one. Caller holds `lock`. */
  private def purgeExpired(t: Instant): Unit =
    val it = until.values.iterator
    var done = false
    while !done && it.hasNext do if !t.isBefore(it.next()) then it.remove() else done = true

object Cooldown:

  /** A wait as people say it, rounded up to whole seconds and never less than one: `1 second`, `15 seconds`. */
  def describe(wait: FiniteDuration): String =
    val seconds = ((wait.toMillis + 999) / 1000).max(1)
    if seconds == 1 then "1 second" else s"$seconds seconds"

  /** What the bot says when a command is used again too soon. */
  def tooSoonMessage(remaining: FiniteDuration): String =
    s"Easy there! The ducks need a moment. Try again in ${describe(remaining)}."
