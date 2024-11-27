package org.mongodb.typhon.core

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import kotlinx.serialization.Serializable
import org.mongodb.typhon.serialization.ReadConcernSerializer
import org.mongodb.typhon.serialization.ReadPreferenceSerializer
import org.mongodb.typhon.serialization.WriteConcernSerializer

@Serializable
data class TestConfig(
    val limit: Int,
    val timeoutSeconds: Int? = null,
    val workers: Int? = null,
    val ordered: Boolean? = null,
    val batchSize: Int? = null,
    val startOffset: Int = 0,
    val statsCollectionIntervalSeconds: Int = 1,
    @Serializable(with = ReadConcernSerializer::class)
    val readConcern: ReadConcern,
    @Serializable(with = WriteConcernSerializer::class)
    val writeConcern: WriteConcern,
    @Serializable(with = ReadPreferenceSerializer::class)
    val readPreference: ReadPreference,
)

