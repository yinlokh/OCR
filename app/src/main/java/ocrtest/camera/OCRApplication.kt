package ocrtest.camera

import android.app.Application
import com.facebook.stetho.Stetho

/**
 * The application
 */
class OCRApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
    }
}