package org.example

import com.mongodb.kotlin.client.coroutine.MongoClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


val logger = LoggerFactory.getLogger("Application")

fun main() {

    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val botToken = dotenv["BOT_TOKEN"] ?: throw IllegalArgumentException("BOT_TOKEN is not set")

    val telegramClient = OkHttpTelegramClient(botToken)

    // MongoDB URI from environment variable
    val mongoUri = dotenv["MONGO_URI"] ?: throw IllegalArgumentException("MONGO_URI is not set")
    // MongoDB client
    val client = MongoClient.create(mongoUri)
    val mongo = Mongo(client)

    // Gemini model and token
    val geminiModel = dotenv["GEMINI_MODEL"] ?: throw IllegalArgumentException("GEMINI_MODEL is not set")
    val geminiToken = dotenv["GEMINI_TOKEN"] ?: throw IllegalArgumentException("GEMINI_TOKEN is not set")
    // Gemini client
    val gemini = Gemini(geminiModel, geminiToken)

    val bot = Bot(telegramClient, mongo, gemini)

//    runBlocking {
//        mongo.getChatHistory(321710353)
//            .forEach { println(it) }
//    }

    TelegramBotsLongPollingApplication().use { app ->
        app.registerBot(botToken, bot)
        Thread.currentThread().join()
    }



}
