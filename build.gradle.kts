plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // Shadow plugin for fat JAR
}

group = "org.example"
version = "1.0.0"

application {
    mainClass.set("org.example.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin standard library
    implementation(kotlin("stdlib"))

    // telegram bots api
    implementation("org.telegram:telegrambots-longpolling:8.0.0")
    implementation("org.telegram:telegrambots-client:8.0.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.5")

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    // MongoDB Kotlin driver dependency
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.3.0")

    // For dotenv (optional, for environment variables)
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

// Configure the Shadow JAR
tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("buddyBot")
        archiveClassifier.set("all")
        archiveVersion.set("1.0.0")

        // Ensure the main class is specified in the manifest
        manifest {
            attributes["Main-Class"] = "org.example.ApplicationKt"
        }

    }
    // Make the build task depend on shadowJar
    build {
        dependsOn(shadowJar)
    }
}
