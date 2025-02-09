package org.example.models

import org.example.Message

interface ModelApi {
    suspend fun generateComment(messages: List<Message>): String
}
