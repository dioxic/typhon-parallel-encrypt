package org.mongodb.typhon.cli.enums

import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

enum class OutputType(private val outputMode: JsonMode) {
    PRETTY(JsonMode.RELAXED),
    ARRAY(JsonMode.EXTENDED),
    NEWLINE(JsonMode.EXTENDED);

    fun jsonWriterSettings(outputMode: JsonMode = this.outputMode): JsonWriterSettings = JsonWriterSettings.builder()
        .indent(this == PRETTY)
        .outputMode(outputMode)
        .build()

    fun isArray() = this == ARRAY
}