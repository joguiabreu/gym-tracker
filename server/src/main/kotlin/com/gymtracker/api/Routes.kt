package com.gymtracker.api

import com.gymtracker.shared.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.workoutRoutes(service: WorkoutService) {

    post("/workout/generate") {
        val request = call.receive<GenerateRequest>()
        val workout = service.generateWorkout(request)
        call.respond(workout)
    }

    post("/workout/resuggest") {
        val request = call.receive<ResuggestRequest>()
        val workout = service.resuggestWorkout(request)
        call.respond(workout)
    }

    post("/split/generate") {
        val request = call.receive<SplitRequest>()
        val split = service.generateSplit(request)
        call.respond(split)
    }

    post("/summary/weekly") {
        val request = call.receive<WeeklySummaryRequest>()
        val summary = service.generateWeeklySummary(request)
        call.respond(summary)
    }

    post("/summary/monthly") {
        val request = call.receive<MonthlySummaryRequest>()
        val summary = service.generateMonthlySummary(request)
        call.respond(summary)
    }
}
