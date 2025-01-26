package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import org.telegram.telegrambots.meta.api.objects.message.Message as MessageTG

class Bot(
    private val botName: String,
    private val telegramClient: OkHttpTelegramClient,
    private val mongo: Mongo,
    private val gemini: Gemini,
) : LongPollingSingleThreadUpdateConsumer {

    private val errHandler = CoroutineExceptionHandler { _, throwable ->
        logger.error("Error in message processing", throwable)
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + errHandler)

    private val chatMutexes = ConcurrentHashMap<Long, Mutex>() // todo : cache expiring by time

    override fun consume(update: Update) {
        if (!update.toProcess()) return

        val messages = update.extractMessages()

        coroutineScope.launch {

//            logger.info(update.toString()) // todo : delete
            val chatMutex = chatMutexes.getOrPut(update.message.chatId) { Mutex() }
            chatMutex.withLock { // messages in single chat processed sequentially

                startTyping(update)
                // todo : put message(s) to cache?
                mongo.saveMessages(messages) // put message(s) to mongo - ignore failures
                val history = mongo.getChatHistory(update.message.chatId) // get chat history (todo : cache or mongo)

                startTyping(update)
                val comment = gemini.generateComment(history) // request reply in gemini
                // send reply to chat
                if (comment.isNotBlank()) {
                    sengTGMessage(SendMessage(update.message.chat.id.toString(), comment))?.let { message ->
                        // save reply to mongo
                         mongo.saveMessages(listOf(message.toMessage()))
                    }
                }

            }
        }
    }

    private fun Update.extractMessages() =
        if (message?.replyToMessage != null)
            listOf(message.replyToMessage.toMessage(), message.toMessage())
        else
            listOf(message.toMessage())

    private fun Update.toProcess(): Boolean {
        if ((message?.text?:"").isBlank()) return false

        return message?.text?.contains("@$botName")?:false
                || message?.replyToMessage?.from?.userName == botName
    }

    private suspend fun sengTGMessage(message: SendMessage) = try {
        telegramClient.executeAsync(message).await()
    } catch (e: Exception) {
        logger.error("Error sending message with telegram client, message: $message", e)
        null
    }

    private suspend fun startTyping(update: Update) = try {
        val typeAction = SendChatAction(update.message.chatId.toString(), ActionType.TYPING.toString())
        telegramClient.executeAsync(typeAction).await()
    } catch (e: Exception) {
        logger.error("Error typing in telegram client, chatId: ${update.message.chatId}", e)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

        private val mskZone = ZoneId.of("Europe/Moscow")

        fun MessageTG.toMessage() = Message(
            chatId,
            messageId,
            LocalDateTime.ofInstant(Instant.ofEpochSecond(date.toLong()), mskZone),
            from.firstName + " " + from.lastName,
            from.userName.ifBlank { null },
            replyToMessage?.messageId,
            text
        )

    }
}

@Serializable
data class Message(
    val chatId: Long,
    val messageId: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val fromName: String,
    val fromNickname: String? = null,
    val replyToMessageId: Int? = null,
    val text: String,
)

@Serializer(forClass = LocalDateTime::class)
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val mm = ConcurrentHashMap<Long, Mutex>()
    (1..6).forEach {
        scope.launch {
            val m = mm.getOrPut(it % 2L) { Mutex() }
            m.withLock {
                delay(1000)
                println('1')
            }
        }
    }

    Thread.sleep(6000)

}