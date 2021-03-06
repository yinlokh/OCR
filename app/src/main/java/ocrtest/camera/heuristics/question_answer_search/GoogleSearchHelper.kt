package ocrtest.camera.heuristics.question_answer_search

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.services.GoogleSearchService
import java.util.regex.Pattern

/**
 * Helper to get number of search results given a query
 */
class GoogleSearchHelper(val searchService : GoogleSearchService) {

    // About 9,870,000,000 results
    private val PATTERN = Pattern.compile("About ([0-9,]+) results")

    fun getResultCount(query : String) : Observable<Double> {
        return searchService.search(query)
                .subscribeOn(Schedulers.computation())
                .map({responseBody -> extractMatchCount(responseBody.string())})
    }

    private fun extractMatchCount(page: String) : Double {
        val matcher = PATTERN.matcher(page)
        if (matcher.find()) {
            try {
                return Integer.parseInt(
                        matcher.group(0)
                                .filter { char -> (char >= '0' && char <= '9') }).toDouble()
            } catch (exception : NumberFormatException) {
                return Integer.MAX_VALUE.toDouble()
            }
        }
        return 0.0
    }
}
