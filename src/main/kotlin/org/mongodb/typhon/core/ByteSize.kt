package org.mongodb.typhon.core

import kotlinx.serialization.Serializable
import org.mongodb.typhon.util.dataVolumePretty

@JvmInline
@Serializable
value class ByteSize(private val byteSize: Long) {
    operator fun compareTo(other: ByteSize): Int = byteSize.compareTo(other.bytes)

    operator fun plus(other: ByteSize): ByteSize = ByteSize(byteSize + other.byteSize)

    operator fun plus(other: Long): ByteSize = ByteSize(byteSize + other)

    operator fun plus(other: Int): ByteSize = ByteSize(byteSize + other.toLong())

    override fun toString(): String {
        return dataVolumePretty(this)
    }

    val bytes: Long
        get() = byteSize

    val kb: Double
        get() = byteSize / 1_000.0

    val mb: Double
        get() = byteSize / 1_000_000.0

    val gb: Double
        get() = byteSize / 1_000_000_000.0

    fun toDocCount(avgDocSizeBytes: Int = 1262) = (bytes / avgDocSizeBytes).toInt()

}