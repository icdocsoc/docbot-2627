package docsoc.commands

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import scala.concurrent.duration._
import scala.util.control.NonFatal

import docsoc.CommandSpec
import docsoc.DuckReply
import docsoc.DuckSource
import docsoc.Wikimedia
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

/** `/randomduck`: a random rubber duck photo from Wikimedia Commons. The pure parts live outside this directory:
  * `Ducks`, `DuckSource`, `Wikimedia` and `DuckReply`.
  */
object RandomDuck:
  val spec: CommandSpec = CommandSpec("randomduck", "Reply with a photo of a random rubber duck")

  val command: Command = Command(spec, randomDuck)

  private val log = org.slf4j.LoggerFactory.getLogger("docsoc.randomduck")

  /** One shared duck list: fetched from Wikimedia Commons on the first use, then reused for 6 hours. */
  private val ducks = DuckSource(() => Wikimedia().fetch(), 6.hours)

  /** Where duck lookups run, so a slow Commons never blocks JDA's event thread. A few plain daemon threads rather than
    * virtual ones: `DuckSource` holds a lock (`synchronized`) while it fetches, and on JDK 21 blocking inside
    * `synchronized` pins a virtual thread's carrier. Waiting callers are bounded by the fetch timeouts anyway.
    */
  private val duckExecutor = Executors.newFixedThreadPool(
    4,
    new ThreadFactory:
      private val counter = java.util.concurrent.atomic.AtomicInteger(0)
      def newThread(r: Runnable): Thread =
        val t = Thread(r, s"randomduck-${counter.incrementAndGet()}")
        t.setDaemon(true)
        t
  )

  /** Acknowledge at once (Discord wants an answer within 3 seconds and a fetch can take longer), look the duck up on
    * another thread, and always finish the interaction with either an embed or the failure message.
    */
  private def randomDuck(event: SlashCommandInteractionEvent): Unit =
    event.deferReply().queue()
    try duckExecutor.execute(() => replyWithDuck(event))
    catch
      case NonFatal(e) =>
        log.error("Could not start the duck lookup", e)
        sendFailure(event)

  private def replyWithDuck(event: SlashCommandInteractionEvent): Unit =
    try
      ducks.next() match
        case Right(duck) =>
          val embed = duckEmbed(DuckReply.of(duck))
          event.getHook
            .sendMessageEmbeds(embed)
            .setAllowedMentions(java.util.Collections.emptyList[Message.MentionType]())
            .queue(_ => (), e => log.warn("Could not send the duck embed", e))
        case Left(problem) =>
          log.warn(s"No duck available: $problem")
          sendFailure(event)
    catch
      case NonFatal(e) =>
        log.error("Duck lookup failed", e)
        sendFailure(event)

  private def duckEmbed(reply: DuckReply): MessageEmbed =
    EmbedBuilder()
      .setTitle(reply.title, reply.pageUrl)
      .setImage(reply.imageUrl)
      .setDescription(reply.description)
      .build()

  private def sendFailure(event: SlashCommandInteractionEvent): Unit =
    try
      event.getHook
        .sendMessage(DuckReply.failureMessage)
        .setAllowedMentions(java.util.Collections.emptyList[Message.MentionType]())
        .queue(_ => (), e => log.warn("Could not send the failure message", e))
    catch case NonFatal(e) => log.error("Could not send the failure message", e)
