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
import kotlinx.serialization.Serializable

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
        install(ContentNegotiation) { json() }

        routing {
            get("/") {
                call.respondText("AniFlix Downloader API", ContentType.Text.Plain)
            }

            get("/health") {
                call.respond(mapOf("status" to "ok", "service" to "AniFlix Downloader API"))
            }

            post("/download") {
                val request = runCatching { call.receive<DownloadRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON request"))
                    return@post
                }

                if (request.url.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("url is required"))
                    return@post
                }

                if (request.quality !in setOf("h", "m", "l")) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("quality must be h, m, or l"))
                    return@post
                }

                // The AbyssVideoDownloader engine is not yet adapted to this API.
                call.respond(
                    HttpStatusCode.NotImplemented,
                    ErrorResponse("API is running, but the Abyss downloader engine is not connected yet.")
                )
            }
        }
    }.start(wait = true)
}
