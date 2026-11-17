package docsoc

import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class SecretSantaTests extends munit.FunSuite:
  private given ExecutionContext = ExecutionContext.global

  private def withStore[A](run: (SecretSantaStore, Path) => A): A =
    val directory = Files.createTempDirectory("docbot-santa-test-")
    try run(new SecretSantaStore(directory), directory)
    finally
      val paths = Files.walk(directory)
      try paths.iterator().asScala.toVector.reverse.foreach(Files.delete)
      finally paths.close()

  private def withAsyncStore(run: SecretSantaStore => Future[Unit]): Future[Unit] =
    val directory = Files.createTempDirectory("docbot-santa-test-")
    run(new SecretSantaStore(directory)).andThen { case _ =>
      val paths = Files.walk(directory)
      try paths.iterator().asScala.toVector.reverse.foreach(Files.delete)
      finally paths.close()
    }

  test("parses mentions, including nickname syntax, commas and newlines") {
    assertEquals(SecretSanta.participants(" <@123>, <@!456>\n<@789> "), Right(Vector("123", "456", "789")))
  }

  test("rejects missing people, duplicates, roles, plain names and invalid IDs") {
    for input <- Vector("", "<@123>", "<@123> <@!123>", "@Alice @Bob", "<@123> <@&456>",
        "<@123> <@456> extra", "<@0> <@456>", "<@0123> <@456>", "<@99999999999999999999> <@456>") do
      assert(SecretSanta.participants(input).isLeft, input)
    val tooMany = (1 to SecretSanta.MaxParticipants + 1).map(i => s"<@$i>").mkString(" ")
    assert(SecretSanta.participants(tooMany).isLeft)
  }

  test("allocations cover everyone exactly once and never assign someone to themselves") {
    val random = new java.util.Random(12345)
    for size <- 2 to SecretSanta.MaxParticipants; attempt <- 1 to 20 do
      val ids = (1 to size).map(_.toString).toVector
      val assignment = SecretSanta.allocate(ids, random)
      assertEquals(assignment.keySet, ids.toSet)
      assertEquals(assignment.values.toSet, ids.toSet)
      assert(assignment.forall((santa, recipient) => santa != recipient))
  }

  test("two people are paired with each other; invalid inputs cannot be allocated") {
    assertEquals(SecretSanta.allocate(Vector("1", "2")), Map("1" -> "2", "2" -> "1"))
    intercept[IllegalArgumentException](SecretSanta.allocate(Vector("1")))
    intercept[IllegalArgumentException](SecretSanta.allocate(Vector("1", "1")))
  }

  test("random allocations vary and can produce all nine derangements of four people") {
    val random = new java.util.Random(42)
    val draws = (1 to 500).map(_ => SecretSanta.allocate(Vector("1", "2", "3", "4"), random)).toSet
    assertEquals(draws.size, 9)
  }

  test("hints route to the author's Santa, and outsiders have no route") {
    val draw = SecretSanta.Draw("draw", "guild", "organizer", Map("1" -> "2", "2" -> "3", "3" -> "1"))
    assertEquals(draw.santaFor("1"), Some("3"))
    assertEquals(draw.santaFor("2"), Some("1"))
    assertEquals(draw.santaFor("3"), Some("2"))
    assertEquals(draw.santaFor("outsider"), None)
  }

  test("draws and delivery progress survive a new store instance without changing assignments") {
    withStore { (store, directory) =>
      val draw = store.create("guild", "organizer", Vector("1", "2", "3"))
      store.markDelivered(draw.id, "1")
      store.markDelivered(draw.id, "2")
      store.markDelivered(draw.id, "1")
      val restored = new SecretSantaStore(directory).load(draw.id).get
      assertEquals(restored, draw.copy(delivered = Set("1", "2")))
      assertEquals(store.load(draw.id.toUpperCase), Some(restored))
      assertEquals(restored.santaFor("1"), draw.santaFor("1"))
      assertEquals(store.load(java.util.UUID.randomUUID().toString), None)
      intercept[IllegalArgumentException](store.load("../outside"))
      if Files.getFileStore(directory).supportsFileAttributeView("posix") then
        assertEquals(Files.getPosixFilePermissions(directory), PosixFilePermissions.fromString("rwx------"))
        assertEquals(Files.getPosixFilePermissions(directory.resolve(s"${draw.id}.properties")),
          PosixFilePermissions.fromString("rw-------"))
    }
  }

  test("overlapping draws remain independent") {
    withStore { (store, _) =>
      val first = store.create("guild", "organizer", Vector("1", "2"))
      val second = store.create("guild", "organizer", Vector("1", "3"))
      assertNotEquals(first.id, second.id)
      assertEquals(store.load(first.id).get.santaFor("1"), Some("2"))
      assertEquals(store.load(second.id).get.santaFor("1"), Some("3"))
    }
  }

  test("corrupt draws are rejected instead of routing hints incorrectly") {
    withStore { (store, directory) =>
      val id = java.util.UUID.randomUUID().toString
      val file = directory.resolve(s"$id.properties")
      Files.writeString(file, "guild=guild\norganizer=organizer\nassignment.1=1\nassignment.2=2\n")
      intercept[IllegalArgumentException](store.load(id))
      Files.writeString(file, "guild=guild\norganizer=organizer\nassignment.1=2\nassignment.2=1\ndelivered=outsider\n")
      intercept[IllegalArgumentException](store.load(id))
    }
  }

  test("failed DMs don't cancel other deliveries; retries use only the original undelivered assignment") {
    withAsyncStore { store =>
      val draw = store.create("guild", "organizer", Vector("1", "2", "3"))
      val delivery = new SecretSantaDelivery(store)
      delivery.deliver(draw.id) { (_, santa, recipient) =>
        assertEquals(recipient, draw.assignments(santa))
        if santa == "2" then Future.failed(new RuntimeException("DMs closed")) else Future.unit
      }.flatMap { failed =>
        assertEquals(failed, Vector("2"))
        assertEquals(store.load(draw.id).get.delivered, Set("1", "3"))
        delivery.deliver(draw.id) { (restored, santa, recipient) =>
          assertEquals(restored.assignments, draw.assignments)
          assertEquals(santa, "2")
          assertEquals(recipient, draw.assignments("2"))
          Future.unit
        }
      }.flatMap { failed =>
        assertEquals(failed, Vector.empty)
        assertEquals(store.load(draw.id).get.delivered, Set("1", "2", "3"))
        delivery.deliver(draw.id)((_, _, _) => fail("No assignment should be resent"))
      }.map(failed => assertEquals(failed, Vector.empty))
    }
  }

  test("synchronous sending errors are retryable too") {
    withAsyncStore { store =>
      val draw = store.create("guild", "organizer", Vector("1", "2"))
      new SecretSantaDelivery(store).deliver(draw.id)((_, _, _) => throw new RuntimeException("send failed"))
        .map { failed =>
          assertEquals(failed.toSet, Set("1", "2"))
          assertEquals(store.load(draw.id).get.delivered, Set.empty)
        }
    }
  }
