package com.woocommerce.android.ui.orders.creation

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.woocommerce.android.ui.barcodescanner.MediaImageProvider
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class GoogleMLKitCodeScanner @Inject constructor(
    private val barcodeScanner: BarcodeScanner,
    private val errorMapper: GoogleCodeScannerErrorMapper,
    private val barcodeFormatMapper: GoogleBarcodeFormatMapper,
    private val inputImageProvider: MediaImageProvider,
) : CodeScanner {
    private var barcodeFound = false
    @androidx.camera.core.ExperimentalGetImage
    override fun startScan(imageProxy: ImageProxy): Flow<CodeScannerStatus> {
        return callbackFlow {
            val barcodeTask = barcodeScanner.process(inputImageProvider.provideImage(imageProxy))
            barcodeTask.addOnCompleteListener {
                // We must call image.close() on received images when finished using them.
                // Otherwise, new images may not be received or the camera may stall.
                imageProxy.close()
            }
            barcodeTask.addOnSuccessListener { barcodeList ->
                // The check for barcodeFound is done because the startScan method will be called
                // continuously by the library as long as we are in the scanning screen.
                // There will be a good chance that the same barcode gets identified multiple times and as a result
                // success callback will be called multiple times.
                if (!barcodeList.isNullOrEmpty() && !barcodeFound) {
                    barcodeFound = true
                    handleScanSuccess(barcodeList.firstOrNull())
                    this@callbackFlow.close()
                }
            }
            barcodeTask.addOnFailureListener { exception ->
                this@callbackFlow.trySend(
                    CodeScannerStatus.Failure(
                        error = exception.message,
                        type = errorMapper.mapGoogleMLKitScanningErrors(exception)
                    )
                )
                this@callbackFlow.close()
            }

            awaitClose()
        }
    }

    private fun ProducerScope<CodeScannerStatus>.handleScanSuccess(code: Barcode?) {
        code?.rawValue?.let {
            trySend(
                CodeScannerStatus.Success(
                    it,
                    barcodeFormatMapper.mapBarcodeFormat(code.format)
                )
            )
        } ?: run {
            trySend(
                CodeScannerStatus.Failure(
                    error = "Failed to find a valid raw value!",
                    type = CodeScanningErrorType.Other(Throwable("Empty raw value"))
                )
            )
        }
    }
}
