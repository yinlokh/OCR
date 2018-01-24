package ocrtest.camera.ocr.tesseract

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.google.common.collect.ImmutableList
import com.googlecode.tesseract.android.TessBaseAPI
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.ocr.OCRStep
import ocrtest.camera.utils.RawTextToQuestion
import java.io.File
import java.io.FileOutputStream

/**
 * OCRStep to use Tesseract
 * (sample: http://www.thecodecity.com/2016/09/creating-ocr-android-app-using-tesseract.html)
 */
class TesseractOCRStep(val context: Context) : OCRStep {

    val tessbaseApi = TessBaseAPI()
    val datapath = Environment.getExternalStorageDirectory().toString()
    val tessDataPath = datapath + "/tessdata/"
    var initialized = false

    override fun performOCR(image: Bitmap): Observable<HeuristicInput>? {
        val rawTextToQuestion = RawTextToQuestion()
        return Observable.fromCallable { getText(image) }
                .subscribeOn(Schedulers.computation())
                .map{ text -> HeuristicInput(rawTextToQuestion.getQuestion(text))}
                .onErrorReturn { HeuristicInput("", ImmutableList.of()) }
    }

    private fun getText(image: Bitmap) : String {
        if (!initialized) {
            val assetManager = context.assets
            val instream = assetManager.open("tesseract/eng.traineddata")
            val dir = File(tessDataPath)
            val file = File(tessDataPath + "eng.traineddata")
            dir.mkdirs()
            val outstream = FileOutputStream(file.path)
            val buffer = ByteArray(2048)
            var read = instream.read(buffer);
            while (read != -1) {
                outstream.write(buffer, 0, read)
                read = instream.read(buffer)
            }
            tessbaseApi.init(datapath, "eng")
            initialized = true
        }

        tessbaseApi.setImage(image)
        return tessbaseApi.utF8Text
    }
}
