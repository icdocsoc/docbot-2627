package docsoc.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/** Hands every slash command interaction to the [[CommandRegistry]]. */
object SlashCommandListener extends ListenerAdapter:
  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit =
    CommandRegistry.dispatch(event)
