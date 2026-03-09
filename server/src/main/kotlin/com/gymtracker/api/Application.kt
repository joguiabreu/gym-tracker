package com.gymtracker.api

import com.gymtracker.ai.AiClient
import com.gymtracker.ai.ClaudeClient
import com.gymtracker.ai.MockAiClient
import com.gymtracker.shared.ErrorResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import org.slf4j.event.Level
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * Entry point: Ktor's EngineMain reads application.conf, starts Netty,
 * and calls this module function. No manual embeddedServer() needed.
 *
 * Config lives in application.conf. Defaults to mock mode so you can
 * just run `./gradlew :server:run` with zero setup. For production,
 * set GYM_MODE=live and ANTHROPIC_API_KEY env vars.
 */
fun Application.module() {
    val config = environment.config
    val mode = config.property("gymtracker.mode").getString()
    val isMock = mode == "mock"

    val aiClient: AiClient = if (isMock) {
        log.info("*** MOCK MODE — no Claude API calls, using fake responses ***")
        MockAiClient()
    } else {
        val apiKey = config.property("gymtracker.claude.apiKey").getString()
        require(apiKey.isNotBlank()) { "ANTHROPIC_API_KEY not set. Use GYM_MODE=mock to run without it." }
        val model = config.property("gymtracker.claude.model").getString()
        log.info("*** LIVE MODE — using Claude model: {} ***", model)
        ClaudeClient(apiKey, model)
    }

    val workoutService = WorkoutService(aiClient)

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        })
    }

    install(CallLogging) {
        level = Level.INFO
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
            call.respondText("OK (mode=$mode)")
        }
        workoutRoutes(workoutService)
    }
}
