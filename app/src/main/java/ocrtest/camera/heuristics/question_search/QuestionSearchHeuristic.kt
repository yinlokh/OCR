package ocrtest.camera.heuristics.question_search

import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.services.GoogleSearchService
import okhttp3.ResponseBody

/**
 * Heuristic to generate histogram of answer appearance
 */
class QuestionSearchHeuristic(val service: GoogleSearchService?) : Heuristic {

    val EMPTY = HeuristicOutput(ImmutableMap.of<String, Int>())

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val longestUniqueWords = selectUniqueLongestSubstring(input.answers)
        if (service == null || longestUniqueWords.contains(null)) {
            return Observable.just(EMPTY)
        }

        return service.search(input.question)
                .subscribeOn(Schedulers.computation())
                .map(object : Function<ResponseBody, HeuristicOutput>{
                    override fun apply(body: ResponseBody): HeuristicOutput {
                        val lowerCase = body.string().toLowerCase()
                        return HeuristicOutput(ImmutableMap.copyOf(
                                longestUniqueWords.filterNotNull().associateBy(
                                    {it : String -> it},
                                    {it -> lowerCase.split(it).size - 1})))
                    }
                })
    }

    /**
     * Answers may contain words separated by spaces, in which case these words may not ever be
     * a full substring inside search results.  Instead a better approach is to extract the longest
     * unique word from the answer to use as an indicator.  e.g. Mt Evererst will result in unique
     * substring of "everest"
     */
    private fun selectUniqueLongestSubstring(answers: List<String>): List<String?> {
        // first create histogram of substrings after splitting using spaces
        val answersAsWords = answers.map { answer ->
            answer.toLowerCase()
                    .split(" ")
                    .distinct()
        }

        val histogram = HashMap<String, Int>()
        answersAsWords.flatten().forEach({ word -> histogram.put(word, histogram.get(word) ?: 0 + 1) })

        return answersAsWords.map { words ->
            words
                    .sortedByDescending { word -> word.length }
                    .filter({ word -> histogram.get(word) ?: 0 == 1 })
                    .firstOrNull()
        }
    }
}