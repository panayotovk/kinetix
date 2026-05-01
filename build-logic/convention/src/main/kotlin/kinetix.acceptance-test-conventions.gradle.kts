tasks.register("verifyAcceptanceTestCompliance") {
    group = "verification"
    description = "Fails if acceptance tests use mocked repositories/stubs/clients or BehaviorSpec."
    notCompatibleWithConfigurationCache("Scans source files at execution time")
    inputs.files(fileTree("src/test").matching { include("**/*AcceptanceTest.kt") })
    doLast {
        val forbiddenPatterns = listOf(
            Regex("""mockk<.*Repository>"""),
            Regex("""mockk<.*CoroutineStub>"""),
            Regex("""mockk<.*ServiceClient>"""),
            Regex("""InMemory[A-Z][A-Za-z]*Repository\("""),
            Regex("""class .* : BehaviorSpec"""),
        )
        val violations = mutableListOf<String>()
        inputs.files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                forbiddenPatterns.forEach { pattern ->
                    if (pattern.containsMatchIn(line)) {
                        violations += "${file.relativeTo(projectDir)}:${index + 1}: $line"
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Acceptance test compliance violations found (${violations.size}):")
                    appendLine("  Forbidden patterns: mockk<*Repository>, mockk<*CoroutineStub>, mockk<*ServiceClient>,")
                    appendLine("  InMemory*Repository(...), class * : BehaviorSpec")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                }
            )
        }
    }
}
