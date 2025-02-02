package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


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
        coroutineScope.launch {

            val chatMutex = chatMutexes.getOrPut(update.message.chatId) { Mutex() }
            chatMutex.withLock { // messages in single chat processed sequentially
                val (messages, noReply) = update.prepareReply(botName)
                saveMessageAndReply(messages, noReply)
            }

        }
    }

    private suspend fun saveMessageAndReply(messages: List<Message>, noReply: Boolean) {
        // todo : put message(s) to cache?
        if (messages.isEmpty()) return
//        mongo.saveMessage(message)
        if (noReply) {
            mongo.saveMessage(messages.last())
            return
        }

        val history = mongo.getChatHistory(messages.last().chatId) // get chat history (todo : cache or mongo)
        history.addAll(messages)

        val comment = gemini.generateComment(history) // request reply in gemini

        mongo.saveMessage(messages.last())

        // send reply to chat
        if (comment.isNotBlank()) {
            startTyping(messages.last().chatId.toString())
            delay(Random.nextInt(3, 10) * 1000L)
            SendMessage(messages.last().chatId.toString(), comment)
                .apply {
                    replyToMessageId = messages.last().messageId
                    allowSendingWithoutReply = true
                    sengTGMessage(this)
                }
        }

    }

    private suspend fun sengTGMessage(message: SendMessage) = try {
        telegramClient.executeAsync(message).await()
    } catch (e: Exception) {
        logger.error("Error sending message with telegram client, message: $message", e)
        null
    }

    private suspend fun startTyping(chatId: String) = try {
        val typeAction = SendChatAction(chatId, ActionType.TYPING.toString())
        telegramClient.executeAsync(typeAction).await()
    } catch (e: Exception) {
        logger.error("Error typing in telegram client, chatId: $chatId", e)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)
    }
}
