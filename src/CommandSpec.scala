package docsoc

import scala.concurrent.duration._

/** A slash command as plain data: its name and description.
  *
  * Design: this file knows nothing about Discord or JDA, so Discord's naming rules can be unit tested. Each command
  * defines its spec once, next to its handler, in `src/commands/` (e.g. `commands/Ping.scala`); the registry there
  * collects them and checks them with [[CommandSpec.validate]]. The bot refuses to start, and a test fails, if the
  * registered specs break Discord's rules.
  *
  * @param cooldown
  *   if set, each user may use the command at most once per this long (applied by the registry before the handler runs;
  *   must be positive)
  */
final case class CommandSpec(name: String, description: String, cooldown: Option[FiniteDuration] = None)

object CommandSpec:

  /** Discord's limit on global (application-wide) slash commands. */
  val MaxGlobalCommands = 100

  private val NamePattern = "^[\\w-]{1,32}$".r

  /** Why a single spec breaks Discord's rules; empty if it is fine. */
  def problems(spec: CommandSpec): Vector[String] =
    Vector(
      Option.when(NamePattern.findFirstIn(spec.name).isEmpty)(
        s"name '${spec.name}' must be 1-32 characters of letters, digits, '_' or '-'"
      ),
      Option.when(spec.name != spec.name.toLowerCase)(s"name '${spec.name}' must be lowercase"),
      Option.when(spec.description.isEmpty || spec.description.length > 100)(
        s"description of '${spec.name}' must be 1-100 characters"
      ),
      Option.when(spec.cooldown.exists(_.toMillis < 1))(s"cooldown of '${spec.name}' must be at least 1 millisecond")
    ).flatten

  /** Every problem in a list of specs, including duplicate names and exceeding the global command limit. */
  def validate(specs: Seq[CommandSpec]): Vector[String] =
    val duplicates = specs.groupBy(_.name).collect { case (name, group) if group.size > 1 => s"duplicate name '$name'" }
    val tooMany = Option.when(specs.size > MaxGlobalCommands)(
      s"${specs.size} commands exceeds Discord's limit of $MaxGlobalCommands global commands"
    )
    specs.toVector.flatMap(problems) ++ duplicates.toVector.sorted ++ tooMany
