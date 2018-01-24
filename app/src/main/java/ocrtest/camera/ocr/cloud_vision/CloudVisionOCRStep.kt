package ocrtest.camera.ocr.cloud_vision

import android.graphics.Bitmap
import android.util.Base64
import com.google.common.collect.ImmutableList
import io.reactivex.Observable
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.models.CloudVisionFeatures
import ocrtest.camera.models.CloudVisionImage
import ocrtest.camera.models.CloudVisionRequest
import ocrtest.camera.models.CloudVisionRequests
import ocrtest.camera.ocr.OCRStep
import ocrtest.camera.services.CloudVisionService
import java.io.ByteArrayOutputStream

/**
 * OCRStep to use Google Cloud Vision Api
 */
class CloudVisionOCRStep(val cloudVisionService: CloudVisionService?) : OCRStep {

    val GOOGLE_VISION_API_KEY = ""

    override fun performOCR(image: Bitmap): Observable<HeuristicInput>? {
        var stream = ByteArrayOutputStream()
        image?.compress(Bitmap.CompressFormat.WEBP, 50, stream)
        val request = CloudVisionRequest(
                CloudVisionImage(Base64.encodeToString(stream.toByteArray(), 0)),
                CloudVisionFeatures("TEXT_DETECTION", "10")
        )
        val requests = CloudVisionRequests(
                ImmutableList.builder<CloudVisionRequest>().add(request).build())
        return cloudVisionService?.annotate(GOOGLE_VISION_API_KEY, requests)
                ?.map { responses -> HeuristicInput(responses.toTriviaQuestion()) }
    }
}