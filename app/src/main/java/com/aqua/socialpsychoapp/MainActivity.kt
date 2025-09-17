package com.aqua.socialpsychoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import org.json.JSONObject
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var report by remember { mutableStateOf<ReportResponse?>(null) }

    LaunchedEffect(Unit) {
        ApiService.getReport(SessionManager.userId!!) { fetched ->
            report = fetched
        }
    }


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
                is Screen.Report -> report?.let {
                    ReportScreen(it)
                } ?: CircularProgressIndicator()

            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var input by remember { mutableStateOf("") }

    // ✅ Messages now come from ViewModel
    val messages = viewModel.messages

    LaunchedEffect(Unit) {
        SessionManager.userId?.let { uid ->
            SessionManager.userName?.let { uname ->
                ApiService.startSession(uid, uname, "en") { response ->
                    val msg = extractMessage(response)
                    val index = messages.size
//                    viewModel.addMessage("🤖 ")
//                    animateBotMessage(msg) { partial ->
//                        viewModel.updateMessage(index, "🤖 $partial")
//                    }
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
                        viewModel.addMessage(userMsg)
                        SessionManager.userId?.let { uid ->
                            ApiService.sendMessage(uid, input) { response ->
                                val msg = extractMessage(response)
                                val index = messages.size
                                viewModel.addMessage("🤖 ")
                                animateBotMessage(msg) { partial ->
                                    viewModel.updateMessage(index, "🤖 $partial")
                                }
                            }
                        }
                        input = ""
                    }
                }
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
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = parseBoldMessage(message), // ✅ parse here
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun animateBotMessage(
    fullMessage: String,
    onUpdate: (String) -> Unit
) {
    val words = fullMessage.split(" ")
    var current = ""
    kotlinx.coroutines.GlobalScope.launch {
        for (word in words) {
            current += if (current.isEmpty()) word else " $word"
            onUpdate(current)
            delay(100L) // typing speed (100ms per word)
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
fun ReportScreen(reportJson: String) {
    // ✅ Convert String -> JSONObject safely
    val jsonObject = remember(reportJson) {
        try {
            JSONObject(reportJson)
        } catch (e: Exception) {
            null
        }
    }

    if (jsonObject == null) {
        Text(
            text = "Invalid report format",
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Now safely extract fields
    val reportType = jsonObject.optString("report_type")
    val generatedDate = jsonObject.optString("generated_date")
    val patientId = jsonObject.optString("patient_id")

    val screeningTools = jsonObject.optJSONObject("screening_tools")
    val phq9 = screeningTools?.optJSONObject("PHQ-9")
    val gad7 = screeningTools?.optJSONObject("GAD-7")

    val phq9Score = phq9?.optString("score") ?: "N/A"
    val phq9Severity = phq9?.optString("severity") ?: "N/A"

    val gad7Score = gad7?.optString("score") ?: "N/A"
    val gad7Severity = gad7?.optString("severity") ?: "N/A"

    val recommendations = jsonObject.optJSONObject("recommendations")
    val actions = recommendations?.optString("immediate_actions") ?: "N/A"
    val followup = recommendations?.optString("followup_timeline") ?: "N/A"
    val referral = recommendations?.optBoolean("referral_needed") ?: false

    // 🎨 Show nicely styled report
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF9FAFB))
            .verticalScroll(rememberScrollState())
    ) {
        Text(reportType, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Text("Generated: $generatedDate", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Patient ID: $patientId", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("PHQ-9: $phq9Score ($phq9Severity)")
                Text("GAD-7: $gad7Score ($gad7Severity)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFFE3F2FD))) {
            Column(Modifier.padding(16.dp)) {
                Text("Recommendations", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                Spacer(Modifier.height(8.dp))
                Text("Actions: $actions")
                Text("Follow-up: $followup")
                Text("Referral Needed: ${if (referral) "Yes" else "No"}")
            }
        }
    }
}

fun extractMessage(response: String): String {
    return try {
        val jsonObj = JsonParser.parseString(response).asJsonObject
        jsonObj.get("message")?.asString ?: "⚠️ No message found"
    } catch (e: Exception) {
        "⚠️ Invalid response"
    }
}



fun parseBoldMessage(message: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var index = 0
    var isBold = false

    while (index < message.length) {
        val start = message.indexOf("**", index)
        if (start == -1) {
            // no more **
            builder.append(message.substring(index))
            break
        }

        // append text before **
        builder.append(message.substring(index, start))

        val end = message.indexOf("**", start + 2)
        if (end == -1) {
            // unmatched **
            builder.append(message.substring(start))
            break
        }

        // text inside **
        val boldText = message.substring(start + 2, end)
        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        builder.append(boldText)
        builder.pop()

        index = end + 2
    }

    return builder.toAnnotatedString()
}
