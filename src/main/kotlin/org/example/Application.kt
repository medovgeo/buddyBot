package org.example

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.cdimascio.dotenv.dotenv
import org.example.models.DeepSeek
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication


val logger = LoggerFactory.getLogger("Application")

fun main() {

    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val botToken = dotenv["BOT_TOKEN"] ?: throw IllegalArgumentException("BOT_TOKEN is not set")
    val botName = dotenv["BOT_NAME"] ?: throw IllegalArgumentException("BOT_NAME is not set")
    logger.info(botName)

    val telegramClient = OkHttpTelegramClient(botToken)

    // MongoDB URI from environment variable
    val mongoUri = dotenv["MONGO_URI"] ?: throw IllegalArgumentException("MONGO_URI is not set")
    // MongoDB client
    val client = MongoClient.create(mongoUri)
    val mongo = Mongo(client)

    // LLM model and token
    val geminiModel = dotenv["GEMINI_MODEL"] ?: throw IllegalArgumentException("GEMINI_MODEL is not set")
    val geminiToken = dotenv["GEMINI_TOKEN"] ?: throw IllegalArgumentException("GEMINI_TOKEN is not set")
    // LLM Model client
    val modelApi = DeepSeek(geminiToken, botName)

    val bot = Bot(botName, telegramClient, mongo, modelApi)
    TelegramBotsLongPollingApplication().use { app ->
        app.registerBot(botToken, bot)
        Thread.currentThread().join()
    }

}
