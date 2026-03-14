package com.kinetix.risk.cache

import com.kinetix.risk.model.ChangeMagnitude
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection

class RedisQuantDiffCache(
    private val connection: StatefulRedisConnection<String, String>,
    private val ttlSeconds: Long = 86_400L, // 24 hours
) : QuantDiffCache {

    private val sync = connection.sync()

    override fun get(baseHash: String, targetHash: String): ChangeMagnitude? {
        val value = sync.get(keyFor(baseHash, targetHash)) ?: return null
        return try {
            ChangeMagnitude.valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override fun put(baseHash: String, targetHash: String, magnitude: ChangeMagnitude) {
        sync.set(keyFor(baseHash, targetHash), magnitude.name, SetArgs().ex(ttlSeconds))
    }

    private fun keyFor(baseHash: String, targetHash: String): String =
        "quant-diff:$baseHash:$targetHash"
}
