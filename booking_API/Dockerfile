FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Step 1: Cache dependencies (only re-runs if pom.xml changes)
COPY pom.xml .
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <name>Maven Central Mirror</name>
      <url>https://repo1.maven.org/maven2</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>network-stability</id>
      <properties>
        <maven.wagon.httpconnectionManager.ttlSeconds>30</maven.wagon.httpconnectionManager.ttlSeconds>
        <maven.wagon.http.retryHandler.count>5</maven.wagon.http.retryHandler.count>
        <maven.wagon.http.pool>false</maven.wagon.http.pool>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>network-stability</activeProfile>
  </activeProfiles>
</settings>
EOF

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
    && rm -rf /var/lib/apt/lists/*

# Use specific JAR name or wildcard, but ensure we only get the main artifact
COPY --from=build /app/target/booking_mvp-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
