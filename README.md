# DoCBoT

The Discord bot for [DoCSoc](https://github.com/icdocsoc), Imperial College's Department of Computing Society. (Yes, the name is spelt that way on purpose. Please don't tell it.)

DoCBoT is meant to grow: this is the home for whatever small, useful, or silly things we want our Discord server to do. For now it does exactly one thing.

## What it does today

### Spelling police
If anyone writes "DoCSoc" with the wrong capitalisation (`docsoc`, `DoCSoC`, `DOCSOC`, ...), or misspells "ICHack" (`IC Hack`, `ic-hack`, `I.C. Hack`, `ichack`, `ICHACK`, `IC Hack 26`, ...; `ICHack26` is fine), DoCBoT replies to their message with a snarky correction. It picks a random reply from a pool of 90-odd quotes, never the same one twice in a row. The pool covers Monty Python, MCU, Star Trek, Brooklyn Nine-Nine, assorted pop culture, famous musicals, Shakespeare, famous lines from British and Western history (Caesar, Churchill, Nelson, and friends), Stargate, and James Bond.

A message that misspells both names gets a single reply, correcting the first one found (DoCSoc, then ICHack).

Known limits: it only checks capitalisation and spacing/punctuation (not typos like "docsock"), and URLs are ignored. "DoCSoc" still matches inside longer words, but "ICHack" is only flagged as a whole word-ish token (not after or before a letter, so "epic hack" and "ichacks" are left alone).

Invisible/zero-width characters (e.g. `doc\u200Bsoc`) are stripped before checking, so they can't be used to dodge the police.

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
- `src/Corrections.scala`: the `Correction` type (correct spelling + pattern) and the DoCSoc and ICHack instances, plus the shared normalisation (invisible characters, URLs). Pure logic, no Discord code, so it's easy to test. To police another name, add a `Correction` to `Corrections.all`.
- `src/Snark.scala`: thin DoCSoc-specific wrapper around `Corrections.DoCSoc`.
- `src/Quotes.scala`: the shared pool of reply templates (`{correct}` and `{user}` placeholders) and the random, no-immediate-repeat `Quotes.pick`. Reusable for other corrections.
- `src/Snark.test.scala`, `src/Corrections.test.scala`: tests for the above.

## Adding a feature

1. Put the logic in its own file under `src/`, keeping it free of Discord types where you can so it can be unit tested.
2. Call it from `Listener` in `Bot.scala` (or add another listener, and any extra gateway intents, in `run`).
3. Add a section to "What it does today" above.
