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
FROM eclipse-temurin:25-jre@sha256:945247f9d092703e0c6be92243ef3c51640229e23ab34d353c8dcdc73e6cd4e6
RUN useradd --system --no-create-home docbot
COPY --from=build /out/docbot.jar /app/docbot.jar
USER docbot
# The bot reads its token from DISCORD_TOKEN; pass it at `docker run` time.
ENTRYPOINT ["java", "-jar", "/app/docbot.jar"]
