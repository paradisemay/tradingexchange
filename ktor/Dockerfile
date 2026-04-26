# Builder — использует gradlew (враппер), чтобы брать правильную версию Gradle из gradle-wrapper.properties
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /app/server/build/install/server .
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
