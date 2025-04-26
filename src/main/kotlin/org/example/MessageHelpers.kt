package org.example

import kotlinx.serialization.*
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
    messageId,
    (forwardFrom ?: from).extractUsername(),
    LocalDateTime.ofInstant(Instant.ofEpochSecond(date.toLong()), mskZone),
    replyToMessage?.from?.extractUsername(),
    overridenText ?: text
)

fun User.extractUsername() = userName.ifBlank { "$firstName $lastName" }

fun MessageTG.toMessageWithReplyieText() =
    toMessage().let { it.copy(text = "${replyToMessage.from.extractUsername()}, ${it.text}") }

fun Update.prepareReply(botName: String): Pair<List<Message>, Boolean> {

    val mess = when {
        message?.forwardFromChat != null -> emptyList()
        message?.text == null -> emptyList()
        message?.text?.let { it.contains(botMention) && !it.contains(botName) } ?: true -> emptyList()
        message?.replyToMessage?.from?.userName == botName -> listOf(message.replyToMessage.toMessage(), message.toMessage())
        else -> listOf(message.toMessage())
    }

    val needsReply = when {
        (message?.text ?: "").isBlank() -> false
        message?.forwardFromChat != null -> false
//        Random.nextInt(1, 100) > 96 -> true
        else -> message?.text?.contains("@$botName") ?: false
                || message?.replyToMessage?.from?.userName == botName
    }

    return mess to !needsReply
}

@Serializable
data class Message(
    val chatId: Long,
    val messageId: Int,
    val from: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val replyTo: String? = null,
    val text: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
class LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

