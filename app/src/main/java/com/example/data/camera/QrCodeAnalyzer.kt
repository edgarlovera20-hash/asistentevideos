package com.example.data.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Real on-device QR detection (CameraX ImageAnalysis + ML Kit) — replaces the old
 * simulated "laser scan" that never touched the camera.
 */
class QrCodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastDetected: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull {
                    it.valueType == Barcode.TYPE_URL || it.valueType == Barcode.TYPE_TEXT
                }?.rawValue ?: barcodes.firstOrNull()?.rawValue

                if (!value.isNullOrBlank() && value != lastDetected) {
                    lastDetected = value
                    onQrDetected(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
