# Build e execução para deploy no Railway (Java 17 / Spring Boot)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Usa o perfil homologação por padrão; pode ser sobrescrito no Railway via env.
ENV SPRING_PROFILES_ACTIVE=homolog

# Ajustes leves para container: menor risco de estourar memória.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:MinRAMPercentage=20"

EXPOSE 8081

CMD ["java","-jar","app.jar"]

