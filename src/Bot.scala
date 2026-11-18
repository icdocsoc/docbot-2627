package docsoc

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent

/** Entry point. Run with: DISCORD_TOKEN=... scala-cli run . */
@main def run(): Unit =
  val token = sys.env.getOrElse("DISCORD_TOKEN", sys.error("Set the DISCORD_TOKEN env var"))

  JDABuilder
    .createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
    .addEventListeners(Listener)
    .build()

/** To add a feature: write another handler and call it from onMessageReceived. */
object Listener extends ListenerAdapter:
  override def onMessageReceived(event: MessageReceivedEvent): Unit =
    if !event.getAuthor.isBot then
      val msg = event.getMessage
      // One reply per message, even if it misspells several names.
      Correction.firstMisspelt(Corrections.all, msg.getContentRaw).foreach { c =>
        msg.reply(c.reply(event.getAuthor.getAsMention))
          .mentionRepliedUser(false)
          .queue()
      }
