package org.mongodb.typhon.serialization

import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder

@OptIn(FormatStringsInDatetimeFormats::class)
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantSerializer", PrimitiveKind.STRING)
    private val dateFormat = DateTimeComponents.Format {
        byUnicodePattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.format(dateFormat))
            else -> throw SerializationException("Instant is not supported by ${encoder::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Instant {
        throw UnsupportedOperationException()
    }
}