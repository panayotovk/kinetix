package com.kinetix.smoke

object SmokeTestConfig {
    val baseUrl: String = System.getenv("SMOKE_BASE_URL") ?: "http://localhost:8080"
    val timeoutMs: Long = System.getenv("SMOKE_TIMEOUT_MS")?.toLongOrNull() ?: 10_000
    val kafkaBootstrap: String = System.getenv("SMOKE_KAFKA_BOOTSTRAP") ?: "localhost:29092"

    val jwtSecret: String = System.getenv("SMOKE_JWT_SECRET") ?: "smoke-test-secret-key-for-local-dev"
    val jwtIssuer: String = System.getenv("SMOKE_JWT_ISSUER") ?: "kinetix"
    val jwtAudience: String = System.getenv("SMOKE_JWT_AUDIENCE") ?: "kinetix-api"

    val seededPortfolioId: String = System.getenv("SMOKE_SEEDED_PORTFOLIO") ?: "equity-growth"
    val seededInstrumentId: String = System.getenv("SMOKE_SEEDED_INSTRUMENT") ?: "AAPL"
}
