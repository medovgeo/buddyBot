package org.example

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse;
import java.security.SecureRandom

interface ModelApi {
    fun commentNews(message: String): String
}

class Gemini(
    private val envModel: String,
    private val envToken: String,
) : ModelApi {

    override fun commentNews(message: String) = runCatching {
        val httpResp = sendRequest(modelRequestBuilder, buildJson(commentNewsPrompt(message), true));
        extractResponseText(mapper.readTree(httpResp));
    }
        .getOrElse {
            // add logging
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

        val mapper = ObjectMapper()

        lateinit var model: String
        lateinit var token: String
        lateinit var modelRequestBuilder: HttpRequest.Builder


//        private static final List<String> characters = List.of(
//        "собирательного образа Ильи Мэддисона, Юрия Хованского и Богдана Ласки Вавилова"
//            "Максима Каца",
//            "Джи Джи Аллина"
//            "Бората"
//        );

//        private static final List<String> manners = List.of(
//        "цинично-издевательской манере",
//        "интеллигентной и профессиональной",
//        "юмористической"
//            "ласковой"
//        );

//        private static final List<String> undertones = List.of(
//        "оптимистичным",
//        "обречённым",
//        "обнадёживающим"
//            "восхваляющим",
//            "непринуждённым",
//            "безумным"
//        );

//        private static final List<String> additions = List.of(
//        "интересный факт",
//        "аналогию",
//        "чёрный юмор"
//            "тонкую отсылку на Звёздные Войны",
//            "тонкую отсылку на Warhammer 40k"
//        );

//        private static final List<String> sizes = List.of(
//        "200",
//        "300",
//        "400",
//        "400",
//        "400",
//        "1000"
//        );

//        private static final List<String> patriotManners = List.of(
//        "патриотичной",
//        "оптимистичной"
//        );


        val rnd = SecureRandom()
        val client = HttpClient.newHttpClient()

    }

    private fun setGenerationCfg(generationConfigNode: ObjectNode) {
        generationConfigNode.put("temperature", 2.0)
//        generationConfigNode.put("maxOutputTokens", 1000)
//        generationConfigNode.put("topP", 0.95)
//        generationConfigNode.put("topK", 10000)
    }

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

    fun commentNewsPrompt(message: String): String {
//        var character = characters.get(rnd.nextInt(characters.size()));
//        var manner = manners.get(rnd.nextInt(manners.size()));
//        var undertone = undertones.get(rnd.nextInt(undertones.size()));
//        var addition = additions.get(rnd.nextInt(additions.size()));
//        var size = sizes.get(rnd.nextInt(sizes.size()));

//        return "Прокомментируй новость от лица" + character + ", " +
//                "в остроумной и " + manner + " манере, " +
//                "c ироничным и " + undertone + " подтекстом " +
//                "и добавь " + addition + " в ответ. " +
//                "Размер ответа не должен превышать " + size + " символов. " +
//                "Не упоминай своё имя в ответе." +
//                "\n```" +
//                "\nНовость: " + message +
//                "\n```";
        return message
    }

}

fun main() {

    val dotenv = dotenv { ignoreIfMissing = true }
    val model = dotenv["GEMINI_MODEL"]
    val token = dotenv["GEMINI_TOKEN"]

    val proxy = Gemini(model, token)

    val originalMessage = "Привет"

    val response = proxy.commentNews(originalMessage)

    println("Response Code: " + response)
}
