package docsoc

import java.util.Collections
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.util.control.NonFatal

class SecretSantaListener(store: SecretSantaStore) extends ListenerAdapter:
  import SecretSanta.Draw
  private given ExecutionContext = ExecutionContext.global
  private val Hint = "santa-hint:"
  private val Retry = "santa-retry:"
  private val SubmitHint = "santa-submit:"
  private val sending = java.util.concurrent.ConcurrentHashMap.newKeySet[String]()
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[SecretSantaListener])
  private val delivery = new SecretSantaDelivery(store)

  override def onReady(event: ReadyEvent): Unit =
    event.getJDA.upsertCommand(
      Commands.slash("secretsanta", "Draw Secret Santa names and DM each person their recipient")
        .setGuildOnly(true)
        .addOption(OptionType.STRING, "participants", "@mention every person taking part (2–50 people)", true)
    ).queue(_ => (), _ => log.error("Could not register /secretsanta"))
    event.getJDA.upsertCommand(
      Commands.slash("secretsanta-resend", "Retry a saved draw's undelivered assignments without drawing again")
        .setGuildOnly(true)
        .addOption(OptionType.STRING, "draw", "Draw ID from the original setup response", true)
    ).queue(_ => (), _ => log.error("Could not register /secretsanta-resend"))

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit =
    if event.getName == "secretsanta" then
      if !event.isFromGuild then event.reply("Start a Secret Santa in your server.").setEphemeral(true).queue()
      else
        SecretSanta.participants(event.getOption("participants").getAsString) match
          case Left(error) => event.reply(error).setEphemeral(true).queue()
          case Right(ids) =>
            event.deferReply(true).queue { hook =>
              val members = Future.traverse(ids) { id =>
                event.getGuild.retrieveMemberById(id).submit().asScala
              }
              members.flatMap { people =>
                if people.exists(_.getUser.isBot) then
                  hook.editOriginal("Only people can take part; please remove bot accounts.").submit().asScala.map(_ => ())
                else
                  Future(store.create(event.getGuild.getId, event.getUser.getId, ids))
                    .flatMap(draw => deliver(event.getJDA, hook, draw))
              }.recover { case NonFatal(_) =>
                log.warn("Could not set up a Secret Santa")
                hook.editOriginal("Could not set up the draw. Check that every mention is a current server member and try again. If this continues, ask the bot administrator to check storage.").queue()
              }
            }
    else if event.getName == "secretsanta-resend" then
      withDraw(event.getOption("draw").getAsString.trim) { draw =>
        if !event.isFromGuild || event.getGuild.getId != draw.guildId || event.getUser.getId != draw.organizerId then
          event.reply("Only the organizer can resend assignments, from the original server.").setEphemeral(true).queue()
        else event.deferReply(true).queue(hook => { deliver(event.getJDA, hook, draw); () })
      }(message => event.reply(message).setEphemeral(true).queue())

  override def onButtonInteraction(event: ButtonInteractionEvent): Unit =
    val id = event.getComponentId
    if id.startsWith(Hint) then
      withDraw(id.stripPrefix(Hint)) { draw =>
        if !draw.assignments.contains(event.getUser.getId) then
          event.reply("You aren't a participant in this draw.").setEphemeral(true).queue()
        else
          val input = TextInput.create("hint", "What would you like your Santa to know?", TextInputStyle.PARAGRAPH)
            .setRequired(true).setMinLength(1).setMaxLength(1000).build()
          event.replyModal(Modal.create(SubmitHint + draw.id, "Send a hint to your Santa")
            .addActionRow(input).build()).queue()
      }(message => event.reply(message).setEphemeral(true).queue())
    else if id.startsWith(Retry) then
      withDraw(id.stripPrefix(Retry)) { draw =>
        if event.getUser.getId != draw.organizerId then
          event.reply("Only the organizer can resend assignments.").setEphemeral(true).queue()
        else event.deferReply(true).queue(hook => { deliver(event.getJDA, hook, draw); () })
      }(message => event.reply(message).setEphemeral(true).queue())

  override def onModalInteraction(event: ModalInteractionEvent): Unit =
    if event.getModalId.startsWith(SubmitHint) then
      withDraw(event.getModalId.stripPrefix(SubmitHint)) { draw =>
        draw.santaFor(event.getUser.getId) match
          case None => event.reply("You aren't a participant in this draw.").setEphemeral(true).queue()
          case Some(santa) =>
            val hint = event.getValue("hint").getAsString.trim
            if hint.isEmpty || hint.length > 1000 then
              event.reply("Please enter a hint between 1 and 1000 characters.").setEphemeral(true).queue()
            else event.deferReply(true).queue { hook =>
              sendHint(event.getJDA, draw, event.getUser.getId, santa, hint)
                .foreach { delivered =>
                  hook.editOriginal(if delivered then "Your hint was sent to your Santa!"
                    else "Your hint couldn't be delivered. Your Santa may need to enable DMs from server members. Please try again later.").queue()
                }
            }
      }(message => event.reply(message).setEphemeral(true).queue())

  private def withDraw(id: String)(use: Draw => Unit)(error: String => Unit): Unit =
    try
      store.load(id) match
        case Some(draw) => use(draw)
        case None => error("This Secret Santa draw is no longer available.")
    catch case NonFatal(_) => error("Couldn't load this draw. Please ask the bot administrator to check storage.")

  private def deliver(jda: JDA, hook: InteractionHook, draw: Draw): Future[Unit] =
    if !sending.add(draw.id) then
      hook.editOriginal("Assignments for this draw are already being sent. Please wait.").submit().asScala.map(_ => ())
    else
      // Capture each failure separately so a closed DM never cancels delivery to everyone else.
      val work = delivery.deliver(draw.id) { (_, santa, recipient) =>
        jda.retrieveUserById(santa).submit().asScala.flatMap { user =>
          user.openPrivateChannel().submit().asScala.flatMap { channel =>
            channel.sendMessage(
              s"🎅 Secret Santa! You're buying for <@$recipient>.\n" +
                s"Draw: `${draw.id}`\n" +
                "Use the button below to send a private hint to the person buying for YOU. " +
                "They can read your hint, but this bot won't reveal who they are or let them reply."
            ).setAllowedMentions(Collections.emptyList())
              .setActionRow(Button.primary(Hint + draw.id, "Send a hint to my Santa"))
              .submit().asScala.map(_ => ())
          }
        }
      }.flatMap { failed =>
        val message =
          if failed.isEmpty then s"🎅 Secret Santa set up for ${draw.assignments.size} people! Everyone has their recipient in DMs.\nDraw: `${draw.id}`"
          else s"The draw is saved, but I couldn't DM: ${failed.map(id => s"<@$id>").mkString(", ")}. " +
            s"Ask them to enable DMs from server members, then retry below. Assignments will stay the same.\nDraw: `${draw.id}`"
        val edit = hook.editOriginal(message).setAllowedMentions(Collections.emptyList())
        if failed.nonEmpty then edit.setActionRow(Button.secondary(Retry + draw.id, "Retry undelivered DMs"))
        edit.submit().asScala.map(_ => ())
      }.recover { case NonFatal(_) =>
        log.warn("Could not finish Secret Santa delivery")
        hook.editOriginal(s"Draw `${draw.id}` is saved. Delivery couldn't finish; retry below.")
          .setActionRow(Button.secondary(Retry + draw.id, "Retry undelivered DMs")).queue()
      }
      work.andThen { case _ => sending.remove(draw.id) }

  private def sendHint(jda: JDA, draw: Draw, author: String, santa: String, hint: String): Future[Boolean] =
    jda.retrieveUserById(santa).submit().asScala.flatMap { user =>
      user.openPrivateChannel().submit().asScala.flatMap { channel =>
        channel.sendMessage(s"🎁 A hint from your Secret Santa recipient <@$author>:\n$hint\n\nDraw: `${draw.id}`")
          .setAllowedMentions(Collections.emptyList()).submit().asScala
      }
    }.map(_ => true).recover { case NonFatal(_) => false }

object SecretSantaListener:
  def fromEnvironment(): SecretSantaListener =
    new SecretSantaListener(new SecretSantaStore(java.nio.file.Path.of(
      sys.env.getOrElse("SECRETSANTA_DATA_DIR", "data/secretsanta"))))
