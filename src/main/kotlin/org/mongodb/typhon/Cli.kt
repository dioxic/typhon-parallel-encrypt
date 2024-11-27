package org.mongodb.typhon

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.subcommands
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import org.bson.Document
import org.mongodb.typhon.commands.*

class Cli : SuspendingCliktCommand() {
    override suspend fun run() = Unit
}

suspend fun checkConnection(client: MongoClient): Boolean =
    try {
        println("Checking connection...")
        client.getDatabase("test").runCommand(Document("ping", 1))
        true
    } catch (_: MongoException) {
        client.clusterDescription.srvResolutionException?.let {
            println(it.message)
            return false
        }
        val serverAddresses = client.clusterDescription.serverDescriptions.map { it.address }
        println("Failed to connect to $serverAddresses")
        false
    }

suspend fun main(args: Array<String>) = Cli()
    .subcommands(
        ParallelEncrypt(),
    ).main(args)