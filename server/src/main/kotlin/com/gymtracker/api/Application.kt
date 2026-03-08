package com.gymtracker.api

import com.gymtracker.ai.ClaudeClient
import com.gymtracker.shared.ErrorResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable not set")

    val model = System.getenv("CLAUDE_MODEL") ?: "claude-sonnet-4-20250514"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val claude = ClaudeClient(apiKey, model)
    val workoutService = WorkoutService(claude)

    embeddedServer(Netty, port = port) {
        configureServer(workoutService)
    }.start(wait = true)
}

fun Application.configureServer(workoutService: WorkoutService) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Post)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val message = cause.message ?: "Internal server error"
            call.application.environment.log.error("Request failed", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(message))
        }
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }
        workoutRoutes(workoutService)
    }
}
