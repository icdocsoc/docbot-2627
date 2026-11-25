# syntax=docker/dockerfile:1

# ---- Build: package everything into one fat jar ----
# Base images are pinned by digest (the tag is kept for readability); Dependabot bumps both together.
FROM virtuslab/scala-cli:1.19.0@sha256:d1715b5c5085db2d461a293757561f240ec6da955846a4f5d2c0f5cc30ae2f9d AS build
WORKDIR /src
COPY project.scala ./
COPY src ./src
# --server=false: no Bloop compile server in a one-shot build (skips its download and startup).
RUN mkdir -p /out && scala-cli --power package . --server=false --assembly --main-class docsoc.run -o /out/docbot.jar

# ---- Runtime: just a JRE and the jar ----
FROM eclipse-temurin:25-jre@sha256:4ed132f6832e9804b123e89a849a7b4502756ae64a4ee08ebab939d9d5d0a9fa
RUN useradd --system --no-create-home docbot
COPY --from=build /out/docbot.jar /app/docbot.jar
USER docbot
# The bot reads its token from DISCORD_TOKEN; pass it at `docker run` time.
ENTRYPOINT ["java", "-jar", "/app/docbot.jar"]
