FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache wget tini
USER app
WORKDIR /app

COPY --chown=app:app services/api-gateway/build/libs/api-gateway-*-boot.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
