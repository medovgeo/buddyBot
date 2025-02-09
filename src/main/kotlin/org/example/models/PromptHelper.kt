package org.example.models


fun generateSystemRole(botName: String): String {
//        var character = characters.get(rnd.nextInt(characters.size()));
//        var manner = manners.get(rnd.nextInt(manners.size()));
    val undertone = undertones.random()
//        val addition = additions[rnd.nextInt(additions.size)]
    val size = sizes.random()
    val extra = extraPrompt.random()

    return "Данный текст в формате json это переписка друзей в чате. " +
//            "Ты один из его участников, под ником $botName. " +
            "Остроумно ответь на последнее сообщение в манере чата, как один из его участников под ником $botName. " +
            extra +
            "Ответ должен быть в текстовом формате " +
            "и быть не больше $size символов" //+ // и содержать $addition " +
}

fun generatePrompt(botName: String, text: String): String {
//        var character = characters.get(rnd.nextInt(characters.size()));
//        var manner = manners.get(rnd.nextInt(manners.size()));
    val undertone = undertones.random()
//        val addition = additions[rnd.nextInt(additions.size)]
    val size = sizes.random()

//    return "Текст ниже в формате json это переписка друзей в чате. " +
//            "Ты один из его участников, под ником $botName. " +
//            "Остроумно ответь на последнее сообщение в чате, с $undertone оттенком. " +
//                "Можешь использовать пару слов или фраз других участников из переписки, если они подходят по смыслу." +
//            "Ответ должен быть в текстовом формате " +
//            "и быть не больше $size символов. " + // и содержать $addition " +
            return "```json\n$text\n```"
}

//        val manners = listOf(
//            "цинично-издевательской манере",
//            "интеллигентной и профессиональной",
//            "юмористической",
//            "ласковой",
//        )

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
//            "обнадёживающим",
//            "хвалебным",
//            "ласковым",
//            "патриотично Российским",
)

private val additions = listOf(
//            "интересный факт",
//            "аналогию",
    "чёрный юмор",
//            "отсылку на Warhammer 40k",
    "оскорбление",
//            "похвалу",
)

private val extraPrompt = listOf(
    "",
    "",
    "",
    "Добавь оскорбление к ответу. ",
    "Добавь в ответ чёрный юмор. ",
    "Натрави участников чата друг на друга. ",
    "Используй слова и фразы других участников. "
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

