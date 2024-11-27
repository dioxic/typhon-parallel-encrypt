package org.mongodb.typhon.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonDocument
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import kotlin.reflect.KClass

@ExperimentalSerializationApi
@OptIn(InternalSerializationApi::class)
abstract class BsonContentPolymorphicSerializer<T : Any>(private val baseClass: KClass<T>) : KSerializer<T> {
    /**
     * A descriptor for this set of content-based serializers.
     * By default, it uses the name composed of [baseClass] simple name,
     * kind is set to [PolymorphicKind.SEALED] and contains 0 elements.
     *
     * However, this descriptor can be overridden to achieve better representation of custom transformed BSON shape
     * for schema generating/introspection purposes.
     */

    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("BsonContentPolymorphicSerializer<${baseClass.simpleName}>", PolymorphicKind.SEALED)

    final override fun serialize(encoder: Encoder, value: T) {
        val actualSerializer =
            encoder.serializersModule.getPolymorphic(baseClass, value)
                ?: value::class.serializerOrNull()
                ?: throwSubtypeNotRegistered(value::class, baseClass)
        @Suppress("UNCHECKED_CAST")
        (actualSerializer as KSerializer<T>).serialize(encoder, value)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asBsonDecoder()
        val bsonValue = input.decodeBsonValue()
        require(bsonValue is BsonDocument)

        throw NotImplementedError()

        return (selectDeserializer(bsonValue) as KSerializer<T>).deserialize(decoder)
    }

    /**
     * Determines a particular strategy for deserialization by looking on a parsed BSON [value].
     */
    protected abstract fun selectDeserializer(value: BsonDocument): DeserializationStrategy<T>

    private fun throwSubtypeNotRegistered(subClass: KClass<*>, baseClass: KClass<*>): Nothing {
        val subClassName = subClass.simpleName ?: "$subClass"
        val scope = "in the scope of '${baseClass.simpleName}'"
        throw SerializationException(
            "Class '${subClassName}' is not registered for polymorphic serialization $scope.\n" +
                    "Mark the base class as 'sealed' or register the serializer explicitly."
        )
    }

}

@ExperimentalSerializationApi
fun Decoder.asBsonDecoder(): BsonDecoder = this as? BsonDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Bson format." +
                "Expected Decoder to be BsonDecoder, got ${this::class}"
    )

@ExperimentalSerializationApi
fun Encoder.asBsonEncoder() = this as? BsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Bson format." +
                "Expected Encoder to be BsonEncoder, got ${this::class}"
    )