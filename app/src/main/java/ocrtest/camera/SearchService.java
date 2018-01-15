package ocrtest.camera;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Service for Google Search
 */

public interface SearchService {

    @GET("")
    Observable<ResponseBody> search(@Query("q") String query);
}
