package com.aeryz.seimbanginapp.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aeryz.seimbanginapp.BuildConfig
import com.aeryz.seimbanginapp.data.network.model.ocr.OcrResponse
import com.aeryz.seimbanginapp.data.network.model.profile.FinanceProfile
import com.aeryz.seimbanginapp.model.TransactionItem
import com.aeryz.seimbanginapp.utils.ResultWrapper
import com.aeryz.seimbanginapp.utils.proceedFlow
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

class GeminiAiRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API
    )
    private val ocrModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API,
        generationConfig = GenerationConfig.builder().apply {
            responseMimeType = "application/json"
        }.build()
    )
    private val gson = Gson()
    private val chatHistory = mutableListOf<Content>()
    private val chat = generativeModel.startChat(chatHistory)

    fun sendPrompt(prompt: String): Flow<ResultWrapper<List<Content>>> {
        val result = proceedFlow {
            val userContent = content(role = "user") { text(prompt) }
            chatHistory.add(userContent)
            val response = chat.sendMessage(prompt)
            val responseText = response.text ?: throw Exception("Response text is null")
            val modelContent = content(role = "model") { text(responseText) }
            chatHistory.add(modelContent)
            chatHistory.toList()
        }
        return result
    }

    fun deleteChatHistory() {
        chatHistory.clear()
    }

    fun generateFinancialAdvice(profile: FinanceProfile): Flow<ResultWrapper<String>> {
        val result = proceedFlow {
            val prompt = buildString {
                appendLine("Berikan saran finansial berdasarkan profil berikut:")
                appendLine("Pendapatan bulanan: Rp${profile.monthlyIncome}")
                appendLine("Tabungan saat ini: Rp${profile.currentSavings}")
                appendLine("Utang: Rp${profile.debt}")
                appendLine("Tujuan finansial: ${profile.financialGoals}")
                appendLine("Tingkat toleransi risiko: ${profile.riskManagement?.uppercase()}")
                appendLine("Berikan saran dalam bahasa Indonesia yang mudah dipahami dan secara singkat. Maksimal 80 kata")
            }
            val response = chat.sendMessage(prompt)
            response.text ?: throw Exception("Response text is null")
        }
        return result
    }

    fun generateOcrTransaction(image: Bitmap): Flow<ResultWrapper<OcrResponse>> {
        return proceedFlow {
            val prompt = """
            Analyze the provided receipt image. Act as a receipt scanner.
            Extract all purchased items.
            Format the output as a single, minified JSON object that strictly follows this structure.
            Do not include any text before or after the JSON object.
            
            For the "category" field, you must choose one of these exact values: 
            "food", "transportation", "utilities", "entertainment", "shopping", "healthcare", "education", "others".

            The JSON structure must be:
            {
              "status": "success",
              "code": 200,
              "message": "Success",
              "data": {
                "items": [
                  {
                    "id": null,
                    "item_name": "Product Name",
                    "category": "Product Category (best guess)",
                    "price": Unit price as an integer (without currency symbols or commas),
                    "quantity": Quantity as an integer (default to 1 if not specified),
                    "subtotal": Total price for this line as an integer (without currency symbols or commas)
                  }
                ]
              }
            }
            
            If a value is not available, use a reasonable default or null.
            """.trimIndent()
            val inputContent = content {
                image(image)
                text(prompt)
            }
            val response = ocrModel.generateContent(inputContent)
            val responseText = response.text
            if (responseText.isNullOrBlank()) {
                throw Exception("Received an empty response from the API.")
            }
            val ocrResponse = gson.fromJson(responseText, OcrResponse::class.java)
            val originalItems = ocrResponse.data?.items
            if (ocrResponse.status != "success" || originalItems.isNullOrEmpty()) {
                throw Exception("Failed to extract items from the receipt or response status was not success.")
            }
            val updatedItems = originalItems.map { product ->
                product.copy(id = (1000000..9999999).random())
            }
            ocrResponse.copy(data = ocrResponse.data.copy(items = updatedItems))
        }

    }
}
