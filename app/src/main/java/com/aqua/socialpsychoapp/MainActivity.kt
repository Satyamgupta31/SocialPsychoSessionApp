package com.aqua.socialpsychoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

sealed class Screen(val title: String) {
    object Chat : Screen("Chat")
    object Analytics : Screen("Analytics")
    object Report : Screen("Report")
}

@Composable
fun MainScreen() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }

    if (!isLoggedIn) {
        LoginScreen(onLogin = { isLoggedIn = true })
    } else {
        BottomNavApp()
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var userId by rememberSaveable { mutableStateOf("") }
    var userName by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧑 Enter your details", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("User Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (userId.isNotBlank() && userName.isNotBlank()) {
                    SessionManager.userId = userId
                    SessionManager.userName = userName
                    onLogin()
                } else {
                    error = "⚠️ Please enter both fields"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Session")
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun BottomNavApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.Chat,
                    onClick = { currentScreen = Screen.Chat },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Chat") },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Analytics,
                    onClick = { currentScreen = Screen.Analytics },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Report,
                    onClick = { currentScreen = Screen.Report },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Report") },
                    label = { Text("Report") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                is Screen.Chat -> ChatScreen()
                is Screen.Analytics -> AnalyticsScreen()
                is Screen.Report -> ReportScreen()
            }
        }
    }
}

@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf("👋 Welcome! Starting session...")) }
    var input by remember { mutableStateOf("") }

    // Start session once
    LaunchedEffect(Unit) {
        SessionManager.userId?.let { uid ->
            SessionManager.userName?.let { uname ->
                ApiService.startSession(uid, uname, "en") { response ->
                    messages = messages + "🤖 $response"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                val isUser = msg.startsWith("🧑")
                ChatBubble(message = msg, isUser = isUser)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        val userMsg = "🧑 $input"
                        messages = messages + userMsg
                        SessionManager.userId?.let { uid ->
                            ApiService.sendMessage(uid, input) { response ->
                                messages = messages + "🤖 $response"
                            }
                        }
                        input = ""
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .widthIn(max = 280.dp) // keeps bubbles from stretching full width
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AnalyticsScreen() {
    var analytics by remember { mutableStateOf("Loading analytics...") }

    LaunchedEffect(Unit) {
        SessionManager.userId?.let { uid ->
            ApiService.getAnalytics(uid) { response ->
                analytics = response
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 Session Analytics", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                analytics,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun ReportScreen() {
    var report by remember { mutableStateOf("Loading report...") }

    LaunchedEffect(Unit) {
        SessionManager.userId?.let { uid ->
            ApiService.getReport(uid) { response ->
                report = response
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📑 Clinical Report", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(report)
    }
}
