FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache wget tini
USER app
WORKDIR /app

COPY --chown=app:app services/auth-api/build/libs/auth-api-*-boot.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
