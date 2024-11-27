package org.mongodb.typhon.cli.options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import java.util.concurrent.TimeUnit

class AuthOptions : OptionGroup(name = "Authentication Options") {
    val username by option("-u", "--username", help = "username for authentication").required()
    val password by option("-p", "--password", help = "password for authentication").prompt(hideInput = true)
    val authSource by option("--authenticationDatabase", help = "authentication database").default("admin")
}

class ConnectionOptions : OptionGroup(name = "Connection Options") {
    val host by option(help = "server to connect to").default("localhost")
    val port by option(help = "port to connect to").int().default(27017)
    val uri by option(help = "mongodb uri connection string").convert("URI") { ConnectionString(it) }
}

class NamespaceOptions : OptionGroup(name = "Namespace Options") {
    val database by option("-d", "--database", help = "database to use").default("test")
    val collection by option("-c", "--collection", help = "collection to use")
    val namespace: String
        get() = "$database.$collection"
}

fun MongoClientSettings.Builder.applyAuthOptions(authOptions: AuthOptions?): MongoClientSettings.Builder {
    if (authOptions != null) {
        credential(
            MongoCredential.createCredential(
                authOptions.username,
                authOptions.authSource,
                authOptions.password.toCharArray()
            )
        )
    }
    return this
}

fun MongoClientSettings.Builder.applyConnectionOptions(connectionOptions: ConnectionOptions): MongoClientSettings.Builder {
    if (connectionOptions.uri != null) {
        applyConnectionString(connectionOptions.uri!!)
    } else {
        applyToClusterSettings { it.hosts(listOf(ServerAddress(connectionOptions.host, connectionOptions.port))) }
    }
    applyToClusterSettings { it.serverSelectionTimeout(3L, TimeUnit.SECONDS) }
    return this
}