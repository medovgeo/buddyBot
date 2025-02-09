package org.example.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class DeepSeek(envToken: String, private val botName: String) : ModelApi {

    init {
        model = "deepseek/chat/completions"
        token = "Bearer $envToken"
        modelRequestBuilder = HttpRequest.newBuilder().uri(URI("https://api.proxyapi.ru/" + model))
            .header("Content-Type", "application/json").header("Authorization", token)
    }

    override suspend fun generateComment(messages: List<org.example.Message>): String = runCatching {
        val text = Json.encodeToString(messages)
        val httpResponse = withContext(Dispatchers.IO) {
            sendRequest(modelRequestBuilder, buildJson(generatePrompt(botName, text)))
        }
        extractResponseText(httpResponse)
    }.getOrElse {
            logger.error("Error while fetching gemini message, mess: $messages", it)
            ""
        }

    fun sendRequest(httpBuilder: HttpRequest.Builder, message: String): String {
        val request = httpBuilder.POST(HttpRequest.BodyPublishers.ofString(message)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun extractResponseText(resp: String) =
        json.decodeFromString<Response>(resp).choices.first().message.content

    fun buildJson(text: String) = Json.encodeToString(
        Request(
            messages = listOf(
                Message("system", generateSystemRole(botName)),
                Message("user", generatePrompt(botName, text)),
            ),
        )
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)
        val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(3)).build()
        val json = Json { ignoreUnknownKeys = true }

        lateinit var model: String
        lateinit var token: String
        lateinit var modelRequestBuilder: HttpRequest.Builder
    }

    @Serializable
    data class Request(
        val model: String = "deepseek-chat",
        val messages: List<Message>,
        val response_format: RespFormat = RespFormat("text"),
        val temperature: Double = 1.8,
    )

    @Serializable
    data class RespFormat(
        val type: String,
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String,
    )

    @Serializable
    data class Response(
        val choices: List<Choice>
    )

    @Serializable
    data class Choice(
        val message: Message,
    )
}