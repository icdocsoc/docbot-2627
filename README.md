# DoCBoT

The Discord bot for [DoCSoc](https://github.com/icdocsoc), Imperial College's Department of Computing Society. (Yes, the name is spelt that way on purpose. Please don't tell it.)

DoCBoT is meant to grow: this is the home for whatever small, useful, or silly things we want our Discord server to do. For now it does exactly one thing.

## What it does today

### Spelling police
If anyone writes "DoCSoc" with the wrong capitalisation (`docsoc`, `DoCSoC`, `DOCSOC`, ...), DoCBoT replies to their message with a snarky correction. It rotates through a pool of replies (Monty Python, MCU, Star Trek, Brooklyn Nine-Nine, and assorted pop culture) so it doesn't repeat itself.

Known limits: it only checks capitalisation (not typos like "docsock"), and it also matches inside URLs and longer words.

## Running it

Requires [scala-cli](https://scala-cli.virtuslab.org/) and a JDK.

1. Create an application at https://discord.com/developers/applications and add a Bot.
2. Under **Bot → Privileged Gateway Intents**, enable **Message Content Intent**.
3. Invite it to the server with the `bot` scope and the *Read Messages*, *Send Messages* and *Read Message History* permissions.
4. Start it:

```bash
DISCORD_TOKEN=your-token scala-cli run .
```

Never commit the token.

## Docker

Every push to `main` builds and publishes an image to GitHub Container Registry (`ghcr.io/icdocsoc/docbot-2627`, tagged `latest` and by commit SHA; `v*` tags get a version tag too).

```bash
docker run -d --restart unless-stopped -e DISCORD_TOKEN=your-token ghcr.io/icdocsoc/docbot-2627:latest
```

To build locally: `docker build -t docbot .`

## Tests

```bash
scala-cli test .
```

## Project layout

- `src/Bot.scala`: Discord wiring (JDA). New features hook in via `Listener`.
- `src/Snark.scala`: the spelling check and the reply pool. Pure logic, no Discord code, so it's easy to test.
- `src/Snark.test.scala`: tests for the above.

## Adding a feature

1. Put the logic in its own file under `src/`, keeping it free of Discord types where you can so it can be unit tested.
2. Call it from `Listener` in `Bot.scala` (or add another listener, and any extra gateway intents, in `run`).
3. Add a section to "What it does today" above.
