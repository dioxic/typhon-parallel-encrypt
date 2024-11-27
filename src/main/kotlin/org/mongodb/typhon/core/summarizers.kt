package org.mongodb.typhon.core

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.result.InsertManyResult

fun intSummarizer(mv: List<Int>): Map<String, Int> =
    mapOf("inserts/s" to mv.sum())

fun insertManySummarizer(mv: List<InsertManyResult>): Map<String, Int> =
    mapOf("inserts/s" to mv.sumOf { it.insertedIds.size })

fun intMapSummarizer(mv: List<Map<String, Int>>): Map<String, Int> =
    mv.flatMap { it.entries }
        .groupBy { it.key }
        .mapValues { (_, v) -> v.sumOf { it.value } }

fun insertManyMapSummarizer(mv: List<Map<String, InsertManyResult>>) =
    mv.flatMap { it.entries }
        .groupBy { it.key }
        .mapValues { (_, v) -> v.sumOf { it.value.insertedIds.size } }

fun bulkWriteSummarizer(mv: List<BulkWriteResult>) =
    mapOf(
        "inserts/s" to mv.sumOf { it.insertedCount },
        "modified/s" to mv.sumOf { it.modifiedCount },
        "matched/s" to mv.sumOf { it.matchedCount },
        "deleted/s" to mv.sumOf { it.deletedCount },
        "upserts/s" to mv.sumOf { it.upserts.size }
    )