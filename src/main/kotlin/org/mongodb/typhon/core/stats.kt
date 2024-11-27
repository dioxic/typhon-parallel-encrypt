package org.mongodb.typhon.core

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.MongoClient
import kotlinx.datetime.Instant
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bson.Document
import org.mongodb.typhon.serialization.ByteSizeSerializer
import org.mongodb.typhon.serialization.DurationSerializer
import org.mongodb.typhon.serialization.InstantSerializer
import org.mongodb.typhon.util.toMap
import kotlin.math.max
import kotlin.time.Duration

@Serializable
data class Stats(
    @Contextual val timestamp: Instant,
    val group: String,
    val executions: Int,
    val throughputs: Map<String, Int>? = null,
    val collStats: CollectionStats? = null,
    val keyCache: String? = null,
    @Contextual val p50: Duration,
    @Contextual val p95: Duration,
    @Contextual val p99: Duration,
    @Contextual val max: Duration,
)

fun interface StatsCollector {
    fun accept(stats: Stats)
}

val simpleConsoleStatsCollector = StatsCollector {
    println(it)
}

val csvConsoleStatsCollector = StatsCollector {
    with(it) {
        println("$group, $executions, $p50, $p95, $p99, $max")
    }
}

@OptIn(FormatStringsInDatetimeFormats::class)
val prettyStatsCollector = object : StatsCollector {
    private val ignoreKeys = listOf("avgDocSizeBytes", "indexSizes", "dataUncompressed", "dataCompressed")
    private val padding = 2
    private var counter = 0
    private lateinit var lastColumns: Map<String, Int>

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(DurationSerializer)
            contextual(InstantSerializer)
            contextual(ByteSizeSerializer)
        }
    }

    private fun Stats.unwind(): Map<String, Any?> =
        toMap(this, json)
            .flatMap { (k, v) ->
                when (v) {
                    is Map<*, *> -> v.map { it.key.toString() to it.value }
                    else -> listOf(k to v)
                }
            }.filterNot { (k, v) ->
                k in ignoreKeys
            }.associate { it }

    private fun columnLengths(stats: Stats): Map<String, Int> =
        stats.unwind().mapValues { (k, v) ->
            max(v.toString().length, k.length)
        }

    override fun accept(stats: Stats) {
        val statsMap = stats.unwind()
        if (counter % 10 == 0 || statsMap.size != lastColumns.size) {
            lastColumns = columnLengths(stats)
            println(
                lastColumns
                .map { (column, len) -> column.padEnd(len + padding) }
                .joinToString(""))
            println("".padStart(lastColumns.values.sum() + lastColumns.size * (padding), '-'))
            counter = 0
        }
        println(
            lastColumns
            .map { (column, len) -> statsMap[column].toString().padEnd(len + padding) }
            .joinToString("")
        )
        counter++
    }
}

/**
 * Prints stats to console and persist to database
 */
inline fun <reified G : Generator> createDbStatsCollector(
    testDefinition: TestDefinition<G>,
    client: MongoClient
): StatsCollector {
    val db = client.getDatabase("results")

    val testIteration = db.getCollection<Document>("iterations")
        .findOneAndUpdate(
            Filters.eq("_id", testDefinition.name),
            Updates.inc("iteration", 1),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        )?.get("iteration")
    val definitionDocument = Document.parse(testDefinition.toJson()).also {
        it["_id"] = "${testDefinition.name}_$testIteration"
    }
    val testId = db.getCollection<Document>("tests").insertOne(definitionDocument).insertedId!!
    val statsCollection = db.getCollection<Document>("results")

    return StatsCollector { stats ->
        prettyStatsCollector.accept(stats)
        val elapsed = (stats.timestamp - testDefinition.startTime).inWholeSeconds
        if (elapsed > 1) {
            statsCollection.insertOne(Document(buildMap {
                put("testId", testId)
                put("ts", stats.timestamp.toJavaInstant())
                put("iteration", testIteration)
                put("elapsed", elapsed)
                put("group", stats.group)
                put("executions", stats.executions)
                put("latencies", buildMap {
                    put("p50", stats.p50.inFractionalMilliseconds)
                    put("p95", stats.p95.inFractionalMilliseconds)
                    put("p99", stats.p99.inFractionalMilliseconds)
                    put("max", stats.max.inFractionalMilliseconds)
                })
                put("throughputs", buildMap {
                    stats.throughputs?.forEach { (k, v) ->
                        put(k, v)
                    }
                })
                stats.collStats?.apply {
                    put("collStats", buildMap {
                        put("dataCompressed", dataCompressed)
                        put("dataUncompressed", dataUncompressed)
                        put("docCount", docCount)
                        put("totalSize", totalSize)
                        put("totalIndexSize", indexSize)
                        put("avgDocSizeBytes", avgDocSizeBytes)
                    })
                }
            }))
        }
    }
}

val Duration.inFractionalMilliseconds
    get() = this.inWholeMicroseconds.div(1000.0)