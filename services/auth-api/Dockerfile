# syntax=docker/dockerfile:1.7

# ---------- builder ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY buildSrc ./buildSrc

RUN ./gradlew --no-daemon help -q || true

COPY libs ./libs
# clients/ is referenced by settings.gradle.kts; Gradle refuses to configure if
# the directory is missing.
COPY clients ./clients
# auth-api's openapi.yaml is the source used by clients/auth-api-client at
# generate time — copy the spec separately so openApiGenerate is reproducible.
COPY services/auth-api ./services/auth-api

RUN ./gradlew --no-daemon :services:auth-api:bootJar -x test

# ---------- runtime ----------
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache wget tini
USER app
WORKDIR /app

COPY --from=builder --chown=app:app \
    /workspace/services/auth-api/build/libs/auth-api-*-boot.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
