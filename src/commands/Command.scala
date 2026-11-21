package docsoc.commands

import docsoc.CommandSpec
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

type Handler = SlashCommandInteractionEvent => Unit

/** A slash command: its Discord-facing spec and what it does. Each command lives in its own file in this directory
  * (spec and handler together) and is listed once in [[CommandRegistry.all]].
  */
final case class Command(spec: CommandSpec, handler: Handler)
