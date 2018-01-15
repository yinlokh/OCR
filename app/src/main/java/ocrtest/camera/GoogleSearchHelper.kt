package ocrtest.camera

import android.os.PatternMatcher
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper to get number of search results given a query
 */
class GoogleSearchHelper(val searchService : SearchService) {

    // About 9,870,000,000 results
    val PATTERN = Pattern.compile("About ([0-9,]+) results")

    fun getResultCount(query : String) : Observable<Int> {
        return searchService.search(query)
                .subscribeOn(Schedulers.computation())
                .map({responseBody -> extractMatchCount(responseBody.string())})
    }

    fun extractMatchCount(page: String) : Int {
        val matcher = PATTERN.matcher(page)
        if (matcher.find()) {
            return Integer.parseInt(
                    matcher.group(0)
                            .filter { char -> (char >= '0' && char <= '9') })
        }
        return 0
    }
}
