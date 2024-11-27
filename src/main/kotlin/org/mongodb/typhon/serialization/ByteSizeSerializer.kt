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
import org.mongodb.typhon.core.ByteSize
import org.mongodb.typhon.util.dataVolumePretty

@OptIn(ExperimentalSerializationApi::class)
object ByteSizeSerializer : KSerializer<ByteSize> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteSizeSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ByteSize) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(dataVolumePretty(value))
            else -> throw SerializationException("ByteSize is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): ByteSize {
        throw UnsupportedOperationException()
    }
}