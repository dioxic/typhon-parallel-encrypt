@file:OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)

package org.mongodb.typhon.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import org.mongodb.typhon.util.nextString
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@Serializable
sealed interface Generator

@Serializable
sealed interface FlowGenerator<T>: Generator {
    fun flow(startAt: Int = 0): Flow<T>
}

@Serializable
sealed interface SimpleGenerator<T> : FlowGenerator<T> {
    fun invoke(i: Int): T
    override fun flow(startAt: Int): Flow<T> = flow {
        var count = startAt
        while (true) {
            emit(invoke(count++))
        }
    }
}

@Serializable
class RandomStringGenerator(
    val length: Int
): SimpleGenerator<String> {
    override fun invoke(i: Int): String = Random.nextString(length)
}