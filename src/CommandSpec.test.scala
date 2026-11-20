package docsoc

class CommandSpecTests extends munit.FunSuite:
  private def spec(name: String, description: String = "A command") = CommandSpec(name, description)

  test("accepts valid names") {
    for name <- Seq("a", "ping", "random-duck", "snake_case", "x1", "a" * 32) do
      assertEquals(CommandSpec.problems(spec(name)), Vector.empty[String], name)
  }
  test("rejects invalid names") {
    for name <- Seq("", "a" * 33, "has space", "Upper", "dot.dot", "slash/") do
      assert(CommandSpec.problems(spec(name)).nonEmpty, name)
  }
  test("rejects uppercase names with a clear reason") {
    assert(CommandSpec.problems(spec("Ping")).exists(_.contains("lowercase")))
  }
  test("description must be 1-100 characters") {
    assert(CommandSpec.problems(spec("ok", "")).nonEmpty)
    assert(CommandSpec.problems(spec("ok", "d" * 101)).nonEmpty)
    assertEquals(CommandSpec.problems(spec("ok", "d")), Vector.empty[String])
    assertEquals(CommandSpec.problems(spec("ok", "d" * 100)), Vector.empty[String])
  }
  test("rejects duplicate names") {
    assertEquals(CommandSpec.validate(Seq(spec("a"), spec("b"), spec("a"))), Vector("duplicate name 'a'"))
  }
  test("rejects more than 100 commands, accepts exactly 100") {
    def n(count: Int) = (1 to count).map(i => spec(s"cmd$i"))
    assertEquals(CommandSpec.validate(n(CommandSpec.MaxGlobalCommands)), Vector.empty[String])
    assertEquals(CommandSpec.validate(n(CommandSpec.MaxGlobalCommands + 1)).size, 1)
  }

  test("cooldown is optional and must be positive") {
    import scala.concurrent.duration._
    assertEquals(spec("a").cooldown, None)
    assertEquals(CommandSpec.problems(CommandSpec("a", "d", Some(1.second))), Vector.empty[String])
    assert(CommandSpec.problems(CommandSpec("a", "d", Some(Duration.Zero))).exists(_.contains("cooldown")))
    assert(CommandSpec.problems(CommandSpec("a", "d", Some((-5).seconds))).nonEmpty)
    assert(CommandSpec.validate(Seq(CommandSpec("a", "d", Some(0.seconds)))).nonEmpty)
  }
