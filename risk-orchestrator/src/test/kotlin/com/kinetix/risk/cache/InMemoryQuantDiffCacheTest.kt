package com.kinetix.risk.cache

import com.kinetix.risk.model.ChangeMagnitude
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InMemoryQuantDiffCacheTest : FunSpec({

    test("returns null for uncached pair") {
        val cache = InMemoryQuantDiffCache()
        cache.get("hash-a", "hash-b") shouldBe null
    }

    test("returns cached magnitude after put") {
        val cache = InMemoryQuantDiffCache()
        cache.put("hash-a", "hash-b", ChangeMagnitude.LARGE)
        cache.get("hash-a", "hash-b") shouldBe ChangeMagnitude.LARGE
    }

    test("treats reversed hash order as different key") {
        val cache = InMemoryQuantDiffCache()
        cache.put("hash-a", "hash-b", ChangeMagnitude.LARGE)
        cache.get("hash-b", "hash-a") shouldBe null
    }

    test("overwrites existing entry") {
        val cache = InMemoryQuantDiffCache()
        cache.put("hash-a", "hash-b", ChangeMagnitude.SMALL)
        cache.put("hash-a", "hash-b", ChangeMagnitude.LARGE)
        cache.get("hash-a", "hash-b") shouldBe ChangeMagnitude.LARGE
    }
})
