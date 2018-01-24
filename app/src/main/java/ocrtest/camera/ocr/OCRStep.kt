package ocrtest.camera.ocr

import android.graphics.Bitmap
import io.reactivex.Observable
import ocrtest.camera.heuristics.HeuristicInput

/**
 * Interface to enable swapping different OCR engines.
 */
interface OCRStep {

    fun performOCR(image: Bitmap) : Observable<HeuristicInput>?
}