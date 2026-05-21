# Build e execução para deploy no Railway (Java 17 / Spring Boot)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=homolog,railway
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=55.0 -XX:InitialRAMPercentage=25.0 -XX:+UseG1GC -XX:MaxMetaspaceSize=192m -XX:ParallelGCThreads=2 -Xss512k"

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]

