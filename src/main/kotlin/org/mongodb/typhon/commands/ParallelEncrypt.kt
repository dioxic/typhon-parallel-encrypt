package org.mongodb.typhon.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.mongodb.*
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.vault.DataKeyOptions
import com.mongodb.client.model.vault.EncryptOptions
import com.mongodb.client.vault.ClientEncryption
import com.mongodb.client.vault.ClientEncryptions
import com.mongodb.kotlin.client.MongoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOn
import org.bson.BsonBinary
import org.bson.BsonString
import org.bson.Document
import org.mongodb.typhon.cli.options.AuthOptions
import org.mongodb.typhon.cli.options.ConnectionOptions
import org.mongodb.typhon.cli.options.applyAuthOptions
import org.mongodb.typhon.cli.options.applyConnectionOptions
import org.mongodb.typhon.core.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

class ParallelEncrypt : SuspendingCliktCommand() {
    init {
        context { helpFormatter = { ctx -> MordantHelpFormatter(ctx, showDefaultValues = true) } }
    }

    private val authOptions by AuthOptions().cooccurring()
    private val connOptions by ConnectionOptions()
    private val limit by option("-l", "--limit", help = "execution limit")
        .int().default(Int.MAX_VALUE)
    private val timeoutSeconds by option("-t", "--timeout", help = "timeout in seconds")
        .int()
    private val workers by option("-w", "--workers", help = "number of workers")
        .int().default(1)

    override fun help(context: Context) = "Expicit CSFLE in parallel"

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
    override suspend fun run() {
        require(timeoutSeconds != null || limit != Int.MAX_VALUE) {
            "--timeout or --limit should be set"
        }

        val mcs = MongoClientSettings.builder()
            .applyAuthOptions(authOptions)
            .applyConnectionOptions(connOptions)
            .build()

        val defTx = TestDefinition(
            name = "csfle",
            description = "test parallism of csfle explict encyption",
            generator = RandomStringGenerator(length = 512),
            config = TestConfig(
                limit = limit,
                workers = workers,
                ordered = false,
                statsCollectionIntervalSeconds = 1,
                timeoutSeconds = timeoutSeconds,
                readConcern = ReadConcern.SNAPSHOT,
                writeConcern = WriteConcern.MAJORITY,
                readPreference = ReadPreference.primary()
            ),
        )

        println("initiating...")
        MongoClient.create(mcs).use { kvClient ->
            kvClient.initializeKeyVault("test")
//            val ce = ThreadLocal.withInitial { createClientEncryption(mcs, generateMasterKey()) }
//            val ce1 = ce.get()
//            val dek = ce1.createDataKey()

//            val ce = createClientEncryption(mcs, generateMasterKey())

            createClientEncryption(mcs, generateMasterKey()).use { ce ->
                val dek = ce.createDataKey()
                defTx.asGeneratorFlow()
                    .parMapTimed("enc", 0.milliseconds, workers) {
                        ce.encrypt(it, dek)
                        mapOf(
                            "enc/s" to 1
                        )
                    }
                    .flowOn(Dispatchers.IO.limitedParallelism(30))
                    .collectStats(defTx.config, ::intMapSummarizer, simpleConsoleStatsCollector)
            }

        }
    }

    fun ClientEncryption.encrypt(plainText: String, keyId: BsonBinary): BsonBinary =
        encrypt(BsonString(plainText), ENCRYPTION_OPTIONS.keyId(keyId))

    fun ClientEncryption.decrypt(cipherText: BsonBinary) =
        decrypt(cipherText)

    fun MongoClient.initializeKeyVault(name: String) {
        val keyVaultCollection = getDatabase(VAULT_DB)
            .getCollection<Document>(name.lowercase() + VAULT_COLLECTION_SUFFIX)

        keyVaultCollection.drop()
        val indexOpts = IndexOptions()
            .partialFilterExpression(Document("keyAltNames", Document("\$exists", true)))
            .unique(true)

        keyVaultCollection.createIndex(ascending("keyAltNames"), indexOpts)
    }

    fun createClientEncryption(mcs: MongoClientSettings, masterKey: ByteArray): ClientEncryption =
        ClientEncryptionSettings.builder()
            .keyVaultMongoClientSettings(mcs)
            .keyVaultNamespace(getKeyVaultNamespace("test"))
            .kmsProviders(getKmsProviders(masterKey))
            .build()
            .let { ClientEncryptions.create(it) }

    fun generateMasterKey() = Random.nextBytes(96)

    fun getKeyVaultNamespace(name: String) = "$VAULT_DB.${name.lowercase()}$VAULT_COLLECTION_SUFFIX"

    fun getKmsProviders(masterKey: ByteArray) =
        mapOf(
            "local" to mapOf("key" to masterKey)
        )

    fun ClientEncryption.createDataKey(keyAltNames: List<String> = listOf(DATA_KEY_ALT_NAME)): BsonBinary =
        createDataKey("local", DataKeyOptions().keyAltNames(keyAltNames))

    companion object {
        private const val VAULT_DB = "keys"

        //        private const val VAULT_COLLECTION = "deks"
        private const val VAULT_COLLECTION_SUFFIX = "_keys"
        private const val DATA_KEY_ALT_NAME = "dek"
        private val ENCRYPTION_OPTIONS = EncryptOptions("AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic")
    }

}
