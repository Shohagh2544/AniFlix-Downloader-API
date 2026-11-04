# AniFlix Downloader API

Java 21 / Kotlin HTTP API starter for integrating the AbyssVideoDownloader engine with AniFlix.

## Current status

- `GET /` and `GET /health` are working health endpoints.
- `POST /download` validates the request but intentionally returns HTTP 501.
- **This starter does not download videos yet.** The original command-line downloader still needs to be adapted and wired into the API.

## Run locally

Requires JDK 21 and Gradle 8.13+.

```bash
gradle run
```

The server listens on `PORT` (default `8080`).

## Deploy to Railway

1. Push this project to the `AniFlix-Downloader-API` GitHub repository.
2. Create a Railway service from that repository.
3. Let Railway use the root `Dockerfile`.
4. Remove any custom Start Command; the Dockerfile already starts the web server.
5. Generate a public domain in Railway → Networking.
6. Visit `https://YOUR-DOMAIN/health`. It should return JSON with `"status":"ok"`.

## Planned request format

`POST /download`

```json
{ "url": "https://example.invalid/video", "quality": "h" }
```

Quality values: `h` (high), `m` (medium), `l` (low).

Before public use, implement the actual downloader engine, job tracking, progress reporting, file delivery, authentication, and rate limiting. Do not expose an unauthenticated download endpoint.
