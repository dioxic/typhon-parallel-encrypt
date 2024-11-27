package org.mongodb.typhon.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonTimestamp
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder

@OptIn(ExperimentalSerializationApi::class)
object BsonTimestampSerializer : KSerializer<BsonTimestamp> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BsonTimestamp", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: BsonTimestamp) {
        when (encoder) {
            is BsonEncoder -> encoder.encodeBsonValue(value)
            else -> throw SerializationException("BsonTimestamp is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): BsonTimestamp {
        return when (decoder) {
            is BsonDecoder -> {
                decoder.decodeBsonValue().asTimestamp()
            }

            else -> throw SerializationException("BsonTimestamp is not supported by ${decoder::class}")
        }
    }
}