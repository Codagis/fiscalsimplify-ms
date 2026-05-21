# Build e execução para deploy no Railway (Java 17 / Spring Boot)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package -q && \
    cp "$(find target -maxdepth 1 -name '*.jar' ! -name 'original-*' | head -n 1)" /app/application.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/application.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENV SPRING_PROFILES_ACTIVE=homolog,railway

EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
