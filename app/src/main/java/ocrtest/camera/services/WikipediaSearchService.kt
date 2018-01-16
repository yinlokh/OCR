package ocrtest.camera.services

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Service to fetch page from wikipedia
 */
interface WikipediaSearchService {

    @GET("wiki/{query}")
    fun search(@Path("query") query: String): Observable<ResponseBody>
}
