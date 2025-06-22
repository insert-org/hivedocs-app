package com.insert.hivedocs.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.insert.hivedocs.ai.GeminiChatbot
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isFromUser: Boolean, val isLoading: Boolean = false)

@Composable
fun ChatbotScreen() {
    val initialMessage = ChatMessage(
        text = "Olá! Eu sou o Bee 🐝, assistente virtual do HiveDocs. Como posso te ajudar hoje?",
        isFromUser = false
    )
    var messages by remember { mutableStateOf(listOf(initialMessage)) }
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun sendMessage() {
        if (userInput.isNotBlank()) {
            val userMessageText = userInput
            messages = messages + ChatMessage(userMessageText, true)
            messages = messages + ChatMessage("Bee está digitando...", false, isLoading = true)
            userInput = ""

            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }

            coroutineScope.launch {
                val botResponseText = GeminiChatbot.generateResponse(userMessageText)
                messages = messages.dropLast(1) + ChatMessage(botResponseText, false)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        bottomBar = { MessageInput(userInput, { userInput = it }, { sendMessage() }) }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (message.isFromUser) 16.dp else 0.dp, bottomEnd = if (message.isFromUser) 0.dp else 16.dp))
                .background(if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            if (message.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bee está digitando...")
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }
            } else {
                Text(text = message.text)
            }
        }
    }
}

@Composable
fun MessageInput(
    userInput: String,
    onUserInputChanged: (String) -> Unit,
    onMessageSent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChanged,
            label = { Text("Digite sua pergunta...") },
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onMessageSent, enabled = userInput.isNotBlank()) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Enviar mensagem",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}