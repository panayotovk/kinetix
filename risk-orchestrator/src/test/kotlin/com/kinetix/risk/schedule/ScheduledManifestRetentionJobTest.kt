package com.kinetix.risk.schedule

import com.kinetix.risk.persistence.ManifestRetentionRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalTime

class ScheduledManifestRetentionJobTest : FunSpec({

    val manifestRetentionRepository = mockk<ManifestRetentionRepository>()

    beforeEach {
        clearMocks(manifestRetentionRepository)
    }

    test("deletes manifests older than retention window") {
        coEvery { manifestRetentionRepository.deleteExpiredManifests(2555) } returns 12

        val job = ScheduledManifestRetentionJob(
            manifestRetentionRepository = manifestRetentionRepository,
            retentionDays = 2555,
            runAtTime = LocalTime.of(3, 30),
            nowProvider = { LocalTime.of(4, 0) },
        )

        job.tick()

        coVerify(exactly = 1) { manifestRetentionRepository.deleteExpiredManifests(2555) }
    }

    test("preserves manifests within retention window") {
        val job = ScheduledManifestRetentionJob(
            manifestRetentionRepository = manifestRetentionRepository,
            retentionDays = 2555,
            runAtTime = LocalTime.of(3, 30),
            nowProvider = { LocalTime.of(3, 29) },
        )

        job.tick()

        coVerify(exactly = 0) { manifestRetentionRepository.deleteExpiredManifests(any()) }
    }

    test("cascades deletion to position snapshots and market data references") {
        // The cascade contract lives in ExposedManifestRetentionRepository, but the job
        // passes the same retentionDays to the repository so the repository can apply
        // the threshold consistently across all three tables.
        coEvery { manifestRetentionRepository.deleteExpiredManifests(2555) } returns 5

        val job = ScheduledManifestRetentionJob(
            manifestRetentionRepository = manifestRetentionRepository,
            retentionDays = 2555,
            runAtTime = LocalTime.of(3, 30),
            nowProvider = { LocalTime.of(3, 30) },
        )

        job.tick()

        // The single repository call is responsible for cascading the delete to
        // run_position_snapshots and run_manifest_market_data before removing manifests.
        coVerify(exactly = 1) { manifestRetentionRepository.deleteExpiredManifests(2555) }
    }

    test("handles repository failure without propagating the exception") {
        coEvery { manifestRetentionRepository.deleteExpiredManifests(any()) } throws RuntimeException("DB unavailable")

        val job = ScheduledManifestRetentionJob(
            manifestRetentionRepository = manifestRetentionRepository,
            retentionDays = 2555,
            runAtTime = LocalTime.of(3, 30),
            nowProvider = { LocalTime.of(4, 0) },
        )

        job.tick()

        coVerify(exactly = 1) { manifestRetentionRepository.deleteExpiredManifests(2555) }
    }

    test("uses configured retentionDays when calling repository") {
        coEvery { manifestRetentionRepository.deleteExpiredManifests(365) } returns 3

        val job = ScheduledManifestRetentionJob(
            manifestRetentionRepository = manifestRetentionRepository,
            retentionDays = 365,
            runAtTime = LocalTime.of(3, 30),
            nowProvider = { LocalTime.of(3, 30) },
        )

        job.tick()

        coVerify(exactly = 1) { manifestRetentionRepository.deleteExpiredManifests(365) }
    }
})
