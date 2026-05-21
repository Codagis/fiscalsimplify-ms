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

COPY railway-jvm.opts /app/railway-jvm.opts

EXPOSE 8081

CMD ["java", "@/app/railway-jvm.opts", "-jar", "app.jar"]

