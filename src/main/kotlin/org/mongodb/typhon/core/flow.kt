package org.mongodb.typhon.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.whileSelect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.TimedValue

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<Result<T>>.mapStats(
    interval: Duration = 1.seconds,
    summarizer: (List<T>) -> Map<String, Int>,
): Flow<Stats> = flow {
    chunked(interval).map {
        it.summarize(summarizer)
    }.collect {
        it.forEach { emit(it) }
    }
}

fun <T> Flow<Result<T>>.mapStats(
    config: TestConfig,
    summarizer: (List<T>) -> Map<String, Int>,
) = mapStats(config.statsCollectionIntervalSeconds.seconds, summarizer)

suspend fun <T> Flow<Result<T>>.collectStats(
    interval: Duration = 1.seconds,
    summarizer: (List<T>) -> Map<String, Int>,
    collector: StatsCollector
) {
    chunked(interval).map {
        it.summarize(summarizer)
    }.collect {
        it.forEach(collector::accept)
    }
}

suspend fun <T> Flow<Result<T>>.collectStats(
    config: TestConfig,
    summarizer: (List<T>) -> Map<String, Int>,
    collector: StatsCollector
) = collectStats(config.statsCollectionIntervalSeconds.seconds, summarizer, collector)

@FlowPreview
@ExperimentalCoroutinesApi
inline fun <A, T> Flow<A>.parMapTimed(
    resultGroup: String,
    delay: Duration,
    concurrency: Int = DEFAULT_CONCURRENCY,
    crossinline transform: suspend (a: A) -> T
): Flow<Result<T>> =
    map { o ->
        flow {
            delay(delay)
            emit(measureResult(resultGroup) {
                transform(o)
            })
        }
    }.flattenMerge(concurrency)

@FlowPreview
@ExperimentalCoroutinesApi
inline fun <A, B> Flow<A>.parMap(
    concurrency: Int = DEFAULT_CONCURRENCY,
    crossinline transform: suspend (a: A) -> B
): Flow<B> =
    map { o ->
        flow {
            emit(transform(o))
        }
    }.flattenMerge(concurrency)

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
fun <T> Flow<T>.chunked(interval: Duration): Flow<TimedValue<List<T>>> =
    channelFlow {
        require(interval.isPositive()) {
            "interval must be positive, but was $interval"
        }
        var results = ArrayList<T>()

        val flowChannel = produce { collect { send(it) } }
        val tickerChannel = ticker(interval.inWholeMilliseconds)
        var lastSummaryTime: TimeMark = TimeSource.Monotonic.markNow()
        var batchElapsed: Duration = Duration.ZERO

        try {
            whileSelect {
                flowChannel.onReceive {
                    batchElapsed = lastSummaryTime.elapsedNow()
                    results.add(it)
                }
                tickerChannel.onReceive {
                    if (results.isNotEmpty()) {
                        send(TimedValue(results, batchElapsed))
                        lastSummaryTime = TimeSource.Monotonic.markNow()
                        results = ArrayList(results.size)
                    }
                    true
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            if (results.isNotEmpty()) {
                send(TimedValue(results, batchElapsed))
            }
        } finally {
            tickerChannel.cancel()
        }
    }

inline fun <reified T, G : FlowGenerator<T>> TestDefinition<G>.asGeneratorFlow(): Flow<T> {
    val start = TimeSource.Monotonic.markNow()
    return this.generator.flow(config.startOffset).run {
        take(config.limit)
    }.run {
        if (config.timeoutSeconds != null) {
            takeWhile {
                start.elapsedNow().inWholeSeconds < config.timeoutSeconds
            }
        } else {
            this
        }
    }
}

fun TestDefinition<*>.asCounterFlow(timeoutSeconds: Int? = config.timeoutSeconds): Flow<Int> {
    val start = TimeSource.Monotonic.markNow()
    return (config.startOffset..config.limit).asFlow().run {
        if (timeoutSeconds != null) {
            takeWhile {
                start.elapsedNow().inWholeSeconds < timeoutSeconds
            }
        } else {
            this
        }
    }
}