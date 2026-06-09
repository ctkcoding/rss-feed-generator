# syntax=docker/dockerfile:1

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew gradlew.bat ./
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties ./gradle/wrapper/
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ ./src/

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080

# Default application.properties values baked in as fallback
# Spring Boot os-env property source overrides these at runtime via env vars
ENV RSS_ARTWORK_DIR=artwork
ENV RSS_EPISODES_DIR=episodes
ENV RSS_INFO_DIR=info
ENV RSS_RSS_FILE_NAME=rss.xml
ENV RSS_SHOW_FILE_NAME=show.json
ENV RSS_EPISODE_FILE_EXTENSION=.mp3
ENV RSS_ARTWORK_FILE_EXTENSION=.jpeg
ENV RSS_EXTRACT_ARTWORK=false
ENV RSS_FILE_WATCH=true
ENV RSS_RUN_ON_STARTUP=false
ENV RSS_LANGUAGE=en-us
ENV RSS_ERROR_LOG_FILE=parse-errors.log
ENV RSS_FAILURE_LIMIT=5

ENTRYPOINT ["java", "-jar", "app.jar"]
