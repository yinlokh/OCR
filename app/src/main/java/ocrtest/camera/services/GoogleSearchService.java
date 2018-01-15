package ocrtest.camera.services;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Service for Google Search
 */

public interface GoogleSearchService {

    @GET("search")
    Observable<ResponseBody> search(@Query("q") String query);
}
