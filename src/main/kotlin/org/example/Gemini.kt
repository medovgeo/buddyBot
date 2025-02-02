package org.example

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface ModelApi {
    suspend fun generateComment(messages: List<Message>): String
}

// todo : move extra code to separate module and helpers
class Gemini(
    envModel: String,
    envToken: String,
    private val botName: String,
) : ModelApi {

    override suspend fun generateComment(messages: List<Message>): String = runCatching {
//        val text = Json.encodeToString(messages)
        val text = StringBuilder()
        val replyTo = messages.last().replyTo?.let { " replyTo: $it," } ?: ""
        messages.forEach { message ->
            text.append("`from: ${message.from},${replyTo} time: ${message.dateTime.format(formatter)}, text: ${message.text}`\n")
        }
        val httpResponse = withContext(Dispatchers.IO) {
            sendRequest(modelRequestBuilder, buildJson(generatePrompt(text.toString()), true))
        }

        extractResponseText(mapper.readTree(httpResponse))
            .removePrefix(messages.last().from)
            .removePrefix(",")
            .trim()
    }
        .getOrElse {
            logger.error("Error while fetching gemini message, mess: $messages", it)
            ""
        }

    init {
        model = "google/v1beta/models/$envModel:generateContent"
        token = "Bearer $envToken"

        modelRequestBuilder = HttpRequest.newBuilder()
            .uri(URI("https://api.proxyapi.ru/" + model))
            .header("Content-Type", "application/json")
            .header("Authorization", token)
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

        val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(3)).build()

        val mapper = ObjectMapper()

        lateinit var model: String
        lateinit var token: String
        lateinit var modelRequestBuilder: HttpRequest.Builder

//        val manners = listOf(
//            "цинично-издевательской манере",
//            "интеллигентной и профессиональной",
//            "юмористической",
//            "ласковой",
//        )

        val undertones = listOf(
            "иронично-оптимистичным",
            "цинично-издевательским",
            "цинично-издевательским",
            "цинично-издевательским",
            "обречённым",
            "игривым",
            "надменным",
//            "обнадёживающим",
//            "хвалебным",
//            "ласковым",
            "патриотично Российским",
        )

        private val additions = listOf(
//            "интересный факт",
//            "аналогию",
            "чёрный юмор",
//            "отсылку на Warhammer 40k",
            "оскорбление",
//            "похвалу",
        )

        private val sizes = listOf(
            "50",
            "50",
//            "50",
//            "50",
            "300",
            "300",
            "400",
            "400",
        )

//        private static final List<String> patriotManners = List.of(
//        "патриотичной",
//        "оптимистичной"
//        )
    }


    private fun generatePrompt(text: String): String {
//        var character = characters.get(rnd.nextInt(characters.size()));
//        var manner = manners.get(rnd.nextInt(manners.size()));
        val undertone = undertones.random()
//        val addition = additions[rnd.nextInt(additions.size)]
        val size = sizes.random()

        return "Текст ниже это переписка друзей в чате. " +
                "Ты один из его участников, под ником $botName. " +
                "Остроумно ответь на последнее сообщение в манере чата, с $undertone оттенком. " +
//                "Можешь использовать пару слов или фраз других участников из переписки, если они подходят по смыслу." +
                "Ответ должен быть в текстовом формате " +
                "и быть не больше $size символов. " + // и содержать $addition " +
                "```$text```"
    }

    private fun setGenerationCfg(generationConfigNode: ObjectNode) {
        generationConfigNode.put("temperature", 2.0)
//        generationConfigNode.put("maxOutputTokens", 1000)
//        generationConfigNode.put("topP", 0.95)
//        generationConfigNode.put("topK", 10000)
    }

    private fun sendRequest(httpBuilder: HttpRequest.Builder, message: String): String {
        val request = httpBuilder.POST(HttpRequest.BodyPublishers.ofString(message))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
            .body()
    }

    private fun extractResponseText(rootNode: JsonNode) =
        rootNode
            .path("candidates").path(0).path("content")
            .path("parts").path(0).path("text").asText();

    private fun setSafetySettings(rootNode: ObjectNode) {
        // Создаем массив safetySettings
        val safetySettingsArray: ArrayNode = mapper.createArrayNode()

        val dangerousContent = mapper.createObjectNode()
        dangerousContent.put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
        dangerousContent.put("threshold", "OFF")
        safetySettingsArray.add(dangerousContent)

        val harassment = mapper.createObjectNode()
        harassment.put("category", "HARM_CATEGORY_HARASSMENT")
        harassment.put("threshold", "OFF")
        safetySettingsArray.add(harassment)

        val hateSpeech = mapper.createObjectNode()
        hateSpeech.put("category", "HARM_CATEGORY_HATE_SPEECH")
        hateSpeech.put("threshold", "OFF")
        safetySettingsArray.add(hateSpeech)

        val sexuallyExplicit = mapper.createObjectNode()
        sexuallyExplicit.put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
        sexuallyExplicit.put("threshold", "OFF")
        safetySettingsArray.add(sexuallyExplicit)

        val civicIntegrity = mapper.createObjectNode()
        civicIntegrity.put("category", "HARM_CATEGORY_CIVIC_INTEGRITY")
        civicIntegrity.put("threshold", "OFF")
        safetySettingsArray.add(civicIntegrity)
        rootNode.set<ObjectNode>("safetySettings", safetySettingsArray)
    }

    private fun getRootNodeWithContents(text: String): ObjectNode {
        // Создаем корневой объект
        val rootNode = mapper.createObjectNode()

        // Создаем массив contents
        val contentsArray = mapper.createArrayNode()
        val contentObject = mapper.createObjectNode()
        val partsArray = mapper.createArrayNode()
        val partObject = mapper.createObjectNode()

        partObject.put("text", text)

        partsArray.add(partObject)
        contentObject.put("role", "user")
        contentObject.set<ArrayNode>("parts", partsArray)
        contentsArray.add(contentObject)
        rootNode.set<ArrayNode>("contents", contentsArray)
        return rootNode
    }

    private fun buildJson(text: String, setSafety: Boolean): String {

        val rootNode = getRootNodeWithContents(text)

        if (setSafety) setSafetySettings(rootNode)

        // Создаем объект generationConfig
        val generationConfigNode = mapper.createObjectNode()
        setGenerationCfg(generationConfigNode)
        rootNode.set<ObjectNode>("generationConfig", generationConfigNode)

        // Преобразуем в строку с отступами
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode)
    }

}