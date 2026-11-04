
# Build the Abyss downloader
FROM gradle:8.13-jdk21 AS abyss-build

WORKDIR /abyss

RUN apt-get update && apt-get install -y git
RUN git clone --depth 1 https://github.com/abdlhay/AbyssVideoDownloader.git .

RUN gradle --no-daemon shadowJar

# Build the AniFlix API
FROM gradle:8.13-jdk21 AS api-build

WORKDIR /app
COPY . .
RUN gradle --no-daemon installDist

# Final runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=api-build /app/build/install/AniFlix-Downloader-API/ ./
COPY --from=abyss-build /abyss/build/libs/abyss-dl-shadowJar.jar /app/abyss-dl.jar

ENV PORT=8080
EXPOSE 8080

CMD ["./bin/AniFlix-Downloader-API"]
