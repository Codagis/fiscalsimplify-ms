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
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -XX:InitialRAMPercentage=20.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxGCPauseMillis=150 -XX:ParallelGCThreads=1 -XX:ConcGCThreads=1 -Xss512k -Dspring.backgroundpreinitializer.ignore=true"

EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
