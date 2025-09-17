package com.aqua.socialpsychoapp

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class ChatViewModel : ViewModel() {
    // Holds chat messages
    private val _messages = mutableStateListOf("👋 Welcome! Starting session...")
    val messages: SnapshotStateList<String> = _messages

    fun addMessage(message: String) {
        _messages.add(message)
    }

    fun updateMessage(index: Int, message: String) {
        if (index in _messages.indices) {
            _messages[index] = message
        }
    }
}
