package docsoc.commands

import scala.concurrent.duration._
import scala.util.control.NonFatal

import docsoc.CommandSpec
import docsoc.ducks.DuckLookup
import docsoc.ducks.DuckReply
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

/** `/randomduck`: a random rubber duck photo from Commons. Discord glue only; everything about ducks lives in
  * `docsoc.ducks` (see `DuckLookup`).
  */
object RandomDuck:
  /** How long a user must wait between `/randomduck` uses. It protects the shared duck lookup and the channel from
    * spam. Per user and per command, in memory only.
    */
  val RandomDuckCooldown: FiniteDuration = 15.seconds

  val spec: CommandSpec =
    CommandSpec("randomduck", "Reply with a photo of a random rubber duck", Some(RandomDuckCooldown))
  val command: Command = Command(spec, randomDuck)

  private val log = org.slf4j.LoggerFactory.getLogger("docsoc.randomduck")

  /** Acknowledge at once (Discord wants an answer within 3 seconds and a fetch can take longer), then finish the
    * interaction with either an embed or the failure message.
    */
  private def randomDuck(event: SlashCommandInteractionEvent): Unit =
    event.deferReply().queue()
    DuckLookup.Default.random {
      case Right(reply) =>
        try
          event.getHook
            .sendMessageEmbeds(duckEmbed(reply))
            .setAllowedMentions(java.util.Collections.emptyList[Message.MentionType]())
            .queue(_ => (), e => log.warn("Could not send the duck embed", e))
        catch
          case NonFatal(e) =>
            log.error("Could not send the duck embed", e)
            sendFailure(event, DuckReply.failureMessage)
      case Left(message) => sendFailure(event, message)
    }

  private def duckEmbed(reply: DuckReply): MessageEmbed =
    EmbedBuilder()
      .setTitle(reply.title, reply.pageUrl)
      .setImage(reply.imageUrl)
      .setDescription(reply.description)
      .build()

  private def sendFailure(event: SlashCommandInteractionEvent, message: String): Unit =
    try
      event.getHook
        .sendMessage(message)
        .setAllowedMentions(java.util.Collections.emptyList[Message.MentionType]())
        .queue(_ => (), e => log.warn("Could not send the failure message", e))
    catch case NonFatal(e) => log.error("Could not send the failure message", e)
