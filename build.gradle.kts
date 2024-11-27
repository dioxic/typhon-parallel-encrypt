import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.github.ben-manes.versions") version "0.51.0"
    application
}

group = "com.mongodb.typhon"
version = "1.0-SNAPSHOT"

val kotlinSerializationVersion = "1.7.3"
val mongodbVersion = "4.10.0"
val cryptVersion = "1.7.3"
//val mongodbVersion = "5.2.1"
//val cryptVersion = "5.2.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.24.1"))
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.mongodb:mongodb-driver-kotlin-sync:$mongodbVersion")
    implementation("org.mongodb:mongodb-driver-sync:$mongodbVersion")
    implementation("org.mongodb:mongodb-crypt:$cryptVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.mongodb:bson-kotlinx:$mongodbVersion")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

application {
    mainClass.set("org.mongodb.typhon.CliKt")
}

distributions {
    main {
        distributionBaseName.set("cli")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.test {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
}