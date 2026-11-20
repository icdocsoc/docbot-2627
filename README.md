# DoCBoT

The Discord bot for [DoCSoc](https://github.com/icdocsoc), Imperial College's Department of Computing Society. (Yes, the name is spelt that way on purpose. Please don't tell it.)

DoCBoT is meant to grow: this is the home for whatever small, useful, or silly things we want our Discord server to do. For now it does exactly one thing.

## What it does today

### Spelling police
If anyone writes "DoCSoc" with the wrong capitalisation (anything except `DoCSoc` and all-lowercase `docsoc`: `DoCSoC`, `DOCSOC`, `Docsoc`, ...), or doesn't write "IC Hack" exactly (capital I, capital C, one space, capital H, lowercase "ack") or all-lowercase "ic hack" (one space): `ICHack`, `ichack`, `ICHACK`, `Ic Hack`, `IC  Hack`, `IC-Hack`, `ic-hack`, `I.C. Hack`, `IC hack`, `IC HACK`, ... The correction also fires for `ICHack26`-style (the digits don't save it). "IC Hack", "ic hack" and "IC Hack 26" are fine (the reply always says "IC Hack"), DoCBoT replies to their message with a snarky correction. It picks a random reply from a pool of 90-odd quotes, never the same one twice in a row. The pool covers Monty Python, MCU, Star Trek, Brooklyn Nine-Nine, assorted pop culture, famous musicals, Shakespeare, famous lines from British and Western history (Caesar, Churchill, Nelson, and friends), Stargate, and James Bond.

A message that misspells both names gets a single reply, correcting the first one found (DoCSoc, then IC Hack).

Known limits: it only checks capitalisation and spacing/punctuation (not typos like "docsock"), and URLs are ignored. "DoCSoc" still matches inside longer words (e.g. "DOCSOCs"), but "IC Hack" variants are only flagged as a whole word-ish token (not after or before a letter, so "epic hack" and "ichacks" are left alone).

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

The tests (`*.test.scala`) and `src/Bot.scala` are excluded: `Bot.scala` is pure Discord wiring that needs a live connection. Without that exclusion the figures are 90.9% of statements (159 of 175) and 66.7% of branches; with it, 100% of both (159 of 159 statements, 4 of 4 branches). Keep that in mind when reading the numbers: "100%" covers the logic, not the wiring. To change the exclusions, edit the `-coverage-exclude-files` regexes in the **Test** job of `ci.yml` (they match the source path without its `.scala` extension).

To produce the same report locally (needs scala-cli and a JDK; results land in `coverage/report/`):

```bash
rm -rf coverage   # drop data from earlier runs
scala-cli test . --scalac-option -coverage-out:coverage/data --scalac-option '-coverage-exclude-files:.*\.test,.*/Bot'
scala-cli run .github/coverage/Report.scala -- coverage/data . coverage/report
```

The compiler only writes raw data; `.github/coverage/Report.scala` converts it with the [scoverage reporter](https://github.com/scoverage/scalac-scoverage-plugin). It sits in a hidden directory, so `scala-cli test`, `fmt` and `fix` on `.` ignore it.

To require a minimum later, set `fail_below_min: true` on the **Summarise coverage** step in `ci.yml`: the build then fails when line coverage drops below the lower threshold (the first number of `thresholds`, currently 60).

## Project layout

- `src/Bot.scala`: Discord wiring (JDA). New features hook in via `Listener`.
- `src/Correction.scala`: the generic mechanism. The `Correction` type (correct spelling + pattern), the shared normalisation (invisible characters, URLs) and `Correction.firstMisspelt`, which finds the first matching correction in a given list. Pure logic, no Discord code, and it knows nothing about specific names.
- `src/Corrections.scala`: the setup. Only the concrete definitions (DoCSoc and IC Hack, each with its pattern and a comment explaining it) and the ordered `Corrections.all` list that the bot enforces.
- `src/Quotes.scala`: the shared pool of reply templates (`{correct}` and `{user}` placeholders) and the random, no-immediate-repeat `Quotes.pick`. Reusable for other corrections.
- `src/Correction.test.scala`: tests for the mechanism, using small local corrections (normalisation, URL and invisible-character handling, first-match ordering).
- `src/Corrections.test.scala`: tests for the concrete DoCSoc and IC Hack behaviour and their order in `Corrections.all`.
- `src/Quotes.test.scala`: tests for the quote pool and `Quotes.pick`.

## Adding a correction

To police another name, edit only `src/Corrections.scala`: define a `Correction("Right Spelling", "(?i)regex".r)` (the regex should find every spelling of the name, right or wrong; anything that isn't exactly the right spelling gets flagged), add it to `Corrections.all` (earlier entries win when a message breaks several), and add a behaviour table for it in `src/Corrections.test.scala`. `Bot.scala` and `Correction.scala` need no changes.

## Adding a feature

1. Put the logic in its own file under `src/`, keeping it free of Discord types where you can so it can be unit tested.
2. Call it from `Listener` in `Bot.scala` (or add another listener, and any extra gateway intents, in `run`).
3. Add a section to "What it does today" above.
