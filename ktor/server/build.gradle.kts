plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

// Force updated transitive deps to resolve Snyk high findings (CWE-770, CVE-2024-25710, CVE-2025-48924)
// Note: SNYK-JAVA-COMFASTERXMLJACKSONCORE-15907551 requires jackson-core 2.21.2 which is unreleased;
//       mitigated by not exposing Flyway JSON parsing to untrusted input.
configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-core:2.18.6")
        force("org.apache.commons:commons-compress:1.26.0")
        force("org.apache.commons:commons-lang3:3.18.0")
    }
}

dependencies {
    implementation(project(":core"))

    // Ktor server
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.websockets)

    // Database
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Redis
    implementation(libs.lettuce.core)

    // Security
    implementation(libs.argon2.jvm)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}
