package org.example

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Mongo(client: MongoClient) {
    private val collection = client.getDatabase("buddy_bot").getCollection<Message>("chats")

    suspend fun saveMessage(message: Message) = try {
        collection.insertOne(message)
    } catch (e: Exception) {
        logger.error("Error saving message in mongo, message: $message", e)
    }

//    suspend fun saveMessages(messages: List<Message>) = try {
//        messages.forEach {
//
//            val filter = Filters.and(Filters.eq(Message::chatId.name, it.chatId), Filters.eq(Message::messageId.name, it.messageId))
//            val update = Updates.combine(
//                Updates.setOnInsert(Message::chatId.name, it.chatId),
//                Updates.setOnInsert(Message::messageId.name, it.messageId),
//                Updates.setOnInsert(Message::dateTime.name, it.dateTime),
//                Updates.setOnInsert(Message::fromName.name, it.fromName),
//                Updates.setOnInsert(Message::fromNickname.name, it.fromNickname),
//                Updates.setOnInsert(Message::replyToMessageId.name, it.replyToMessageId),
//                Updates.setOnInsert(Message::text.name, it.text),
//            )
//            val options = UpdateOptions().upsert(true)
//            collection.updateOne(filter, update, options)
//        }
//    } catch (e: Exception) {
//        logger.error("Error saving message in mongo, message: $messages", e)
//    }

    suspend fun getChatHistory(chatId: Long): MutableList<Message> = try {
        collection.find(Filters.eq(Message::chatId.name, chatId)).limit(50)
            .toCollection(mutableListOf())
    } catch (e: Exception) {
        logger.error("Error reading messages from mongo for chatId: $chatId", e)
        mutableListOf()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)
    }
}