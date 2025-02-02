package org.example

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.telegram.telegrambots.meta.api.objects.message.Message as MessageTG

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val botMention = Regex("@.+b", setOf(RegexOption.IGNORE_CASE))

private val mskZone = ZoneId.of("Europe/Moscow")

fun MessageTG.toMessage(overridenText: String? = null) = Message(
    chatId,
    LocalDateTime.ofInstant(Instant.ofEpochSecond(date.toLong()), mskZone),
    (forwardFrom ?: from).extractUsername(),
    overridenText ?: text
)

fun User.extractUsername() = userName.ifBlank { "$firstName $lastName" }

fun MessageTG.toMessageWithReplyieText() =
    toMessage().let { it.copy(text = "${replyToMessage.from.extractUsername()}, ${it.text}") }

fun Update.prepareReply(botName: String): Pair<Message?, Boolean> {

    val mess = when {
        message?.forwardFromChat != null -> null
        message?.text == null -> null
        message?.text?.let { it.contains(botMention) && !it.contains(botName) } ?: true -> null
        message?.replyToMessage?.text != null -> message.toMessageWithReplyieText()
        else -> message.toMessage()
    }

    val needsReply = when {
        (message?.text ?: "").isBlank() -> false
        message?.forwardFromChat != null -> false
        Random.nextInt(1, 100) > 96 -> true
        else -> message?.text?.contains("@$botName") ?: false
                || message?.replyToMessage?.from?.userName == botName
    }

    return mess to !needsReply
}

@Serializable
data class Message(
    val chatId: Long,
//    val messageId: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val from: String,
//    val fromName: String,
//    val fromNickname: String? = null,
//    val replyToMessageId: Int? = null,
    val text: String,
)

@Serializer(forClass = LocalDateTime::class)
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

