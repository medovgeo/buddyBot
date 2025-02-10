package org.example.models


fun generateSystemRole(botName: String): String {
    val undertone = undertones.random()
    val size = sizes.random()
    val extra = extraPrompt.random()

    return "Данный текст в формате json это переписка друзей в чате. " +
            "Ответь на последнее сообщение в манере чата, как один из его участников под ником $botName. " +
            extra +
            "Ответ должен быть в текстовом формате " +
            "и быть не больше $size символов" //+ // и содержать $addition " +
}

fun generatePrompt(text: String) = "```json\n$text\n```"

val undertones = listOf(
    "иронично-оптимистичным",
//    "иронично-оптимистичным",
//            "цинично-издевательским",
    "цинично-издевательским",
    "цинично-издевательским",
//    "обречённым",
//    "игривым",
    "надменным",
    "дерзким",
)

private val extraPrompt = listOf(
    "",
    "",
    "Добавь оскорбление к ответу. ",
    "Добавь в ответ чёрный юмор. ",
    "Натрави участников чата друг на друга. ",
)

private val sizes = listOf(
    "50",
    "50",
    "300",
    "300",
    "400",
    "400",
)