package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.Bot.Companion.mskZone
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
import java.time.ZoneOffset
import org.telegram.telegrambots.meta.api.objects.message.Message as MessageTG
import java.util.concurrent.ConcurrentHashMap

class Bot(
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
        if (!update.hasMessage()) return
        // todo : if message has mention or reply to bot
        val messages = if (update.message.replyToMessage != null)
            listOf(update.message.replyToMessage.toMessage(), update.message.toMessage())
        else
            listOf(update.message.toMessage())

        coroutineScope.launch {

            println(update)
            val chatMutex = chatMutexes.getOrPut(update.message.chatId) { Mutex() }
            chatMutex.withLock { // messages in single chat processed sequentially
                startTyping(update)

                // todo : put message(s) to cache?
                // put message(s) to mongo - ignore failure
                mongo.saveMessages(messages)
                // get chat history (cache or mongo)
                startTyping(update)
                // request reply in gemini
                // send reply to chat
                // save reply to mongo : with message id, etc


                val mess = SendMessage(update.message.chat.id.toString(), update.message.text)
                sengTGMessage(mess)
            }
        }
    }

    private suspend fun sengTGMessage(message: SendMessage) = try {
        telegramClient.executeAsync(message).await()
    } catch (e: Exception) {
        logger.error("Error sending message with telegram client", e)
    }

    private suspend fun startTyping(update: Update) = try {
        val typeAction = SendChatAction(update.message.chatId.toString(), ActionType.TYPING.toString())
        telegramClient.executeAsync(typeAction).await()
    } catch (e: Exception) {
        logger.error("Error sending message with telegram client", e)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

        val mskZone = ZoneId.of("Europe/Moscow")

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

data class Message(
    val chatId: Long,
    val messageId: Int,
    val dateTime: LocalDateTime,
    val fromName: String,
    val fromNickname: String? = null,
    val replyToMessageId: Int? = null,
    val text: String,
)

fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val inst = Instant.ofEpochSecond(1737897000)
    println(inst)
    val lt = LocalDateTime.ofInstant(inst, ZoneId.of("Europe/Moscow"))
    println(lt)
    println(LocalDateTime.ofEpochSecond(1737897000, 0, ZoneOffset.of(mskZone.id)).toString(),)

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