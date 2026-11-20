package docsoc

/** The slash commands the bot offers, as plain data.
  *
  * Design: this file knows nothing about Discord or JDA, so the registry and Discord's naming rules can be unit tested.
  * Each command's spec is defined once, as a named val here (e.g. [[Commands.Ping]]). The behaviour lives in
  * `Bot.scala`, where `Listener.commands` pairs each spec with its handler. That single list drives both the Discord
  * registration and the dispatch, so a name is never written twice. The bot refuses to start, and a test fails, if the
  * specs in that list break Discord's rules (see [[Commands.validate]]).
  *
  * To add a command: define its [[CommandSpec]] here, then add a `Command(spec, handler)` to `Listener.commands`.
  */
final case class CommandSpec(name: String, description: String)

object Commands:

  /** Discord's limit on global (application-wide) slash commands. */
  val MaxGlobalCommands = 100

  private val NamePattern = "^[\\w-]{1,32}$".r

  val Ping: CommandSpec = CommandSpec("ping", "Check that the bot is alive")

  /** Why a single spec breaks Discord's rules; empty if it is fine. */
  def problems(spec: CommandSpec): Vector[String] =
    Vector(
      Option.when(NamePattern.findFirstIn(spec.name).isEmpty)(
        s"name '${spec.name}' must be 1-32 characters of letters, digits, '_' or '-'"
      ),
      Option.when(spec.name != spec.name.toLowerCase)(s"name '${spec.name}' must be lowercase"),
      Option.when(spec.description.isEmpty || spec.description.length > 100)(
        s"description of '${spec.name}' must be 1-100 characters"
      )
    ).flatten

  /** Every problem in a list of specs, including duplicate names and exceeding the global command limit. */
  def validate(specs: Seq[CommandSpec]): Vector[String] =
    val duplicates = specs.groupBy(_.name).collect { case (name, group) if group.size > 1 => s"duplicate name '$name'" }
    val tooMany = Option.when(specs.size > MaxGlobalCommands)(
      s"${specs.size} commands exceeds Discord's limit of $MaxGlobalCommands global commands"
    )
    specs.toVector.flatMap(problems) ++ duplicates.toVector.sorted ++ tooMany
