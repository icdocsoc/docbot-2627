package docsoc.commands

import docsoc.CommandSpec

class CommandRegistryTests extends munit.FunSuite:
  private val registered = CommandRegistry.all.map(_.spec)

  test("the registered commands follow Discord's rules") {
    assertEquals(CommandSpec.validate(registered), Vector.empty[String])
    assertEquals(CommandRegistry.problems, Vector.empty[String])
  }
  test("the registered commands include /ping") {
    assert(registered.contains(Ping.spec))
  }
  test("the registered commands include /randomduck") {
    assert(registered.contains(RandomDuck.spec))
  }
  test("registered command names are unique, so each has exactly one handler") {
    assertEquals(registered.map(_.name).distinct, registered.map(_.name))
  }
