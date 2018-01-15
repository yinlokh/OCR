package ocrtest.camera.utils

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Stream to provide information to console.
 */
class ConsoleLogStream(private val stream: BehaviorSubject<String> = BehaviorSubject.create()) {

    fun logs() : Observable<String> = stream.hide()

    fun write(log: String) = stream.onNext(log)

    fun writeDivider() = stream.onNext("===========================")
}
