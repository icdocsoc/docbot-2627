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
  test("/randomduck has the 15 second cooldown and /ping has none") {
    import scala.concurrent.duration._
    val byName = registered.map(c => c.name -> c.cooldown).toMap
    assertEquals(byName("randomduck"), Some(15.seconds))
    assertEquals(byName("randomduck"), Some(RandomDuck.RandomDuckCooldown))
    assertEquals(byName("ping"), None)
  }
