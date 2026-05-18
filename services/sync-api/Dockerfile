# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY buildSrc ./buildSrc
RUN ./gradlew --no-daemon help -q || true
COPY libs ./libs
COPY services/sync-api ./services/sync-api
RUN ./gradlew --no-daemon :services:sync-api:bootJar -x test

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache wget tini
USER app
WORKDIR /app
COPY --from=builder --chown=app:app \
    /workspace/services/sync-api/build/libs/sync-api-*-boot.jar /app/app.jar
EXPOSE 8083
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
