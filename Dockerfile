# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - build: full JDK + Maven, never shipped to production
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Dependencies are resolved in their own layer so that editing source code
# does not invalidate the (slow) dependency download.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - runtime: slim JRE, only the fat JAR is copied across
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="calculator-api" \
      org.opencontainers.image.description="Calculator REST API - CSE 816 SPE mini-project" \
      org.opencontainers.image.source="https://github.com/jayeshpandit/calculator-devops" \
      org.opencontainers.image.licenses="MIT"

# wget is used by the container HEALTHCHECK below
RUN apk add --no-cache wget \
    && addgroup -S -g 10001 appgroup \
    && adduser -S -u 10001 -G appgroup appuser

WORKDIR /app

COPY --from=build --chown=appuser:appgroup /build/target/calculator.jar app.jar

# Drop privileges: the JVM never runs as root
USER appuser:appgroup

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
