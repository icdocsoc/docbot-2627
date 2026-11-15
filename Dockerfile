# syntax=docker/dockerfile:1

# ---- Build: package everything into one fat jar ----
FROM virtuslab/scala-cli:1.5.1 AS build
WORKDIR /src
COPY project.scala ./
COPY src ./src
RUN mkdir -p /out && scala-cli --power package . --assembly --main-class docsoc.run -o /out/docbot.jar

# ---- Runtime: just a JRE and the jar ----
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home docbot
COPY --from=build /out/docbot.jar /app/docbot.jar
USER docbot
# The bot reads its token from DISCORD_TOKEN; pass it at `docker run` time.
ENTRYPOINT ["java", "-jar", "/app/docbot.jar"]
