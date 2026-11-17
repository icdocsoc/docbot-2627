# DoCBoT

The Discord bot for [DoCSoc](https://github.com/icdocsoc), Imperial College's Department of Computing Society. (Yes, the name is spelt that way on purpose. Please don't tell it.)

DoCBoT is meant to grow: this is the home for whatever small, useful, or silly things we want our Discord server to do.

## What it does today

### Spelling police
If anyone writes "DoCSoc" with the wrong capitalisation (`docsoc`, `DoCSoC`, `DOCSOC`, ...), DoCBoT replies to their message with a snarky correction. It rotates through a pool of replies (Monty Python, MCU, Star Trek, Brooklyn Nine-Nine, and assorted pop culture) so it doesn't repeat itself.

Known limits: it only checks capitalisation (not typos like "docsock"), URLs are ignored, but it still matches inside longer words.

Invisible/zero-width characters (e.g. `doc\u200Bsoc`) are stripped before checking, so they can't be used to dodge the police.

### Secret Santa

Run `/secretsanta` in the server and put everyone's @mentions in the `participants` field:

```text
/secretsanta participants: @Alice @Bob @Charlie
```

- Include 2–50 distinct people from the server. The organizer only takes part if explicitly mentioned. Bots cannot participate.
- Everyone gets exactly one recipient, nobody gets themselves, and everyone has exactly one Santa. Allocations are random.
- The bot DMs each person their recipient and a **Send a hint to my Santa** button. Click it to write a private hint (up to 1000 characters) to the person buying for you. They receive it in a DM; there is no reply function and the bot doesn't reveal your Santa's identity.
- Setup results and hint confirmations are private. If some DMs fail, the organizer gets a retry button. Ask those people to enable DMs from server members, then retry; successful deliveries and the original assignments are preserved.
- To retry later, even after restarting Discord, use `/secretsanta-resend draw: <draw-id>` in the original server. Only the organizer can do this. Save the draw ID from the setup response. Running `/secretsanta` again creates a separate draw.
- Hint buttons refer to a specific draw, so people can take part in multiple exchanges.

Draws and delivery progress are saved under `data/secretsanta/` by default; set `SECRETSANTA_DATA_DIR` to use another directory. Keep that directory on persistent storage across restarts and deployments, and run one bot instance. Assignment files contain the secret pairings and should be kept private; hint text is not stored. Deleting a draw's file disables its hint buttons and retries. If the process stops between sending a DM and recording delivery, a retry may repeat that same assignment DM.

## Running it

Requires [scala-cli](https://scala-cli.virtuslab.org/) and a JDK.

1. Create an application at https://discord.com/developers/applications and add a Bot.
2. Under **Bot → Privileged Gateway Intents**, enable **Message Content Intent**.
3. Invite it to the server with the `bot` and `applications.commands` scopes and the *Read Messages*, *Send Messages* and *Read Message History* permissions. Existing installations may need to be reauthorized with `applications.commands`. See [JDA's command setup guide](https://jda.wiki/using-jda/interactions/#creating-slash-commands).
4. Start it:

```bash
DISCORD_TOKEN=your-token scala-cli run .
```

Never commit the token.

Slash commands are registered automatically when the bot connects. Server administrators can restrict who may run them in Discord's integration settings.

## Docker

Every push to `main` builds and publishes an image to GitHub Container Registry (`ghcr.io/icdocsoc/docbot-2627`, tagged `latest` and by commit SHA; `v*` tags get a version tag too).

```bash
docker run -d --restart unless-stopped -e DISCORD_TOKEN=your-token -v docbot-data:/data ghcr.io/icdocsoc/docbot-2627:latest
```

To build locally: `docker build -t docbot .`

The Docker build runs the test suite before packaging, including on pull requests in CI.

The image stores draws in `/data/secretsanta`; reuse the named volume when replacing the container. The DigitalOcean App Platform spec in `.do/app.yaml` uses an ephemeral filesystem: draws will be lost on redeploy unless the bot is moved to a host with persistent storage (or the store is replaced with an external database).

## Tests

```bash
scala-cli test .
```

## Project layout

- `src/Bot.scala`: Discord wiring (JDA). New features hook in via `Listener`.
- `src/Snark.scala`: the spelling check and the reply pool. Pure logic, no Discord code, so it's easy to test.
- `src/Snark.test.scala`: tests for the above.
- `src/SecretSanta.scala`: participant validation, allocation, hint routing, persistent draws and delivery progress.
- `src/SecretSantaListener.scala`: slash commands, assignment DMs, retries and hint modals.
- `src/SecretSanta.test.scala`: allocation, routing, persistence and failed-delivery retry tests.

## Adding a feature

1. Put the logic in its own file under `src/`, keeping it free of Discord types where you can so it can be unit tested.
2. Call it from `Listener` in `Bot.scala` (or add another listener, and any extra gateway intents, in `run`).
3. Add a section to "What it does today" above.
