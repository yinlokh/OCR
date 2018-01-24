package ocrtest.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CaptureRequest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.common.collect.ImmutableList
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.heuristics.combination.CombinationHeuristic
import ocrtest.camera.heuristics.google_answer_search.GoogleAnswerSearchHeuristic
import ocrtest.camera.heuristics.question_answer_search.QuestionAnswerSearchHeuristic
import ocrtest.camera.heuristics.question_search.QuestionSearchHeuristic
import ocrtest.camera.heuristics.wiki_answer_search.WikiAnswerSearchHeuristic
import ocrtest.camera.heuristics.wiki_question_search.WikiQuestionSearchHeuristic
import ocrtest.camera.ocr.cloud_vision.CloudVisionOCRStep
import ocrtest.camera.ocr.tesseract.TesseractOCRStep
import ocrtest.camera.services.CloudVisionService
import ocrtest.camera.services.GoogleSearchService
import ocrtest.camera.services.WikipediaSearchService
import ocrtest.camera.utils.ConsoleLogStream
import ocrtest.camera.widgets.BoundingBoxView
import ocrtest.camera.widgets.TabbedConsoleView
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Written with sample from
 * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
 */
class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    val GOOGLE_SEARCH_BASE_URL = "http://www.google.com"
    val GOOGLE_VISION_BASE_URL = "https://vision.googleapis.com"
    val WIKIPEDIA_SEARCH_BASE_URL = "https://en.wikipedia.org"
    val PAGE_ANALYSIS = "Analysis"
    val PAGE_QUESTION = "?"
    val PAGE_ANSWER_1 = "A1"
    val PAGE_ANSWER_2 = "A2"
    val PAGE_ANSWER_3 = "A3"

    var boundingBoxView: BoundingBoxView? = null
    var captureButton : Button? = null
    var cloudVisionService : CloudVisionService? = null
    var consoleButton: Button? = null
    var previewSurface : TextureView? = null
    var cameraManager : CameraManager? = null
    var camera : CameraDevice? = null
    var googleSearchService: GoogleSearchService? = null
    var consoleLogs : ConsoleLogStream = ConsoleLogStream()
    var wikipediaSearchService : WikipediaSearchService? = null
    var tabbedConsole : TabbedConsoleView? = null
    val ocrStep = CloudVisionOCRStep(cloudVisionService)
    val tesseract = TesseractOCRStep(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        boundingBoxView = findViewById(R.id.bounding_box)
        captureButton = findViewById(R.id.capture_button)
        consoleButton = findViewById(R.id.console_button)
        previewSurface = findViewById(R.id.preview_surface)
        previewSurface?.surfaceTextureListener = this
        tabbedConsole = TabbedConsoleView(this, ImmutableList.of(
                PAGE_ANALYSIS, PAGE_QUESTION, PAGE_ANSWER_1, PAGE_ANSWER_2, PAGE_ANSWER_3))

        var contentContainer : FrameLayout = findViewById(R.id.contentContainer)
        contentContainer.addView(tabbedConsole)
        tabbedConsole?.visibility = View.GONE

        var timeElapsed = 0;
        captureButton?.setOnClickListener {
            tabbedConsole?.clearLoadStates()
            tabbedConsole?.visibility = View.VISIBLE
            capture()
            timeElapsed = 0
        }
        consoleButton?.setOnClickListener {
            if (tabbedConsole?.visibility == View.VISIBLE) {
                tabbedConsole?.visibility = View.GONE
            } else {
                tabbedConsole?.visibility = View.VISIBLE
            }
        }

        Observable.interval(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    timer.setText("Time Elapsed: " + timeElapsed/1000 + " seconds")
                    if (timeElapsed > 7000) {
                        timer.setBackgroundColor(Color.RED)
                    } else if (timeElapsed > 5000) {
                        timer.setBackgroundColor(Color.YELLOW)
                    } else {
                        timer.setBackgroundColor(Color.WHITE)
                    }
                    timeElapsed+=300}
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cloudVisionService = getRetrofit(GOOGLE_VISION_BASE_URL)
                .create(CloudVisionService::class.java)
        googleSearchService = getRetrofit(GOOGLE_SEARCH_BASE_URL)
                .create(GoogleSearchService::class.java)
        wikipediaSearchService = getRetrofit(WIKIPEDIA_SEARCH_BASE_URL)
                .create(WikipediaSearchService::class.java)
        consoleLogs.logs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({log -> tabbedConsole
                        ?.getTextView(PAGE_ANALYSIS)
                        ?.append(log+"\n====================\n")})
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        closeCamera()
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        cameraOpen()
    }

    fun getRetrofit(baseUrl: String) : Retrofit {
        var okHttpClient = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
    }

    fun cameraOpen() {
        if (cameraManager == null) {
            return
        }

        try {
            for (cameraId in cameraManager!!.cameraIdList) {
                val characteristic = cameraManager!!.getCameraCharacteristics(cameraId)
                if (characteristic.get(LENS_FACING).equals(LENS_FACING_BACK)) {
                    cameraOpen(cameraId)
                    return
                }
            }
        } catch (e: Exception) {}
    }

    fun cameraOpen(id : String) : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            consoleLogs.write("Missing Camera Permission, Cannot open camera.")
            return false
        }
        cameraManager!!.openCamera(id, CameraCallback(this), null)
        return true
    }

    fun cameraOpened(camera: CameraDevice?) {
        this.camera = camera
        var surfaces = ArrayList<Surface>()
        var surface : Surface? = null
        if (previewSurface != null) {
            surface = Surface(previewSurface?.surfaceTexture)
            surfaces.add(surface)
        }

        previewSurface?.surfaceTexture?.setDefaultBufferSize(
                previewSurface?.width?:640, previewSurface?.height?:480)
        var previewRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        if (surface != null) {
            previewRequestBuilder?.addTarget(surface as? Surface)
        }

        consoleLogs.write("Creating capture session")
        camera?.createCaptureSession(surfaces, StateCallback(this, previewRequestBuilder), null)
    }

    fun cameraDisconnected() { }

    fun captureSessionConfigured(
            session: CameraCaptureSession?,
            previewRequestBuilder: CaptureRequest.Builder?) {
        var request = previewRequestBuilder?.build()
        if (request != null) {
            session?.setRepeatingRequest(
                    request,
                    object : CameraCaptureSession.CaptureCallback() {},
                    null)
        }
    }

    fun closeCamera() {
        camera?.close()
    }

    fun capture() {
        var startX : Int = boundingBoxView?.boxStartX?.toInt()?:0
        var startY : Int = boundingBoxView?.boxStartY?.toInt()?:0
        var endX : Int = boundingBoxView?.boxEndX?.toInt()?:0
        var endY : Int = boundingBoxView?.boxEndY?.toInt()?:0
        var bitmap = previewSurface?.getBitmap()
        if (startX != endX && startY != endY) {
            bitmap = Bitmap.createBitmap(
                    bitmap,
                    Math.min(startX, endX),
                    Math.min(startY, endY),
                    Math.abs(startX - endX),
                    Math.abs(startY - endY))
        }

        solveQuestion(bitmap)
        tabbedConsole?.getTextView(PAGE_ANALYSIS)?.setText("")
    }

    fun solveQuestion(image: Bitmap?) {
        if (image == null) {
            return;
        }

        ocrStep.performOCR(image)
                ?.doOnNext{ question -> loadDefinitionGooglePage(question)}
                ?.doOnNext{ question -> if (question.question.length == 0) consoleLogs.write("could not read question.")}
                ?.filter{ question -> question.question.length > 0}
                ?.subscribeOn(Schedulers.computation())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnNext{ response -> showOCRText(response)}
                ?.flatMap { response -> calculateHeuristics(response) }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                        {answer -> showResults(answer)})
    }

    fun loadDefinitionGooglePage(input: HeuristicInput) {
        val pages = ImmutableList.of(PAGE_ANSWER_1, PAGE_ANSWER_2, PAGE_ANSWER_3)
        input.answers.forEachIndexed { index, it ->
            loadGoogleQueryToPage("What is " + it, pages.get(index))
        }
        loadGoogleQueryToPage(input.question, PAGE_QUESTION)
    }

    fun loadGoogleQueryToPage(query: String, page: String) {
        val regex = Pattern.compile("\\A.*About[0-9, ]+results").toRegex()
        googleSearchService?.search(query)
                ?.subscribeOn(Schedulers.computation())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.map { response -> response.string().replace(regex, "") }
                ?.onErrorReturn { "Page load failed =(" }
                ?.subscribe{bodyText ->
                    tabbedConsole?.markPageLoaded(page)
                    tabbedConsole?.getTextView(page)?.setText(
                            Html.fromHtml(bodyText, Html.FROM_HTML_MODE_COMPACT))}
    }

    fun calculateHeuristics(input: HeuristicInput) : Observable<HeuristicOutput> {
        val answerSearch = GoogleAnswerSearchHeuristic(googleSearchService, consoleLogs)
        val questionAnswerSearch = QuestionAnswerSearchHeuristic(googleSearchService, consoleLogs)
        val questionSearch = QuestionSearchHeuristic(googleSearchService, consoleLogs)
        val wikiQuestionSearch = WikiQuestionSearchHeuristic(wikipediaSearchService, consoleLogs)
        val wikiAnswerSearch = WikiAnswerSearchHeuristic(wikipediaSearchService, consoleLogs)
        val heuristic = CombinationHeuristic(ImmutableList.of(
                answerSearch,
                questionSearch,
                questionAnswerSearch,
                wikiQuestionSearch,
                wikiAnswerSearch))
        return heuristic.compute(input)
    }

    fun showOCRText(question: HeuristicInput) {
        consoleLogs.write(
                "Question: "
                        + question.question
                        + "\nChoice 1: "
                        + question.answers[0]
                        + "\nChoice 2: "
                        + question.answers[1]
                        + "\nChoice 3: "
                        + question.answers[2])
    }

    fun showResults(output: HeuristicOutput) {
        val answers = output.scores.keys.sortedByDescending { it -> output.scores[it] }
        consoleLogs.write("Descending in likelihood: \n" + answers.toString())
        tabbedConsole?.markPageLoaded(PAGE_ANALYSIS)
    }

    class CameraCallback(var activity : MainActivity) : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice?) {
            activity.cameraOpened(camera)
        }

        override fun onDisconnected(camera: CameraDevice?) {
            activity.cameraDisconnected()
        }

        override fun onError(camera: CameraDevice?, error: Int) { }
    }

    class StateCallback(var activity: MainActivity, var requestBuilder : CaptureRequest.Builder?)
        : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession?) {
            activity.captureSessionConfigured(session, requestBuilder)
        }

        override fun onConfigureFailed(session: CameraCaptureSession?) { }
    }
}
