package docsoc

class CommandsTests extends munit.FunSuite:
  private def spec(name: String, description: String = "A command") = CommandSpec(name, description)

  private val registered = Listener.commands.map(_.spec)

  test("the registered commands follow Discord's rules") {
    assertEquals(Commands.validate(registered), Vector.empty[String])
  }
  test("the registered commands include /ping") {
    assert(registered.contains(Commands.Ping))
  }
  test("registered command names are unique, so each has exactly one handler") {
    assertEquals(registered.map(_.name).distinct, registered.map(_.name))
  }

  test("accepts valid names") {
    for name <- Seq("a", "ping", "random-duck", "snake_case", "x1", "a" * 32) do
      assertEquals(Commands.problems(spec(name)), Vector.empty[String], name)
  }
  test("rejects invalid names") {
    for name <- Seq("", "a" * 33, "has space", "Upper", "dot.dot", "slash/") do
      assert(Commands.problems(spec(name)).nonEmpty, name)
  }
  test("rejects uppercase names with a clear reason") {
    assert(Commands.problems(spec("Ping")).exists(_.contains("lowercase")))
  }
  test("description must be 1-100 characters") {
    assert(Commands.problems(spec("ok", "")).nonEmpty)
    assert(Commands.problems(spec("ok", "d" * 101)).nonEmpty)
    assertEquals(Commands.problems(spec("ok", "d")), Vector.empty[String])
    assertEquals(Commands.problems(spec("ok", "d" * 100)), Vector.empty[String])
  }
  test("rejects duplicate names") {
    assertEquals(Commands.validate(Seq(spec("a"), spec("b"), spec("a"))), Vector("duplicate name 'a'"))
  }
  test("rejects more than 100 commands, accepts exactly 100") {
    def n(count: Int) = (1 to count).map(i => spec(s"cmd$i"))
    assertEquals(Commands.validate(n(Commands.MaxGlobalCommands)), Vector.empty[String])
    assertEquals(Commands.validate(n(Commands.MaxGlobalCommands + 1)).size, 1)
  }
