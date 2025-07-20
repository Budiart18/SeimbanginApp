package com.aeryz.seimbanginapp.ui.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeryz.seimbanginapp.data.network.model.ocr.OcrResponse
import com.aeryz.seimbanginapp.data.repository.GeminiAiRepository
import com.aeryz.seimbanginapp.utils.ResultWrapper
import kotlinx.coroutines.launch

class OcrViewModel(private val repository: GeminiAiRepository) : ViewModel() {
    private val _scanReceiptResult = MutableLiveData<ResultWrapper<OcrResponse>>()
    val scanReceiptResult: LiveData<ResultWrapper<OcrResponse>> = _scanReceiptResult

    private val _isUploadEnabled = MutableLiveData(false)
    val isUploadEnabled: LiveData<Boolean> = _isUploadEnabled

    var currentImageUri: Uri? = null
        set(value) {
            field = value
            _isUploadEnabled.postValue(value != null)
        }

    fun scanReceipt(context: Context) {
        val imageUri = currentImageUri ?: run {
            _scanReceiptResult.postValue(
                ResultWrapper.Error(Exception("Image URI is missing."))
            )
            return
        }
        viewModelScope.launch {
            try {
                val bitmap = uriToBitmap(context, imageUri)
                repository.generateOcrTransaction(bitmap).collect { result ->
                    _scanReceiptResult.postValue(result)
                }
            } catch (e: Exception) {
                _scanReceiptResult.postValue(ResultWrapper.Error(e))
            }
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }
}
