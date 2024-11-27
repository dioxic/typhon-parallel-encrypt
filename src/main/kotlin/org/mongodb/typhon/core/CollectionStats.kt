package org.mongodb.typhon.core

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class CollectionStats(
    @Contextual val dataUncompressed: ByteSize,
    @Contextual val dataCompressed: ByteSize,
    val docCount: Long,
    @Contextual val avgDocSizeBytes: ByteSize?,
    @Contextual val totalSize: ByteSize,
    val indexSizes: Map<String, @Contextual ByteSize>,
    @Contextual val indexSize: ByteSize,
)

