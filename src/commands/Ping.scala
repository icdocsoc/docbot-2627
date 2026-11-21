package docsoc.commands

import docsoc.CommandSpec

object Ping:
  val spec: CommandSpec = CommandSpec("ping", "Check that the bot is alive")

  val command: Command = Command(spec, event => event.reply("Pong!").setEphemeral(true).queue())
