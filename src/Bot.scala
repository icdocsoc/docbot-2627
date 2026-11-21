package docsoc

import docsoc.commands.CommandRegistry
import docsoc.commands.SlashCommandListener
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent

/** Entry point. Run with: DISCORD_TOKEN=... scala-cli run .
  *
  * Slash commands are registered globally (they can take a while to appear). Set DISCORD_GUILD_ID to register them in
  * that one server instead, where they update instantly: handy while testing.
  */
@main def run(): Unit =
  val token = sys.env.getOrElse("DISCORD_TOKEN", sys.error("Set the DISCORD_TOKEN env var"))

  CommandRegistry.validate() // fail fast, before connecting

  val jda = JDABuilder
    .createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
    .addEventListeners(Listener, SlashCommandListener)
    .build()

  CommandRegistry.register(jda, sys.env.get("DISCORD_GUILD_ID").filter(_.nonEmpty))

/** The spelling police. To add a message feature: write another handler and call it from onMessageReceived. Slash
  * commands live in `src/commands` (see `CommandRegistry`).
  */
object Listener extends ListenerAdapter:

  override def onMessageReceived(event: MessageReceivedEvent): Unit =
    if !event.getAuthor.isBot then
      val msg = event.getMessage
      // One reply per message, even if it misspells several names.
      Correction.firstMisspelt(Corrections.all, msg.getContentRaw).foreach { c =>
        msg
          .reply(c.reply(event.getAuthor.getAsMention))
          .mentionRepliedUser(false)
          .queue()
      }
