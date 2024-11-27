package org.mongodb.typhon.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource

val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = false
}

inline fun <reified G : Generator> logTests(definitions: TestDefinition<G>, block: ((Stats) -> Unit) -> Unit) {
    println("Test Definition: ${prettyJson.encodeToString(definitions)}")
    println("Starting '${definitions.name}'")
    val start = TimeSource.Monotonic.markNow()
    block {
        println(it)
    }
    println("Finished '${definitions.name}' in ${start.elapsedNow()}")
}

inline fun <reified G : Generator> TestDefinition<G>.toJson() =
    prettyJson.encodeToString(this)

fun Stats.toJson() =
    Json.encodeToString(this)