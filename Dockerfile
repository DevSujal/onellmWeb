## Multi-stage build for Spring Boot (Java 17)

FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Cache dependencies first
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Build
COPY src ./src
RUN mvn -q -DskipTests package \
    && JAR_FILE=$(ls -1 target/*.jar | grep -vE '(sources|javadoc|original)\\.jar$' | head -n 1) \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar


FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /app/app.jar /app/app.jar

USER app

ENV JAVA_OPTS=""
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
