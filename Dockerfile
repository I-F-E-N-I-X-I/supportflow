FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :api:bootJar :worker:bootJar
FROM eclipse-temurin:21-jre AS api
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/api/build/libs/api-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
FROM eclipse-temurin:21-jre AS worker
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/worker/build/libs/worker-1.0.0.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
