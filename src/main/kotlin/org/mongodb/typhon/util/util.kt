package org.mongodb.typhon.util


import com.mongodb.TransactionOptions
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.InsertManyOptions
import kotlinx.serialization.json.*
import org.bson.types.Binary
import org.mongodb.typhon.core.ByteSize
import org.mongodb.typhon.core.TestConfig
import java.security.MessageDigest
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.random.Random

private val characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

fun Random.nextString(len: Int = 10) =
    buildString(len) {
        repeat(len) {
            append(characters[nextInt(characters.size)])
        }
    }

fun Random.nextBoolean(chance: Double) =
    Random.nextDouble(1.0) <= chance

private val md5Digest = ThreadLocal.withInitial { MessageDigest.getInstance("MD5") }

fun Int.toByteArray(): ByteArray =
    byteArrayOf(
        shr(24).toByte(),
        shr(16).toByte(),
        shr(8).toByte(),
        toByte(),
    )

fun Long.toByteArray(): ByteArray =
    byteArrayOf(
        shr(56).toByte(),
        shr(48).toByte(),
        shr(40).toByte(),
        shr(32).toByte(),
        shr(24).toByte(),
        shr(16).toByte(),
        shr(8).toByte(),
        toByte(),
    )

fun ByteArray.toInt(): Int =
    get(0).toInt().and(0xFF).shl(24)
        .or(get(1).toInt().and(0xFF).shl(16))
        .or(get(2).toInt().and(0xFF).shl(8))
        .or(get(3).toInt().and(0xFF))

fun Int.hashMod(mod: Int): Int = abs(hash().toInt()) % mod

fun ByteArray.hash(): ByteArray =
    md5Digest.get().digest(this)

fun Int.hash(): ByteArray =
    toByteArray().hash()

fun Long.hash(): ByteArray =
    toByteArray().hash()

/**
 * Returns 1 for true and 0 for false
 */
fun Boolean.toInt() = if (this) 1 else 0

/**
 * @param size size in bytes
 */
fun Random.nextBinary(size: Int) =
    Binary(Random.nextBytes(size))

inline fun <reified T> toMap(obj: T, json: Json = Json): Map<String, Any?> {
    return jsonObjectToMap(json.encodeToJsonElement(obj).jsonObject)
}

fun jsonObjectToMap(element: JsonObject): Map<String, Any?> {
    return element.entries.associate {
        it.key to extractValue(it.value)
    }
}

private fun extractValue(element: JsonElement): Any? {
    return when (element) {
        is JsonNull -> null
        is JsonPrimitive -> element.content
        is JsonArray -> element.map { extractValue(it) }
        is JsonObject -> jsonObjectToMap(element)
    }
}

fun dataVolumePretty(byteSize: ByteSize): String {
    var b: Long = byteSize.bytes
    if (-1000 < b && b < 1000) {
        return "$b bytes"
    }
    val ci = StringCharacterIterator("kMGTPE")
    while (b <= -999_950 || b >= 999_950) {
        b /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", b / 1000.0, ci.current())
}

fun TestConfig.toInsertManyOptions(): InsertManyOptions {
    requireNotNull(ordered) { "'ordered' config not set" }
    return InsertManyOptions().ordered(ordered)
}

fun TestConfig.toBulkWriteOptions(): BulkWriteOptions {
    requireNotNull(ordered) { "'ordered' config not set" }
    return BulkWriteOptions().ordered(ordered)
}

fun TestConfig.toTransactionOptions(): TransactionOptions =
    TransactionOptions.builder()
        .readConcern(readConcern)
        .writeConcern(writeConcern)
        .readPreference(readPreference)
        .build()