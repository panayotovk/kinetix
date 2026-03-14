package com.kinetix.risk.cache

import com.kinetix.risk.model.ChangeMagnitude
import java.util.concurrent.ConcurrentHashMap

class InMemoryQuantDiffCache : QuantDiffCache {

    private val cache = ConcurrentHashMap<String, ChangeMagnitude>()

    override fun get(baseHash: String, targetHash: String): ChangeMagnitude? =
        cache[keyFor(baseHash, targetHash)]

    override fun put(baseHash: String, targetHash: String, magnitude: ChangeMagnitude) {
        cache[keyFor(baseHash, targetHash)] = magnitude
    }

    private fun keyFor(baseHash: String, targetHash: String): String =
        "quant-diff:$baseHash:$targetHash"
}
