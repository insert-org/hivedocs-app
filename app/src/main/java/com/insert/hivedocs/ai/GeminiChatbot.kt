package com.insert.hivedocs.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.insert.hivedocs.BuildConfig
import com.insert.hivedocs.data.KnowledgeBase

object GeminiChatbot {

    private fun buildPrompt(question: String): String {
        val knowledgeString = KnowledgeBase.qna.entries.joinToString("\n") { (keywords, answer) ->
            "Se a pergunta contiver palavras como '${keywords.joinToString(", ")}', responda com: '$answer'"
        }

        return """
            Você é o "Bee", um assistente virtual amigável e prestativo do aplicativo HiveDocs.
            Sua única fonte de verdade é a base de conhecimento abaixo. Responda sempre em Português do Brasil, de forma clara e direta.
            Não invente funcionalidades que não estão listadas. Se a pergunta do usuário não corresponder a nada na base de conhecimento,
            responda: "Desculpe, não encontrei informações sobre isso. Posso te ajudar com dúvidas sobre como enviar, ler ou avaliar artigos?".

            --- INÍCIO DA BASE DE CONHECIMENTO ---
            $knowledgeString
            --- FIM DA BASE DE CONHECIMENTO ---

            Pergunta do usuário: "$question"
            Sua resposta:
        """.trimIndent()
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.API_KEY
    )

    suspend fun generateResponse(userInput: String): String {
        return try {
            val prompt = buildPrompt(userInput)
            val response = generativeModel.generateContent(prompt)
            response.text ?: "Desculpe, não consegui processar sua pergunta no momento."
        } catch (e: Exception) {
            e.printStackTrace()
            "Ocorreu um erro ao me conectar. Por favor, verifique sua conexão com a internet e tente novamente."
        }
    }
}