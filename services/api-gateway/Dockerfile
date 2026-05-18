# syntax=docker/dockerfile:1.7

# ---------- builder ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Copy the bare minimum to let Gradle resolve dependencies first — maximizes
# Docker layer cache hits when only source files change.
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY buildSrc ./buildSrc

# Resolve plugins + classpath without any service sources present. Allowed to
# fail (the project layout isn't complete yet) — we just want the cache primed.
RUN ./gradlew --no-daemon help -q || true

# Now bring in everything the gateway needs: common libs + the service itself.
COPY libs ./libs
COPY services/api-gateway ./services/api-gateway

RUN ./gradlew --no-daemon :services:api-gateway:bootJar -x test

# ---------- runtime ----------
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache wget tini
USER app
WORKDIR /app

COPY --from=builder --chown=app:app \
    /workspace/services/api-gateway/build/libs/api-gateway-*-boot.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
