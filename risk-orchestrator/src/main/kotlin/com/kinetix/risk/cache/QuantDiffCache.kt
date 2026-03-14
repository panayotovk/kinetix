package com.kinetix.risk.cache

import com.kinetix.risk.model.ChangeMagnitude

interface QuantDiffCache {
    fun get(baseHash: String, targetHash: String): ChangeMagnitude?
    fun put(baseHash: String, targetHash: String, magnitude: ChangeMagnitude)
}
