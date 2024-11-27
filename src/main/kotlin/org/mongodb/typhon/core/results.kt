package org.mongodb.typhon.core

import kotlinx.datetime.Clock
import org.apache.commons.math3.stat.StatUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

data class Result<T>(val group: String, val duration: Duration, val value: T)

fun List<Result<*>>.summarizeDurations(group: String): Stats {
    val durations = map { it.duration }
    return Stats(
        timestamp = Clock.System.now(),
        group = group,
        executions = size,
        p50 = durations.percentile(50.0),
        p95 = durations.percentile(95.0),
        p99 = durations.percentile(99.0),
        max = durations.max(),
    )
}

inline fun <T> List<Result<T>>.summarize(
    group: String,
    batchDuration: Duration,
    crossinline summarizer: (List<T>) -> Map<String, Int>
): Stats {
    val scale = 1.seconds.div(batchDuration)
    return summarizeDurations(group).copy(
        throughputs = summarizer.invoke(map { it.value })
            .mapValues { (_, v) -> v.times(scale).toInt() }
    )
}

private fun List<Map<String, Int>>.sum(): Map<String, Int> {
    return flatMap { it.entries }
        .groupBy { it.key }
        .mapValues { (_, m) ->
            m.sumOf { it.value }
        }
}

fun Iterable<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }.percentile(percentile)

fun Sequence<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }
        .toList().percentile(percentile)

fun List<Double>.percentile(percentile: Double) =
    StatUtils.percentile(toDoubleArray(), percentile)
        .toDuration(DurationUnit.MILLISECONDS)

fun <T> TimedValue<List<Result<T>>>.summarize(summarizer: (List<T>) -> Map<String, Int>) =
    value.groupBy { it.group }
        .map { (group, results) ->
            results.summarize(group, duration, summarizer)
        }

@OptIn(ExperimentalContracts::class)
inline fun <T> measureResult(group: String, block: () -> T): Result<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return TimeSource.Monotonic.measureResult(group, block)
}

@OptIn(ExperimentalContracts::class)
inline fun <T> TimeSource.measureResult(group: String, block: () -> T): Result<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val mark = markNow()
    val res = block()
    return Result(group, mark.elapsedNow(), res)
}