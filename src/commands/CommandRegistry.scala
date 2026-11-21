package docsoc.commands

import docsoc.CommandSpec
import docsoc.Cooldown
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.{Commands => DiscordCommands}

/** Discord glue for slash commands (everything under `src/commands` is excluded from coverage, like `Bot.scala`).
  *
  * To add a command: write `commands/YourCommand.scala` with its spec and handler (see `Ping.scala`), then add its
  * `command` to [[all]]. Registration and dispatch are both generated from that list, so they cannot drift apart and a
  * name is written in one place only.
  */
object CommandRegistry:

  /** Every slash command the bot offers: the single source of truth. */
  val all: Vector[Command] = Vector(
    Ping.command,
    RandomDuck.command
  )

  private val handlers: Map[String, Handler] = all.map(c => c.spec.name -> c.handler).toMap

  /** One cooldown per command that declares one in its spec, keyed by user id. */
  private val cooldowns: Map[String, Cooldown] =
    all.flatMap(c => c.spec.cooldown.map(window => c.spec.name -> Cooldown(window))).toMap

  /** Runs the handler of the command named in the event; unknown names get an ephemeral reply. */
  def dispatch(event: SlashCommandInteractionEvent): Unit =
    handlers.get(event.getName) match
      case Some(handler) =>
        // Checked before the handler, so a command that is cooling down never defers or touches the network.
        cooldowns.get(event.getName).map(_.tryAcquire(event.getUser.getId)) match
          case Some(Left(remaining)) =>
            event.reply(Cooldown.tooSoonMessage(remaining)).setEphemeral(true).queue()
          case _ => handler(event)
      case None => event.reply("Unknown command").setEphemeral(true).queue()

  /** Why the registered specs break Discord's rules; empty if they are fine. */
  def problems: Vector[String] = CommandSpec.validate(all.map(_.spec))

  /** Fail fast at startup if the registered commands break Discord's rules. */
  def validate(): Unit =
    require(problems.isEmpty, s"Invalid slash commands: ${problems.mkString("; ")}")

  /** Registers every command with Discord: globally (slow to appear), or in one guild (instant) if given. */
  def register(jda: JDA, guildId: Option[String]): Unit =
    val commandData = all.map(c => DiscordCommands.slash(c.spec.name, c.spec.description))
    guildId match
      case Some(id) =>
        jda.awaitReady()
        val guild = Option(jda.getGuildById(id))
          .getOrElse(sys.error(s"DISCORD_GUILD_ID $id: the bot is not in that server"))
        guild.updateCommands().addCommands(commandData*).queue()
      case None =>
        jda.updateCommands().addCommands(commandData*).queue()
