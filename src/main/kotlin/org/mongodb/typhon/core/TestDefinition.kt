package org.mongodb.typhon.core

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TestDefinition<G: Generator>(
    val name: String,
    val description: String,
    val startTime: Instant = Clock.System.now(),
    val generator: G,
    val config: TestConfig,
    val tags: Map<String, String>? = null,
    val collStats: CollectionStats? = null
)
