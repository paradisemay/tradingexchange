# Builder — использует gradlew (враппер), чтобы брать правильную версию Gradle из gradle-wrapper.properties
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/server/build/install/server .
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
