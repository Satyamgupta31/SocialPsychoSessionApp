package com.aqua.socialpsychoapp

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.os.Handler
import android.os.Looper

object SessionManager {
    var userId: String? = null
    var userName: String? = null
    var language: String = "en" // default language
}

data class ReportResponse(
    val report_type: String,
    val generated_date: String,
    val patient_id: String,
    val screening_tools: Map<String, ScreeningTool>,
    val clinical_impression: String,
    val recommendations: Recommendations,
    val crisis_risk: String
)

data class ScreeningTool(
    val score: String,
    val severity: String,
    val suicidal_ideation: String? = null
)

data class Recommendations(
    val immediate_actions: String,
    val referral_needed: Boolean,
    val followup_timeline: String
)


object ApiService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val BASE_URL = "https://652e690d0426.ngrok-free.app" // change when testing on device

    // 🔹 Utility: Run callback on Main thread
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun runOnMain(callback: (String) -> Unit, response: String) {
        mainHandler.post { callback(response) }
    }

    fun startSession(userId: String, name: String, language: String, callback: (String) -> Unit) {
        val json = gson.toJson(mapOf("user_id" to userId, "name" to name, "language" to language))
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/start")
            .post(body)
            .build()

        client.newCall(request).enqueue(callbackHandler(callback))
    }

    fun sendMessage(userId: String, message: String, callback: (String) -> Unit) {
        val json = gson.toJson(mapOf("user_id" to userId, "message" to message))
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/message")
            .post(body)
            .build()

        client.newCall(request).enqueue(callbackHandler(callback))
    }

    fun getAnalytics(userId: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/analytics/$userId")
            .get()
            .build()

        client.newCall(request).enqueue(callbackHandler(callback))
    }

//    fun getReport(userId: String, callback: (String) -> Unit) {
//        val request = Request.Builder()
//            .url("$BASE_URL/report/$userId")
//            .get()
//            .build()
//
//        client.newCall(request).enqueue(callbackHandler(callback))
//    }
    fun getReport(userId: String, callback: (ReportResponse?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/report/$userId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain(callback, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                val report = try {
                    gson.fromJson(bodyString, ReportResponse::class.java)
                } catch (e: Exception) {
                    null
                }
                runOnMain(callback, report)
            }
        })
    }

    // for ReportResponse callbacks
    private fun runOnMain(callback: (ReportResponse?) -> Unit, response: ReportResponse?) {
        mainHandler.post { callback(response) }
    }


    private fun callbackHandler(callback: (String) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain(callback, "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string() ?: "No response"
                runOnMain(callback, bodyString)
            }
        }
    }
}

