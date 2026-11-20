package docsoc

/** The slash commands the bot offers, as plain data.
  *
  * Design: this file knows nothing about Discord or JDA, so the registry and Discord's naming rules can be unit tested.
  * The behaviour of each command lives in `Bot.scala`, in a handler map keyed by [[CommandSpec.name]]. A test checks
  * that the two stay in sync, and the bot refuses to start if they are not.
  *
  * To add a command: add a [[CommandSpec]] to [[Commands.all]] and a handler with the same name in `Bot.scala`.
  */
final case class CommandSpec(name: String, description: String)

object Commands:

  /** Discord's limit on global (application-wide) slash commands. */
  val MaxGlobalCommands = 100

  private val NamePattern = "^[\\w-]{1,32}$".r

  /** Every command the bot registers. */
  val all: Vector[CommandSpec] = Vector(
    CommandSpec("ping", "Check that the bot is alive")
  )

  val names: Set[String] = all.map(_.name).toSet

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
