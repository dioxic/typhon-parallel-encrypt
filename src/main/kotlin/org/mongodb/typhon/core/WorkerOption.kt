package org.mongodb.typhon.core

import kotlinx.serialization.Serializable

@Serializable
sealed class WorkerOption {
    abstract val value: Int

    data class Fixed(val workers: Int): WorkerOption() {
        override val value: Int
            get() = workers
    }

    data class Range(val maxWorkers: Int): WorkerOption() {
        override val value: Int
            get() = maxWorkers
    }
}