package org.mongodb.typhon.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlin.time.Duration
import kotlin.time.DurationUnit

@OptIn(ExperimentalSerializationApi::class)
object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Duration) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.toString(DurationUnit.MILLISECONDS, 1))
            else -> throw SerializationException("Duration is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Duration {
        throw UnsupportedOperationException()
    }
}