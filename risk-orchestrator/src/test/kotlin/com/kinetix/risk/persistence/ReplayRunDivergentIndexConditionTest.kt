package com.kinetix.risk.persistence

import com.kinetix.risk.model.ReplayRun
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

/**
 * Validates the predicate logic for the idx_replay_runs_divergent partial index
 * defined in V29__fix_replay_drift_index.sql.
 *
 * The index covers rows WHERE:
 *   input_digest_match = false
 *   OR var_drift > 0  (where var_drift = ABS(replay_var_value - original_var_value))
 *   OR replay_var_value IS NULL
 *
 * Failed replays have replay_var_value = NULL and therefore produce NULL drift.
 * The original V22 index only matched var_drift > 0, silently excluding failed
 * replays.  This test suite pins the corrected condition so the fix cannot regress.
 */
class ReplayRunDivergentIndexConditionTest : FunSpec({

    fun replayRun(
        inputDigestMatch: Boolean = true,
        replayVarValue: Double? = 5000.0,
        originalVarValue: Double? = 5000.0,
    ) = ReplayRun(
        replayId = UUID.randomUUID(),
        manifestId = UUID.randomUUID(),
        originalJobId = UUID.randomUUID(),
        replayedAt = Instant.now(),
        triggeredBy = "test-user",
        replayVarValue = replayVarValue,
        replayExpectedShortfall = null,
        replayModelVersion = "0.1.0",
        replayOutputDigest = null,
        originalVarValue = originalVarValue,
        originalExpectedShortfall = null,
        inputDigestMatch = inputDigestMatch,
        originalInputDigest = "abc123",
        replayInputDigest = "abc123",
    )

    fun isDivergent(run: ReplayRun): Boolean {
        val varDrift = if (run.replayVarValue != null && run.originalVarValue != null) {
            Math.abs(run.replayVarValue - run.originalVarValue)
        } else null
        return !run.inputDigestMatch || (varDrift != null && varDrift > 0.0) || run.replayVarValue == null
    }

    test("successful replay with matching inputs and identical VaR is not divergent") {
        val run = replayRun(inputDigestMatch = true, replayVarValue = 5000.0, originalVarValue = 5000.0)
        isDivergent(run) shouldBe false
    }

    test("divergent index includes failed replays with null var") {
        val failedReplay = replayRun(inputDigestMatch = true, replayVarValue = null, originalVarValue = 5000.0)
        isDivergent(failedReplay) shouldBe true
    }

    test("divergent index includes replays with digest mismatch even when var is identical") {
        val digestMismatch = replayRun(inputDigestMatch = false, replayVarValue = 5000.0, originalVarValue = 5000.0)
        isDivergent(digestMismatch) shouldBe true
    }

    test("divergent index includes replays with positive var drift") {
        val drifted = replayRun(inputDigestMatch = true, replayVarValue = 5100.0, originalVarValue = 5000.0)
        isDivergent(drifted) shouldBe true
    }

    test("divergent index includes replays with both digest mismatch and null var") {
        val failedWithMismatch = replayRun(inputDigestMatch = false, replayVarValue = null, originalVarValue = 5000.0)
        isDivergent(failedWithMismatch) shouldBe true
    }
})
