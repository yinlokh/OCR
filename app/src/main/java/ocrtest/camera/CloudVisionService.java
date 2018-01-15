package ocrtest.camera;

import io.reactivex.Observable;
import ocrtest.camera.models.CloudVisionRequests;
import ocrtest.camera.models.CloudVisionResponses;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Service to call into Google Cloud Vision
 */

public interface CloudVisionService {

    @POST("v1/images:annotate")
    Observable<CloudVisionResponses> annotate(
            @Query("key") String key,
            @Body CloudVisionRequests requests);
}
