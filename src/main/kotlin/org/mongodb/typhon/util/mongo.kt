package org.mongodb.typhon.util

import com.mongodb.MongoException
import com.mongodb.TransactionOptions
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.ClientSession
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.*
import org.bson.conversions.Bson
import org.bson.types.Binary
import org.mongodb.typhon.core.CollectionStats
import org.mongodb.typhon.core.TestConfig

private val logger = KotlinLogging.logger { }

fun <T : Any> MongoClient.transaction(
    txOptions: TransactionOptions = TransactionOptions.builder().build(),
    txBody: (ClientSession) -> T
): T {
    startSession().use { session ->
        return session.withTransaction({
            txBody(session)
        }, txOptions)
    }
}

fun <T : Any> MongoClient.measureTransaction(
    txOptions: TransactionOptions = TransactionOptions.builder().build(),
    txBody: (ClientSession) -> T
): Pair<T, Int> {
    var retries = 0
    while (true) {
        try {
            startSession().use { session ->
                session.startTransaction(txOptions)
                val retVal = txBody(session)
                session.commitTransactionWithRetry()
                return retVal to retries
            }
        } catch (e: MongoException) {
            if (e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                retries++
                continue
            } else {
                throw e
            }
        }
    }
}

fun ClientSession.commitTransactionWithRetry() {
    while (true) {
        try {
            commitTransaction()
            break
        } catch (e: MongoException) {
            if (e.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)) {
                continue
            } else {
                throw e
            }
        }
    }
}

fun MongoCollection<*>.stats(): CollectionStats =
    this.withDocumentClass<CollectionStats>().aggregate(
        listOf(
            collStats(),
            Aggregates.project(
                Projections.fields(
                    Projections.computed("dataUncompressed", "\$storageStats.size"),
                    Projections.computed("dataCompressed", "\$storageStats.storageSize"),
                    Projections.computed("docCount", "\$storageStats.count"),
                    Projections.computed("avgDocSizeBytes", "\$storageStats.avgObjSize"),
                    Projections.computed("totalSize", "\$storageStats.totalSize"),
                    Projections.computed("indexSizes", "\$storageStats.indexSizes"),
                    Projections.computed("indexSize", "\$storageStats.totalIndexSize"),
                )
            )
        )
    ).first()

private fun collStats(): Bson = Document("\$collStats", mapOf("storageStats" to emptyMap<String, String>()))

fun MongoClient.shardCollection(ns: String, shardKey: Map<String, Int>) {
    getDatabase("admin").runCommand(
        Document(
            mapOf(
                "shardCollection" to ns,
                "key" to shardKey
            )
        )
    )
}

fun MongoClient.split(ns: String, parentId: ByteArray, name: String) {
    getDatabase("admin").runCommand(
        Document(
            mapOf(
                "split" to ns,
                "middle" to Document(
                    "key", mapOf(
                        "parentId" to Binary(parentId),
                        "name" to name
                    )
                )
            )
        )
    )
}

fun MongoClient.listShards() =
    getDatabase("admin").runCommand(
        Document(
            mapOf(
                "listShards" to 1
            )
        )
    ).getList("shards", Document::class.java).map { it.getString("_id") }

fun MongoClient.moveChunk(ns: String, parentId: ByteArray, name: String, shard: String) {
    getDatabase("admin").runCommand(
        Document(
            mapOf(
                "moveChunk" to ns,
                "find" to Document(
                    "key", mapOf(
                        "parentId" to Binary(parentId),
                        "name" to name
                    )
                ),
                "to" to shard
            )
        )
    )
}

fun <T : Any> MongoCollection<T>.applyConfig(config: TestConfig): MongoCollection<T> =
    withReadConcern(config.readConcern)
        .withReadPreference(config.readPreference)
        .withWriteConcern(config.writeConcern)

object Expressions {
    fun expr(a: BsonDocument) =
        BsonDocument("\$expr", a)

    fun lt(a: BsonValue, b: BsonValue) =
        BsonDocument("\$lt", BsonArray(mutableListOf(a, b)))

    fun mod(a: BsonValue, b: BsonValue) =
        BsonDocument("\$mod", BsonArray(mutableListOf(a, b)))

    fun abs(a: BsonValue) =
        BsonDocument("\$abs", a)

    fun toHashedIndexKey(a: String) =
        BsonDocument("\$toHashedIndexKey", BsonString(a))

    fun hashFilter(acceptRatio: Double) =
        expr(
            lt(
                mod(abs(toHashedIndexKey("\$name")), 100_000.toBson()),
                acceptRatio.times(100_000).toBson()
            )
        )
}

fun Long.toBson() = BsonInt64(this)
fun String.toBson() = BsonString(this)
fun Int.toBson() = BsonInt32(this)
fun Double.toBson() = BsonDouble(this)

private val matchObj = match(
    and(
        eq("orderId", 0),
        gt("key", BsonMinKey())
    )
)