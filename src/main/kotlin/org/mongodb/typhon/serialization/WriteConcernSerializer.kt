package org.mongodb.typhon.serialization

import com.mongodb.WriteConcern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WriteConcernSerializer : KSerializer<WriteConcern> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ReadConcernSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: WriteConcern) {
        encoder.encodeString(value.wObject?.toString() ?: "UNKNOWN")
    }

    override fun deserialize(decoder: Decoder): WriteConcern {
        return WriteConcern(decoder.decodeString())
    }
}