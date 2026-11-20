package docsoc

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.{Commands => DiscordCommands}
import net.dv8tion.jda.api.requests.GatewayIntent

/** Entry point. Run with: DISCORD_TOKEN=... scala-cli run .
  *
  * Slash commands are registered globally (they can take a while to appear). Set DISCORD_GUILD_ID to register them in
  * that one server instead, where they update instantly: handy while testing.
  */
@main def run(): Unit =
  val token = sys.env.getOrElse("DISCORD_TOKEN", sys.error("Set the DISCORD_TOKEN env var"))

  // Fail fast if the registered commands break Discord's rules.
  val problems = Commands.validate(Listener.commands.map(_.spec))
  require(problems.isEmpty, s"Invalid slash commands: ${problems.mkString("; ")}")

  val jda = JDABuilder
    .createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
    .addEventListeners(Listener)
    .build()

  val commandData = Listener.commands.map(c => DiscordCommands.slash(c.spec.name, c.spec.description))
  sys.env.get("DISCORD_GUILD_ID").filter(_.nonEmpty) match
    case Some(guildId) =>
      jda.awaitReady()
      val guild = Option(jda.getGuildById(guildId))
        .getOrElse(sys.error(s"DISCORD_GUILD_ID $guildId: the bot is not in that server"))
      guild.updateCommands().addCommands(commandData*).queue()
    case None =>
      jda.updateCommands().addCommands(commandData*).queue()

type Handler = SlashCommandInteractionEvent => Unit

/** A slash command: its Discord-facing spec (defined once in `Commands.scala`) and what it does. */
final case class Command(spec: CommandSpec, handler: Handler)

/** To add a message feature: write another handler and call it from onMessageReceived. To add a slash command: define
  * its spec in `Commands.scala`, then add a `Command(spec, handler)` to `commands` below.
  */
object Listener extends ListenerAdapter:

  /** Every slash command the bot offers. The single source of truth: Discord registration and dispatch are both
    * generated from this list, so they cannot drift apart.
    */
  val commands: Vector[Command] = Vector(
    Command(Commands.Ping, event => event.reply("Pong!").setEphemeral(true).queue())
  )

  private val handlers: Map[String, Handler] = commands.map(c => c.spec.name -> c.handler).toMap

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit =
    handlers.get(event.getName) match
      case Some(handler) => handler(event)
      case None          => event.reply("Unknown command").setEphemeral(true).queue()

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
