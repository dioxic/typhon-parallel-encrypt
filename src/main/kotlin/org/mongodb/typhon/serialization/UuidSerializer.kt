package org.mongodb.typhon.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import org.bson.BsonBinary
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class, ExperimentalSerializationApi::class)
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.BYTE)

    override fun serialize(encoder: Encoder, value: Uuid) {
        when (encoder) {
            is BsonEncoder -> encoder.encodeBsonValue(BsonBinary(4, value.toByteArray()))
            is JsonEncoder -> encoder.encodeString(value.toHexString())
            else -> throw SerializationException("UUID is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return when (decoder) {
            is BsonDecoder -> decoder.decodeBsonValue().asBinary().asUuid().toKotlinUuid()
            is JsonDecoder -> Uuid.parseHex(decoder.decodeString())

            else -> throw SerializationException("UUID is not supported by ${decoder::class}")
        }
    }
}