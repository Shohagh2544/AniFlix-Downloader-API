FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/AniFlix-Downloader-API/ ./
ENV PORT=8080
EXPOSE 8080
CMD ["./bin/AniFlix-Downloader-API"]
