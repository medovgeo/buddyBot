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

    override suspend fun generateComment(messages: List<org.example.Message>): String {
        val text = Json.encodeToString(messages)
        val request = buildJson(text)
        return try {
            val httpResponse = withContext(Dispatchers.IO) {
                sendRequest(modelRequestBuilder, request)
            }
            extractResponseText(httpResponse)
        } catch (e: Exception) {
            logger.error("Error while fetching deepseek message, request: $request", e)
            ""
        }
    }

    fun sendRequest(httpBuilder: HttpRequest.Builder, message: String): String {
        val request = httpBuilder.POST(HttpRequest.BodyPublishers.ofString(message)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun extractResponseText(resp: String) = try {
        json.decodeFromString<Response>(resp).choices.first().message.content
    } catch (e: Exception) {
        logger.error("Error while parsing deepseek message, mess: $resp", e)
        throw e
    }

    fun buildJson(text: String) = Json.encodeToString(
        Request(
            model = "deepseek-chat",
            messages = listOf(
                Message("system", generateSystemRole(botName)),
                Message("user", generatePrompt(botName, text)),
            ),
            response_format = RespFormat("text"),
            temperature = 1.5
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
        val model: String,
        val messages: List<Message>,
        val response_format: RespFormat,
        val temperature: Double,
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