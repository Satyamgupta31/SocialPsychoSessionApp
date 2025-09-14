package com.aqua.socialpsychoapp

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

object SessionManager {
    var userId: String? = null
    var userName: String? = null
}

object ApiService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val BASE_URL = "https://2428ed270c1b.ngrok-free.app" // Use your PC's LAN IP for real device

    fun startSession(userId: String, name: String, language: String, callback: (String) -> Unit) {
        val json = gson.toJson(mapOf("user_id" to userId, "name" to name, "language" to language))
        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url("$BASE_URL/start")
            .post(body)
            .build()

        client.newCall(request).enqueue(callbackHandler(callback))
    }

    fun sendMessage(userId: String, message: String, callback: (String) -> Unit) {
        val json = gson.toJson(mapOf("user_id" to userId, "message" to message))
        val body = RequestBody.create("application/json".toMediaType(), json)

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

    fun getReport(userId: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/report/$userId")
            .get()
            .build()

        client.newCall(request).enqueue(callbackHandler(callback))
    }

    private fun callbackHandler(callback: (String) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string() ?: "No response")
            }
        }
    }
}