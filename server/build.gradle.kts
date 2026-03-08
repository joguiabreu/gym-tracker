plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "com.gymtracker"
version = "0.1.0"

application {
    mainClass.set("com.gymtracker.api.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)

    // Ktor Client (for calling Claude API)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.logback)
}
