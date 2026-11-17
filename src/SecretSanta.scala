package docsoc

import java.nio.file.{Files, Path, StandardCopyOption}
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.{Properties, UUID}
import scala.jdk.CollectionConverters.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/** Allocation and hint routing, independent of Discord. */
object SecretSanta:
  val MaxParticipants = 50
  private val Mention = "<@!?([1-9][0-9]{0,19})>".r

  def participants(text: String): Either[String, Vector[String]] =
    val ids = Mention.findAllMatchIn(text).map(_.group(1)).toVector
    if Mention.replaceAllIn(text, "").exists(c => !c.isWhitespace && c != ',') then
      Left("Use @mentions for every participant, separated by spaces or commas.")
    else if ids.size < 2 then Left("Mention at least two people.")
    else if ids.size > MaxParticipants then Left(s"A draw can have at most $MaxParticipants people.")
    else if ids.distinct.size != ids.size then Left("Each person must be mentioned exactly once.")
    else if ids.exists(id => scala.util.Try(java.lang.Long.parseUnsignedLong(id)).isFailure) then
      Left("One of the mentions has an invalid Discord user ID.")
    else Right(ids)

  /** Rejection sampling gives every permutation without self-assignments equal probability. */
  def allocate(ids: Vector[String], random: java.util.Random = new SecureRandom()): Map[String, String] =
    require(ids.size >= 2 && ids.distinct.size == ids.size)
    var recipients = ids.toArray
    def shuffle(): Unit =
      recipients = ids.toArray
      for i <- (recipients.length - 1) to 1 by -1 do
        val j = random.nextInt(i + 1)
        val tmp = recipients(i)
        recipients(i) = recipients(j)
        recipients(j) = tmp
    shuffle()
    while ids.indices.exists(i => ids(i) == recipients(i)) do shuffle()
    ids.zip(recipients).toMap

  case class Draw(
      id: String,
      guildId: String,
      organizerId: String,
      assignments: Map[String, String],
      delivered: Set[String] = Set.empty
  ):
    // A hint goes to the person BUYING FOR its author, not to their own recipient.
    def santaFor(authorId: String): Option[String] =
      assignments.collectFirst { case (santa, `authorId`) => santa }

/** One atomic, private file per draw. Hint text is never stored. Use a persistent directory. */
class SecretSantaStore(directory: Path):
  import SecretSanta.Draw

  private def path(id: String): Path = directory.resolve(s"${UUID.fromString(id)}.properties")

  def create(guildId: String, organizerId: String, participants: Vector[String]): Draw = synchronized {
    val draw = Draw(UUID.randomUUID().toString, guildId, organizerId, SecretSanta.allocate(participants))
    save(draw)
    draw
  }

  def load(id: String): Option[Draw] = synchronized {
    val file = path(id)
    if !Files.exists(file) then None
    else
      val props = new Properties()
      val in = Files.newInputStream(file)
      try props.load(in) finally in.close()
      val assignments = props.stringPropertyNames().asScala
        .filter(_.startsWith("assignment."))
        .map(key => key.stripPrefix("assignment.") -> props.getProperty(key)).toMap
      require(assignments.size >= 2 && assignments.keySet == assignments.values.toSet &&
        assignments.forall((santa, recipient) => santa != recipient), "Invalid Secret Santa draw")
      val delivered = props.getProperty("delivered", "").split(",").filter(_.nonEmpty).toSet
      require(props.getProperty("guild") != null && props.getProperty("organizer") != null &&
        delivered.subsetOf(assignments.keySet), "Invalid Secret Santa metadata")
      Some(Draw(UUID.fromString(id).toString, props.getProperty("guild"), props.getProperty("organizer"),
        assignments, delivered))
  }

  def markDelivered(id: String, userId: String): Unit = synchronized {
    val draw = load(id).getOrElse(throw new IllegalStateException("Draw missing"))
    require(draw.assignments.contains(userId))
    save(draw.copy(delivered = draw.delivered + userId))
  }

  private def save(draw: Draw): Unit =
    Files.createDirectories(directory)
    val posix = Files.getFileStore(directory).supportsFileAttributeView("posix")
    if posix then Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"))
    val tmp =
      if posix then Files.createTempFile(directory, "draw-", ".tmp",
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
      else Files.createTempFile(directory, "draw-", ".tmp")
    try
      val props = new Properties()
      props.setProperty("guild", draw.guildId)
      props.setProperty("organizer", draw.organizerId)
      props.setProperty("delivered", draw.delivered.toVector.sorted.mkString(","))
      draw.assignments.foreach((santa, recipient) => props.setProperty(s"assignment.$santa", recipient))
      val out = Files.newOutputStream(tmp)
      try props.store(out, "DoCBoT Secret Santa") finally out.close()
      Files.move(tmp, path(draw.id), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    finally Files.deleteIfExists(tmp)

/** Retries always reload delivery progress and never generate new assignments. */
class SecretSantaDelivery(store: SecretSantaStore)(using ExecutionContext):
  def deliver(id: String)(send: (SecretSanta.Draw, String, String) => Future[Unit]): Future[Vector[String]] =
    Future(store.load(id).getOrElse(throw new IllegalStateException("Draw missing"))).flatMap { draw =>
      Future.traverse(draw.assignments.toVector.filterNot((santa, _) => draw.delivered(santa))) {
        (santa, recipient) =>
          // Start inside a Future so synchronous send errors are handled like asynchronous failures.
          Future.unit.flatMap(_ => send(draw, santa, recipient)).map { _ =>
            store.markDelivered(draw.id, santa)
            Option.empty[String]
          }.recover { case NonFatal(_) => Some(santa) }
      }.map(_.flatten)
    }
