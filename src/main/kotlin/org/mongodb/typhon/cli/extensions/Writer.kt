package org.mongodb.typhon.cli.extensions

import org.bson.codecs.Codec
import org.bson.codecs.EncoderContext
import org.bson.json.JsonWriter
import org.bson.json.JsonWriterSettings
import java.io.Writer

fun <T> Writer.writeJson(values: Iterable<T>, codec: Codec<T>, jws: JsonWriterSettings, arrayOutput: Boolean = false) =
    writeJson(values.iterator(), codec, jws, arrayOutput)

fun <T> Writer.writeJson(values: Sequence<T>, codec: Codec<T>, jws: JsonWriterSettings, arrayOutput: Boolean = false) =
    writeJson(values.iterator(), codec, jws, arrayOutput)

fun <T> Writer.writeJson(values: Iterator<T>, codec: Codec<T>, jws: JsonWriterSettings, arrayOutput: Boolean = false) {
    if (arrayOutput) append('[')

    var first = true

    for (doc in values) {
        when {
            first -> first = false
            arrayOutput -> append(",")
            else -> write(System.lineSeparator())
        }
        writeJson(doc, jws, codec)
    }

    if (arrayOutput) append(']')
}

fun <T> Writer.writeJson(value: T, jws: JsonWriterSettings, codec: Codec<T>) =
    codec.encode(JsonWriter(this, jws), value, EncoderContext.builder().build())