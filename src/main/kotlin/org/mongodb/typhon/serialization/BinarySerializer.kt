package org.mongodb.typhon.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonBinary
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import org.bson.types.Binary

@OptIn(ExperimentalSerializationApi::class)
object BinarySerializer : KSerializer<Binary> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BinarySerializer", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: Binary) {
        when (encoder) {
            is BsonEncoder -> encoder.encodeBsonValue(BsonBinary(value.type, value.data))
            else -> throw SerializationException("Binary is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Binary {
        return when (decoder) {
            is BsonDecoder -> {
                with(decoder.decodeBsonValue().asBinary()) {
                    Binary(type, data)
                }
            }

            else -> throw SerializationException("Binary is not supported by ${decoder::class}")
        }
    }
}