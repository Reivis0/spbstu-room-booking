FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Step 1: Cache dependencies (only re-runs if pom.xml changes)
COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Step 2: Compile (only re-runs if src/ changes)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -m spring

USER spring

# Use specific JAR name or wildcard, but ensure we only get the main artifact
COPY --from=build --chown=spring:spring /app/target/booking_mvp-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
