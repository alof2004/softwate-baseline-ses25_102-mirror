FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN groupadd --system app && useradd --system --gid app --create-home app \
    && chown app:app /app/app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
