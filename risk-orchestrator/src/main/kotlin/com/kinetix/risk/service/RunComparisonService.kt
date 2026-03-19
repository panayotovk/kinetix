package com.kinetix.risk.service

import com.kinetix.risk.mapper.toRunSnapshot
import com.kinetix.risk.model.ComparisonType
import com.kinetix.risk.model.InputChangeSummary
import com.kinetix.risk.model.RunComparison
import com.kinetix.risk.model.RunSnapshot
import java.time.LocalDate
import java.util.UUID

class RunComparisonService(
    private val jobRecorder: ValuationJobRecorder,
    private val differ: SnapshotDiffer,
    private val manifestRepo: RunManifestRepository? = null,
    private val inputChangeDiffer: InputChangeDiffer? = null,
) {

    suspend fun compareByJobIds(baseJobId: UUID, targetJobId: UUID): RunComparison {
        val baseJob = jobRecorder.findByJobId(baseJobId)
            ?: throw IllegalArgumentException("Base job not found: $baseJobId")
        val targetJob = jobRecorder.findByJobId(targetJobId)
            ?: throw IllegalArgumentException("Target job not found: $targetJobId")

        val baseModelVersion = resolveModelVersion(baseJob.manifestId)
        val targetModelVersion = resolveModelVersion(targetJob.manifestId)
        val base = baseJob.toRunSnapshot("Base", baseModelVersion)
        val target = targetJob.toRunSnapshot("Target", targetModelVersion)
        val inputChanges = loadInputChanges(baseJob.manifestId, targetJob.manifestId)
        return compareSnapshots(base, target, ComparisonType.RUN_OVER_RUN, baseJob.bookId, inputChanges)
    }

    suspend fun compareDayOverDay(
        bookId: String,
        targetDate: LocalDate,
        baseDate: LocalDate,
    ): RunComparison {
        val baseJob = jobRecorder.findOfficialEodByDate(bookId, baseDate)
            ?: jobRecorder.findLatestCompletedByDate(bookId, baseDate)
            ?: throw IllegalArgumentException("No completed job for portfolio $bookId on $baseDate")
        val targetJob = jobRecorder.findOfficialEodByDate(bookId, targetDate)
            ?: jobRecorder.findLatestCompletedByDate(bookId, targetDate)
            ?: throw IllegalArgumentException("No completed job for portfolio $bookId on $targetDate")

        val baseModelVersion = resolveModelVersion(baseJob.manifestId)
        val targetModelVersion = resolveModelVersion(targetJob.manifestId)
        val baseLabel = if (baseJob.runLabel == com.kinetix.risk.model.RunLabel.OFFICIAL_EOD) "Official EOD $baseDate" else "$baseDate"
        val targetLabel = if (targetJob.runLabel == com.kinetix.risk.model.RunLabel.OFFICIAL_EOD) "Official EOD $targetDate" else "$targetDate"
        val base = baseJob.toRunSnapshot(baseLabel, baseModelVersion)
        val target = targetJob.toRunSnapshot(targetLabel, targetModelVersion)
        val inputChanges = loadInputChanges(baseJob.manifestId, targetJob.manifestId)
        return compareSnapshots(base, target, ComparisonType.RUN_OVER_RUN, bookId, inputChanges)
    }

    private suspend fun resolveModelVersion(manifestId: UUID?): String? {
        if (manifestId == null || manifestRepo == null) return null
        return manifestRepo.findByManifestId(manifestId)?.modelVersion?.ifEmpty { null }
    }

    private suspend fun loadInputChanges(baseManifestId: UUID?, targetManifestId: UUID?): InputChangeSummary? {
        val repo = manifestRepo ?: return null
        val differ = inputChangeDiffer ?: return null
        if (baseManifestId == null || targetManifestId == null) return null
        val baseManifest = repo.findByManifestId(baseManifestId) ?: return null
        val targetManifest = repo.findByManifestId(targetManifestId) ?: return null

        val basePositions = if (baseManifest.positionDigest != targetManifest.positionDigest)
            repo.findPositionSnapshot(baseManifest.manifestId) else emptyList()
        val targetPositions = if (baseManifest.positionDigest != targetManifest.positionDigest)
            repo.findPositionSnapshot(targetManifest.manifestId) else emptyList()
        val baseRefs = if (baseManifest.marketDataDigest != targetManifest.marketDataDigest)
            repo.findMarketDataRefs(baseManifest.manifestId) else emptyList()
        val targetRefs = if (baseManifest.marketDataDigest != targetManifest.marketDataDigest)
            repo.findMarketDataRefs(targetManifest.manifestId) else emptyList()

        val summary = differ.computeInputChanges(baseManifest, targetManifest, basePositions, targetPositions, baseRefs, targetRefs)
        return summary.copy(
            baseManifestId = baseManifestId.toString(),
            targetManifestId = targetManifestId.toString(),
        )
    }

    fun compareSnapshots(
        base: RunSnapshot,
        target: RunSnapshot,
        type: ComparisonType,
        bookId: String,
        inputChanges: InputChangeSummary? = null,
    ): RunComparison = RunComparison(
        comparisonId = UUID.randomUUID(),
        type = type,
        bookId = bookId,
        baseRun = base,
        targetRun = target,
        portfolioDiff = differ.computePortfolioDiff(base, target),
        componentDiffs = differ.matchComponents(base.componentBreakdowns, target.componentBreakdowns),
        positionDiffs = differ.matchPositions(base.positionRisks, target.positionRisks),
        parameterDiffs = differ.diffParameters(base, target),
        attribution = null,
        inputChanges = inputChanges,
    )
}
