FROM gradle:8.13-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :server:installDist --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache libstdc++ curl
COPY --from=builder /app/server/build/install/server .
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
