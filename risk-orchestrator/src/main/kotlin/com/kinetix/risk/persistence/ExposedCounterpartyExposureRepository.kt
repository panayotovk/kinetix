package com.kinetix.risk.persistence

import com.kinetix.risk.model.CounterpartyExposureSnapshot
import com.kinetix.risk.model.ExposureAtTenor
import com.kinetix.risk.model.NettingSetExposure
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val repoJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class NettingSetExposureRecord(
    val nettingSetId: String,
    val agreementType: String,
    val netExposure: Double,
    val peakPfe: Double,
)

class ExposedCounterpartyExposureRepository(
    private val db: Database? = null,
) : CounterpartyExposureRepository {

    override suspend fun save(snapshot: CounterpartyExposureSnapshot): CounterpartyExposureSnapshot =
        newSuspendedTransaction(db = db) {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val pfeJson = buildJsonArray {
                snapshot.pfeProfile.forEach { tenor ->
                    add(buildJsonObject {
                        put("tenor", tenor.tenor)
                        put("tenorYears", tenor.tenorYears)
                        put("expectedExposure", tenor.expectedExposure)
                        put("pfe95", tenor.pfe95)
                        put("pfe99", tenor.pfe99)
                    })
                }
            }.toString()

            val nettingSetJson = repoJson.encodeToString(
                (snapshot.nettingSetExposures ?: emptyList()).map {
                    NettingSetExposureRecord(
                        nettingSetId = it.nettingSetId,
                        agreementType = it.agreementType,
                        netExposure = it.netExposure,
                        peakPfe = it.peakPfe,
                    )
                }
            )
            val wwrFlagsJson = repoJson.encodeToString(snapshot.wrongWayRiskFlags ?: emptyList<String>())

            val result = CounterpartyExposureHistoryTable.insert {
                it[counterpartyId] = snapshot.counterpartyId
                it[calculatedAt] = now
                it[pfeProfileJson] = pfeJson
                it[nettingSetExposuresJson] = nettingSetJson
                it[wrongWayRiskFlagsJson] = wwrFlagsJson
                it[currentNetExposure] = BigDecimal.valueOf(snapshot.currentNetExposure)
                it[peakPfe] = BigDecimal.valueOf(snapshot.peakPfe)
                it[cva] = snapshot.cva?.let { v -> BigDecimal.valueOf(v) }
                it[cvaEstimated] = snapshot.cvaEstimated
                it[currency] = snapshot.currency
                it[collateralHeld] = BigDecimal.valueOf(snapshot.collateralHeld)
                it[collateralPosted] = BigDecimal.valueOf(snapshot.collateralPosted)
                it[netNetExposure] = snapshot.netNetExposure?.let { v -> BigDecimal.valueOf(v) }
            }
            snapshot.copy(
                id = result[CounterpartyExposureHistoryTable.id],
                calculatedAt = now.toInstant(),
            )
        }

    override suspend fun findLatestByCounterpartyId(counterpartyId: String): CounterpartyExposureSnapshot? =
        newSuspendedTransaction(db = db) {
            CounterpartyExposureHistoryTable
                .selectAll()
                .where { CounterpartyExposureHistoryTable.counterpartyId eq counterpartyId }
                .orderBy(CounterpartyExposureHistoryTable.calculatedAt, SortOrder.DESC)
                .limit(1)
                .map { it.toSnapshot() }
                .firstOrNull()
        }

    override suspend fun findByCounterpartyId(counterpartyId: String, limit: Int): List<CounterpartyExposureSnapshot> =
        newSuspendedTransaction(db = db) {
            CounterpartyExposureHistoryTable
                .selectAll()
                .where { CounterpartyExposureHistoryTable.counterpartyId eq counterpartyId }
                .orderBy(CounterpartyExposureHistoryTable.calculatedAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toSnapshot() }
        }

    override suspend fun findLatestForAllCounterparties(): List<CounterpartyExposureSnapshot> =
        newSuspendedTransaction(db = db) {
            // Use a subquery approach: fetch all ordered by time, deduplicate in memory.
            // For production with many counterparties, a DISTINCT ON query would be preferable.
            CounterpartyExposureHistoryTable
                .selectAll()
                .orderBy(CounterpartyExposureHistoryTable.calculatedAt, SortOrder.DESC)
                .map { it.toSnapshot() }
                .distinctBy { it.counterpartyId }
        }

    private fun ResultRow.toSnapshot(): CounterpartyExposureSnapshot {
        val pfeJson = Json.parseToJsonElement(this[CounterpartyExposureHistoryTable.pfeProfileJson]).jsonArray
        val pfeProfile = pfeJson.map { element ->
            val obj = element.jsonObject
            ExposureAtTenor(
                tenor = obj["tenor"]!!.jsonPrimitive.content,
                tenorYears = obj["tenorYears"]!!.jsonPrimitive.double,
                expectedExposure = obj["expectedExposure"]!!.jsonPrimitive.double,
                pfe95 = obj["pfe95"]!!.jsonPrimitive.double,
                pfe99 = obj["pfe99"]!!.jsonPrimitive.double,
            )
        }

        val nettingSetExposures = try {
            repoJson.decodeFromString<List<NettingSetExposureRecord>>(
                this[CounterpartyExposureHistoryTable.nettingSetExposuresJson]
            ).map { NettingSetExposure(it.nettingSetId, it.agreementType, it.netExposure, it.peakPfe) }
        } catch (_: Exception) { emptyList() }

        val wrongWayRiskFlags = try {
            repoJson.decodeFromString<List<String>>(
                this[CounterpartyExposureHistoryTable.wrongWayRiskFlagsJson]
            )
        } catch (_: Exception) { emptyList() }

        return CounterpartyExposureSnapshot(
            id = this[CounterpartyExposureHistoryTable.id],
            counterpartyId = this[CounterpartyExposureHistoryTable.counterpartyId],
            calculatedAt = this[CounterpartyExposureHistoryTable.calculatedAt].toInstant(),
            pfeProfile = pfeProfile,
            currentNetExposure = this[CounterpartyExposureHistoryTable.currentNetExposure].toDouble(),
            peakPfe = this[CounterpartyExposureHistoryTable.peakPfe].toDouble(),
            cva = this[CounterpartyExposureHistoryTable.cva]?.toDouble(),
            cvaEstimated = this[CounterpartyExposureHistoryTable.cvaEstimated],
            currency = this[CounterpartyExposureHistoryTable.currency],
            nettingSetExposures = nettingSetExposures,
            collateralHeld = this[CounterpartyExposureHistoryTable.collateralHeld].toDouble(),
            collateralPosted = this[CounterpartyExposureHistoryTable.collateralPosted].toDouble(),
            netNetExposure = this[CounterpartyExposureHistoryTable.netNetExposure]?.toDouble(),
            wrongWayRiskFlags = wrongWayRiskFlags,
        )
    }
}
