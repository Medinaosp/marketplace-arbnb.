FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/marketplace-*.jar /app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app.jar ./app.jar
ENV TZ=America/Bogota
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-jar", "/app/app.jar"]
