
package com.aniflix.api

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class DownloadRequest(
    val url: String,
    val quality: String = "h"
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/") {
                call.respondText("AniFlix Downloader API")
            }

            get("/health") {
                call.respond(
                    mapOf(
                        "status" to "ok",
                        "service" to "AniFlix Downloader API"
                    )
                )
            }

            post("/download") {
                val request = runCatching {
                    call.receive<DownloadRequest>()
                }.getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid JSON request")
                    )
                    return@post
                }

                if (request.quality !in setOf("h", "m", "l")) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Quality must be h, m, or l")
                    )
                    return@post
                }

                val uri = runCatching {
                    URI(request.url)
                }.getOrNull()

                val host = uri?.host?.lowercase()

                val allowedHost = host != null && (
                    host == "abyss.to" ||
                    host.endsWith(".abyss.to") ||
                    host == "abysscdn.com" ||
                    host.endsWith(".abysscdn.com")
                )

                if (uri?.scheme != "https" || !allowedHost) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Only supported Abyss video URLs are allowed")
                    )
                    return@post
                }

                val id = UUID.randomUUID().toString()
                val output = File("/tmp/$id.mp4")
                val log = File("/tmp/$id.log")

                try {
                    val result = withContext(Dispatchers.IO) {
                        val process = ProcessBuilder(
                            "java",
                            "-jar",
                            "/app/abyss-dl.jar",
                            request.url,
                            request.quality,
                            "-o",
                            output.absolutePath,
                            "-c",
                            "4"
                        )
                            .redirectErrorStream(true)
                            .redirectOutput(log)
                            .start()

                        val finished = process.waitFor(10, TimeUnit.MINUTES)

                        if (!finished) {
                            process.destroyForcibly()
                            return@withContext false
                        }

                        process.exitValue() == 0 &&
                            output.exists() &&
                            output.length() > 0
                    }

                    if (!result) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Download failed or timed out")
                        )
                        return@post
                    }

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"AniFlix-$id.mp4\""
                    )

                    call.respondFile(output)

                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Unable to download this video")
                    )
                } finally {
                    log.delete()
                }
            }
        }
    }.start(wait = true)
}
