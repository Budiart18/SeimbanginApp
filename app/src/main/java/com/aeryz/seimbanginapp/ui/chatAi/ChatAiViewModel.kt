package com.aeryz.seimbanginapp.ui.chatAi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeryz.seimbanginapp.data.repository.GeminiAiRepository
import com.aeryz.seimbanginapp.utils.ResultWrapper
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.launch

class ChatAiViewModel(private val repository: GeminiAiRepository) : ViewModel() {

    private val _chatResult = MutableLiveData<ResultWrapper<List<Content>>>()
    val chatResult: LiveData<ResultWrapper<List<Content>>> get() = _chatResult

    fun sendChat(prompt: String) {
        viewModelScope.launch {
            repository.sendPrompt(prompt).collect {
                _chatResult.postValue(it)
            }
        }
    }

    fun deleteChatHistory() {
        repository.deleteChatHistory()
    }
}
