package com.aqua.socialpsychoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqua.socialpsychoapp.ReportResponse
import com.aqua.socialpsychoapp.ScreeningTool
import com.aqua.socialpsychoapp.Recommendations

@Composable
fun ReportScreen(report: ReportResponse) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // 🏷️ Header
        Text(
            text = report.report_type,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Generated: ${report.generated_date}",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 👤 Patient Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Patient ID: ${report.patient_id}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Clinical Impression: ${report.clinical_impression.uppercase()}")
                Text("Crisis Risk: ${report.crisis_risk}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Screening Tools
        Text("Screening Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        report.screening_tools.forEach { (toolName, tool) ->
            ScreeningToolCard(toolName, tool)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 💡 Recommendations
        Text("Recommendations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        RecommendationCard(report.recommendations)
    }
}

@Composable
fun ScreeningToolCard(name: String, tool: ScreeningTool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Score: ${tool.score}")
            Text("Severity: ${tool.severity}")
            tool.suicidal_ideation?.let {
                Text("Suicidal Ideation: $it")
            }
        }
    }
}

@Composable
fun RecommendationCard(recommendations: Recommendations) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Immediate Actions", fontWeight = FontWeight.Bold)
            Text(recommendations.immediate_actions, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text("Follow-up Timeline: ${recommendations.followup_timeline}")
            Text("Referral Needed: ${if (recommendations.referral_needed) "Yes" else "No"}")
        }
    }
}
