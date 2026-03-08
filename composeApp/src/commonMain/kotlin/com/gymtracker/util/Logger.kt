package com.gymtracker.util

/**
 * Simple structured logger for KMP.
 *
 * Why not use a library? Most JVM logging frameworks (SLF4J, Logback) don't work
 * in WASM/JS. This gives us consistent logging across all targets using println,
 * which maps to console.log in browsers and stdout elsewhere.
 *
 * Each log line includes: timestamp, level, tag, message, and optional key-value context.
 * Structured context (key=value pairs) makes logs searchable and parseable — this is
 * a core observability practice. Instead of "Generated workout with 5 exercises",
 * you log "workout.generated" with exercises=5, target="chest", equipment=3.
 * Tools can then filter/aggregate on those fields.
 */
object Logger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    var minLevel: Level = Level.DEBUG

    fun debug(tag: String, message: String, vararg context: Pair<String, Any?>) =
        log(Level.DEBUG, tag, message, *context)

    fun info(tag: String, message: String, vararg context: Pair<String, Any?>) =
        log(Level.INFO, tag, message, *context)

    fun warn(tag: String, message: String, vararg context: Pair<String, Any?>) =
        log(Level.WARN, tag, message, *context)

    fun error(tag: String, message: String, error: Throwable? = null, vararg context: Pair<String, Any?>) {
        log(Level.ERROR, tag, message, *context)
        error?.let { println("  ↳ ${it::class.simpleName}: ${it.message}") }
    }

    private fun log(level: Level, tag: String, message: String, vararg context: Pair<String, Any?>) {
        if (level < minLevel) return

        val prefix = "${level.name.padEnd(5)} [$tag]"
        val contextStr = if (context.isNotEmpty()) {
            " | " + context.joinToString(", ") { "${it.first}=${it.second}" }
        } else ""

        println("$prefix $message$contextStr")
    }
}
