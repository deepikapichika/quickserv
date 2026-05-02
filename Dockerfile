# Multi-stage Dockerfile for building and running the QuickServ Spring Boot app
# Stage 1 - build with Maven wrapper
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# copy mvnw and maven wrapper dir first for faster rebuilds
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw -DskipTests clean package -DskipITs || mvn -DskipTests clean package

# Stage 2 - runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
