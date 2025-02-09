package org.example.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.Message
import org.example.models.Gemini.Companion.modelRequestBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class Gemini(envModel: String, envToken: String, private val botName: String) :
    ModelApi {

    init {
        model = "google/v1beta/models/$envModel:generateContent"
        token = "Bearer $envToken"
        modelRequestBuilder = HttpRequest.newBuilder().uri(URI("https://api.proxyapi.ru/" + model))
            .header("Content-Type", "application/json").header("Authorization", token)
    }

    override suspend fun generateComment(messages: List<Message>): String = runCatching {
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
        json.decodeFromString<Response>(resp).candidates.first().content.parts.first().text

    fun buildJson(text: String) = Json.encodeToString(
        Request(
            listOf(Content("user", listOf(Part(text)))),
            listOf(
                SafetySetting(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "OFF"),
                SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "OFF"),
                SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "OFF"),
                SafetySetting(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "OFF"),
                SafetySetting(category = "HARM_CATEGORY_CIVIC_INTEGRITY", threshold = "OFF")
            ),
            GenerationConfig(temperature = 2.0)
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
        val contents: List<Content>,
        val safetySettings: List<SafetySetting>,
        val generationConfig: GenerationConfig
    )

    @Serializable
    data class Content(
        val role: String,
        val parts: List<Part>
    )

    @Serializable
    data class Part(
        val text: String
    )

    @Serializable
    data class SafetySetting(
        val category: String,
        val threshold: String
    )

    @Serializable
    data class GenerationConfig(
        val temperature: Double
    )

    @Serializable
    data class Response(
        val candidates: List<Candidate>
    )

    @Serializable
    data class Candidate(
        val content: Content
    )
}

