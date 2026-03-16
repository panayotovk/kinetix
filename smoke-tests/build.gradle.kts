plugins {
    id("kinetix.kotlin-common")
    id("kinetix.kotlin-testing")
}

dependencies {
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.kafka.clients)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("*SmokeTest")
        isFailOnNoMatchingTests = false
    }
}

val testSourceSets = the<JavaPluginExtension>().sourceSets

val smokeTest by tasks.registering(Test::class) {
    description = "Runs smoke tests against a running stack."
    group = "verification"
    testClassesDirs = testSourceSets["test"].output.classesDirs
    classpath = testSourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("*SmokeTest")
        isFailOnNoMatchingTests = false
    }
}
