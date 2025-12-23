FROM maven:3.9.6-eclipse-temurin-17 AS build

RUN apt-get update && apt-get install -y protobuf-compiler

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
