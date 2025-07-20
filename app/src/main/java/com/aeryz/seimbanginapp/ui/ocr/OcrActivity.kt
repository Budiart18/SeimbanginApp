package com.aeryz.seimbanginapp.ui.ocr

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aeryz.seimbanginapp.R
import com.aeryz.seimbanginapp.databinding.ActivityOcrBinding
import com.aeryz.seimbanginapp.ui.transaction.createTransaction.CreateTransactionActivity
import com.aeryz.seimbanginapp.utils.exception.ApiException
import com.aeryz.seimbanginapp.utils.proceedWhen
import com.aeryz.seimbanginapp.utils.uriToFile
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.shashank.sony.fancytoastlib.FancyToast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.androidx.viewmodel.ext.android.viewModel

class OcrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding

    private val viewModel: OcrViewModel by viewModel()

    private var ocrMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolBar()
        setupClickListeners()
        observeScanReceiptResult()
    }

    private fun setupToolBar() {
        val toolbar = binding.toolBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.ocr_menu, menu)
        ocrMenuItem = menu?.findItem(R.id.action_show_example)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_example -> {
                showPopUpInfo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val resultUri = result.uriContent
            if (resultUri != null) {
                viewModel.currentImageUri = resultUri
                showImage()
            } else {
                Log.e("UCrop", "Crop operation failed")
            }
        }
    }

    private fun showImage() {
        binding.ivImagePreview.setImageURI(viewModel.currentImageUri)
    }

    private fun startCrop() {
        cropImage.launch(
            CropImageContractOptions(
                uri = viewModel.currentImageUri,
                CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    multiTouchEnabled = true,
                    toolbarColor = ContextCompat.getColor(this, R.color.primary_500)
                )
            )
        )
    }

    private fun uploadImage() {
        viewModel.currentImageUri?.let { uri ->
            val imageFile = uriToFile(uri, this)
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "photo",
                imageFile.name,
                requestImageFile
            )
            viewModel.scanReceipt(this)
        }
    }

    private fun observeScanReceiptResult() {
        viewModel.scanReceiptResult.observe(this) { resultWrapper ->
            resultWrapper.proceedWhen(
                doOnSuccess = {
                    showLoading(false)
                    showToast(getString(R.string.text_scan_receipt_success), FancyToast.SUCCESS)
                    val ocrData = it.payload?.data
                    Log.d("SCAN_RESULT", "observeScanReceiptResult: $ocrData")
                    if (ocrData != null) {
                        CreateTransactionActivity.startActivity(this, ocrData)
                    }
                },
                doOnLoading = {
                    showLoading(true)
                },
                doOnError = {
                    showLoading(false)
                    if (it.exception is ApiException) {
                        showToast(it.exception.getParsedError()?.message, FancyToast.ERROR)
                    }
                }
            )
        }
    }

    private fun setupClickListeners() {
        binding.btnChooseImage.setOnClickListener {
            viewModel.currentImageUri = null
            startCrop()
        }
        viewModel.isUploadEnabled.observe(this) { isEnabled ->
            binding.btnUpload.isEnabled = isEnabled
        }
        binding.btnUpload.setOnClickListener {
            uploadImage()
        }
    }

    private fun showToast(message: String?, type: Int) {
        FancyToast.makeText(this, message, FancyToast.LENGTH_SHORT, type, false).show()
    }

    private fun showLoading(isShowLoading: Boolean) {
        binding.pbLoading.isVisible = isShowLoading
        binding.btnChooseImage.isEnabled = !isShowLoading
        binding.btnUpload.isEnabled = !isShowLoading
    }

    private fun showPopUpInfo() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.layout_popup_dialog_receipt_example)
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams
        val button = dialog.findViewById<ImageView>(R.id.close)
        button.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
