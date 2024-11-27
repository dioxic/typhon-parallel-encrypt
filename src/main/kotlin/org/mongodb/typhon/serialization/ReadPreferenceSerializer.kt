package org.mongodb.typhon.serialization

import com.mongodb.ReadPreference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ReadPreferenceSerializer : KSerializer<ReadPreference> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReadPreferenceSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ReadPreference) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): ReadPreference {
        return when (decoder.decodeString()) {
            "primary" -> ReadPreference.primary()
            "secondary" -> ReadPreference.secondary()
            "primaryPreferred" -> ReadPreference.primaryPreferred()
            "nearest" -> ReadPreference.nearest()
            else -> throw IllegalArgumentException("Unknown read preference!")
        }
    }
}