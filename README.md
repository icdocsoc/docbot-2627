# DoCBoT

The Discord bot for [DoCSoc](https://github.com/icdocsoc), Imperial College's Department of Computing Society. (Yes, the name is spelt that way on purpose. Please don't tell it.)

DoCBoT is meant to grow: this is the home for whatever small, useful, or silly things we want our Discord server to do. For now it polices spelling and has `/ping` and `/randomduck` slash commands.

## What it does today

### Spelling police
If anyone writes "DoCSoc" with the wrong capitalisation (anything except `DoCSoc` and all-lowercase `docsoc`: `DoCSoC`, `DOCSOC`, `Docsoc`, ...), or doesn't write "IC Hack" exactly (capital I, capital C, one space, capital H, lowercase "ack") or all-lowercase "ic hack" (one space): `ICHack`, `ichack`, `ICHACK`, `Ic Hack`, `IC  Hack`, `IC-Hack`, `ic-hack`, `I.C. Hack`, `IC hack`, `IC HACK`, ... The correction also fires for `ICHack26`-style (the digits don't save it). "IC Hack", "ic hack" and "IC Hack 26" are fine (the reply always says "IC Hack"), DoCBoT replies to their message with a snarky correction. It picks a random reply from a pool of 90-odd quotes, never the same one twice in a row. The pool covers Monty Python, MCU, Star Trek, Brooklyn Nine-Nine, assorted pop culture, famous musicals, Shakespeare, famous lines from British and Western history (Caesar, Churchill, Nelson, and friends), Stargate, and James Bond.

A message that misspells both names gets a single reply, correcting the first one found (DoCSoc, then IC Hack).

Known limits: it only checks capitalisation and spacing/punctuation (not typos like "docsock"), and URLs are ignored. "DoCSoc" still matches inside longer words (e.g. "DOCSOCs"), but "IC Hack" variants are only flagged as a whole word-ish token (not after or before a letter, so "epic hack" and "ichacks" are left alone).

Invisible/zero-width characters (e.g. `doc\u200Bsoc`) are stripped before checking, so they can't be used to dodge the police.

### Slash commands
| Command | What it does |
| --- | --- |
| `/ping` | Replies "Pong!" (only visible to you). Proves the slash command wiring works. |
| `/randomduck` | Replies with a photo of a random rubber duck (see below). |

#### `/randomduck`
Shows a random rubber duck photo from the Wikimedia Commons category [Rubber ducks](https://commons.wikimedia.org/wiki/Category:Rubber_ducks) (direct files only, and only JPEG, PNG, GIF and WebP pictures). It never shows the same duck twice in a row.

- **Attribution:** the images are freely licensed, but many licences (such as CC BY and CC BY-SA) require crediting the author. Every reply is an embed whose title links to the picture's Commons file page, and whose description names the author and licence ("Title by Author, CC BY-SA 4.0, via Wikimedia Commons"). The file page has the full licence details.
- **Caching:** the list of ducks is fetched on the first use and then kept for 6 hours (the `DuckSource` behind `DuckLookup.Default` in `src/ducks/DuckLookup.scala`). If a refresh fails, the old list keeps being used. If there is no list at all, the bot replies "The ducks are on strike. Try again later."
- **Network:** the bot makes outbound HTTPS requests to `commons.wikimedia.org` (a few per refresh, with 5 second connect and 10 second request timeouts). Hosts with a firewall or egress allow-list (a DigitalOcean droplet, say) must allow that host. The pictures themselves are loaded by Discord from `upload.wikimedia.org`, not by the bot.
- **Replies:** the bot answers straight away with a "thinking" state and edits in the result (`DuckLookup` does the lookup on a small daemon thread pool), so a slow Commons can't hit Discord's 3 second limit. Titles and credits come from contributors, so markdown is escaped, `@` and `<` are defused and replies are sent with no allowed mentions: a duck can't ping anyone.

Slash commands need no Message Content intent, but the invite link must include the `applications.commands` scope (alongside `bot`). If the bot is already in your server, re-invite it with that scope added (no need to kick it first), otherwise the commands won't show up.

Commands are registered globally on startup, and Discord can take a while (up to about an hour) to show new or changed global commands. For testing, set the optional `DISCORD_GUILD_ID` env var to a server's ID (Discord's developer mode, then right-click the server, then Copy Server ID) and the commands are registered in just that server instead, where they update instantly. The bot must already be in that server, and with the variable set the commands are not registered globally (clear leftovers from earlier global registrations by restarting without it).

To add a command: create one file under `src/commands/` holding its `CommandSpec(name, description)` and its handler together (copy `src/commands/Ping.scala`), then add its `command` to `CommandRegistry.all` in `src/commands/CommandRegistry.scala`. That one list generates both the Discord registration and the dispatch, so the name is written in one place only. Tests (and a check at startup) fail if a name or description breaks Discord's rules (name `[\w-]{1,32}` and lowercase, description 1-100 characters, unique names, at most 100 global commands).

## Running it

Requires [scala-cli](https://scala-cli.virtuslab.org/) and a JDK.

1. Create an application at https://discord.com/developers/applications and add a Bot.
2. Under **Bot → Privileged Gateway Intents**, enable **Message Content Intent**.
3. Invite it to the server with the `bot` scope and the *Read Messages*, *Send Messages* and *Read Message History* permissions.
4. Add the `applications.commands` scope to the invite too, for the slash commands (see "Slash commands").
5. Start it:

```bash
DISCORD_TOKEN=your-token scala-cli run .
```

To register slash commands in one server only (instant updates, for testing), also set `DISCORD_GUILD_ID=your-server-id`.

Never commit the token.

## Docker

Every push to `main` (once the checks pass) builds and publishes an image to GitHub Container Registry (`ghcr.io/icdocsoc/docbot-2627`, tagged `latest` and by commit SHA; `v*` tags get a version tag too).

```bash
docker run -d --restart unless-stopped -e DISCORD_TOKEN=your-token ghcr.io/icdocsoc/docbot-2627:latest
```

To build locally: `docker build -t docbot .`

## Development

```bash
scala-cli test .                    # run the tests
scala-cli fmt .                     # format (scalafmt, see .scalafmt.conf)
scala-cli fmt --check .             # check formatting without changing files
scala-cli --power fix .             # lint and rewrite (scalafix, see .scalafix.conf)
scala-cli --power fix --check .     # check without changing files
```

Run `fmt` and `fix` before committing. `fix` also removes unused imports and keeps the `//> using` directives in `project.scala` tidy.

### CI

A single workflow, `.github/workflows/ci.yml` (named **CI**), runs on pushes to `main`, `v*` tags and pull requests to `main`:

1. **Format (scalafmt)**, **Lint (scalafix)** and **Test** run in parallel as separate jobs (`fmt --check`, `fix --check`, and `test` with unused code warnings as errors and coverage measured, see [Coverage](#coverage)).
2. **Coverage report** runs after **Test**, publishes the coverage summary and comments on the pull request. Nothing waits for it, so it never blocks a release.
3. **Docker (build and push)** builds the image, and pushes it to GHCR except on pull requests. It only runs if format, lint and test pass.
4. **Deploy (DigitalOcean)** redeploys the app on pushes to `main`.

Supply chain: third-party actions are pinned to full commit SHAs (the trailing comment is the tag) and the Dockerfile base images to digests; `.github/dependabot.yml` opens weekly grouped PRs to bump both. Workflow permissions default to none and each job grants only what it needs. scala-cli dependencies in `project.scala` are not covered by Dependabot, so watch for scala-cli's "outdated" hints. The scala-cli version is pinned in both `ci.yml` and the `Dockerfile`; keep them in sync.

CI passes `--server=false` to scala-cli: a fresh runner gains nothing from the Bloop compile server, which only adds a large download and startup time. Locally, leave it on for faster incremental builds.

## Coverage

CI measures test coverage with the Scala 3 compiler's built-in scoverage instrumentation. It is turned on only for the CI test run (`-coverage-out`), never in `project.scala`, so the Docker image is not instrumented. It is report-only: low coverage does not fail the build. Each run produces:

- **Run summary**: a coverage table on the workflow run's summary page (job **Coverage report**), coloured green from 80% and yellow from 60% line coverage.
- **Artifact**: `coverage-report` (kept 14 days) with `cobertura.xml` and an `html/` report to browse (open `html/index.html`).
- **Pull request comment**: the same table as a single comment that is updated on every push. It is only posted for pull requests from this repository (the token is read-only for forks); the summary and artifact are always produced.

The tests (`*.test.scala`), `src/Bot.scala` and everything under `src/commands/` are excluded: they are Discord glue (JDA types, thin handlers) that needs a live connection. The testable logic (such as `CommandSpec` and its validation, and all of `src/ducks/`) lives outside `src/commands/`, so it stays in the report. Excluding only the tests, the figures are 86.4% of statements (648 of 750) and 81.1% of branches; with all the exclusions, 95.9% of statements (637 of 664) and 93.6% of branches. The gap in `src/ducks/` is mostly `DuckLookup.Default`, the real wiring (the daemon pool and the live Wikimedia client), which tests replace with fakes. Keep that in mind when reading the numbers: "100%" covers the logic, not the wiring. To change the exclusions, edit the `-coverage-exclude-files` regexes in the **Test** job of `ci.yml` (they match the source path without its `.scala` extension).

To produce the same report locally (needs scala-cli and a JDK; results land in `coverage/report/`):

```bash
rm -rf coverage   # drop data from earlier runs
scala-cli test . --scalac-option -coverage-out:coverage/data --scalac-option '-coverage-exclude-files:.*\.test,.*/Bot,.*/commands/.*'
scala-cli run .github/coverage/Report.scala -- coverage/data . coverage/report
```

The compiler only writes raw data; `.github/coverage/Report.scala` converts it with the [scoverage reporter](https://github.com/scoverage/scalac-scoverage-plugin). It sits in a hidden directory, so `scala-cli test`, `fmt` and `fix` on `.` ignore it.

To require a minimum later, set `fail_below_min: true` on the **Summarise coverage** step in `ci.yml`: the build then fails when line coverage drops below the lower threshold (the first number of `thresholds`, currently 60).

## Project layout

- `src/Bot.scala`: the entry point (token, JDA with its intents, listeners, command registration) and `Listener`, the message-based spelling police.
- `src/CommandSpec.scala`: the `CommandSpec` type (name, description) and the validation of Discord's rules (`CommandSpec.validate`). Pure logic, no Discord code.
- `src/CommandSpec.test.scala`: tests for the validation rules.
- `src/commands/`: the slash commands. Discord glue (JDA types, thin handlers), excluded from coverage; put testable logic outside this directory.
  - `Command.scala`: the `Handler` type and `Command(spec, handler)`.
  - `Ping.scala`: the `/ping` spec and handler. One file per command.
  - `RandomDuck.scala`: the `/randomduck` spec and its thin Discord glue only: defers the reply, calls `DuckLookup.Default.random`, builds the embed from the `DuckReply` (no allowed mentions) or sends the failure message. No duck logic. Logs as `docsoc.randomduck`.
  - `CommandRegistry.scala`: `all`, the single list of commands; dispatch by name, the startup validation and the registration with Discord (globally or in `DISCORD_GUILD_ID`).
  - `SlashCommandListener.scala`: the JDA listener that hands slash command interactions to the registry.
  - `CommandRegistry.test.scala`: tests that the registered commands follow Discord's rules, are unique and include `/ping` and `/randomduck`.
- `src/Correction.scala`: the generic mechanism. The `Correction` type (correct spelling + pattern), the shared normalisation (invisible characters, URLs) and `Correction.firstMisspelt`, which finds the first matching correction in a given list. Pure logic, no Discord code, and it knows nothing about specific names.
- `src/Corrections.scala`: the setup. Only the concrete definitions (DoCSoc and IC Hack, each with its pattern and a comment explaining it) and the ordered `Corrections.all` list that the bot enforces.
- `src/Quotes.scala`: the shared pool of reply templates (`{correct}` and `{user}` placeholders) and the random, no-immediate-repeat `Quotes.pick`. Reusable for other corrections.
- `src/Correction.test.scala`: tests for the mechanism, using small local corrections (normalisation, URL and invisible-character handling, first-match ordering).
- `src/Corrections.test.scala`: tests for the concrete DoCSoc and IC Hack behaviour and their order in `Corrections.all`.
- `src/Quotes.test.scala`: tests for the quote pool and `Quotes.pick`.
- `src/ducks/`: everything about rubber ducks (package `docsoc.ducks`), with no Discord or JDA code, so all of it is unit tested and counted in coverage. `src/commands/RandomDuck.scala` is the only thing that touches Discord.
  - `Ducks.scala`: rubber duck photos from Wikimedia Commons (`Category:Rubber_ducks`). The `Duck` type, `Ducks.parse` for the API response (image files only, attribution built from the metadata), `Ducks.continuation` and `Ducks.mergePages` (Commons returns the file details in several pages), the random, no-immediate-repeat `Ducks.pick`, and `DuckSource`, a cached source with an injected fetch function.
  - `Ducks.test.scala`: tests for the above, using a trimmed real API response as a fixture (no network).
  - `Wikimedia.scala`: the HTTP client for the duck list (`java.net.http`, no extra dependency): User-Agent, timeouts, only HTTP 200 is success, follows the API's `continue` marker, and every failure is a `Left`. The URL is a constructor parameter so tests can use a local server.
  - `Wikimedia.test.scala`: tests for the client against an in-process `HttpServer` on localhost (no real network).
  - `DuckReply.scala`: what `/randomduck` shows, as plain strings (no JDA types): title, image, credit description with markdown escaped, mentions defused and Discord's length limits applied, plus the failure message.
  - `DuckReply.test.scala`: tests for the reply builder (escaping, mentions, truncation).
  - `DuckLookup.scala`: the one entry point for front ends. `DuckLookup(source, executor).random(onDone)` looks a duck up off the caller's thread and calls `onDone` exactly once with `Right(DuckReply)` or `Left(failure message)`, never throwing (a failing source, a rejecting executor or a throwing callback are all handled and logged as `docsoc.ducks`). `DuckLookup.Default` is the real one: the shared `DuckSource` over `Wikimedia` with a 6 hour cache, on four daemon threads.
  - `DuckLookup.test.scala`: tests with a fake source and inline, rejecting and real single-thread executors.

## Adding a correction

To police another name, edit only `src/Corrections.scala`: define a `Correction("Right Spelling", "(?i)regex".r)` (the regex should find every spelling of the name, right or wrong; anything that isn't exactly the right spelling gets flagged), add it to `Corrections.all` (earlier entries win when a message breaks several), and add a behaviour table for it in `src/Corrections.test.scala`. `Bot.scala` and `Correction.scala` need no changes.

## Adding a feature

1. Put the logic in its own file under `src/`, keeping it free of Discord types where you can so it can be unit tested.
2. Call it from `Listener` in `Bot.scala` (or add another listener, and any extra gateway intents, in `run`). For a slash command, add a file under `src/commands/` and list it in `CommandRegistry.all` instead, keeping any logic worth testing outside that directory.
3. Add a section to "What it does today" above.
