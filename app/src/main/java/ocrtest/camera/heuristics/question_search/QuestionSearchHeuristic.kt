package ocrtest.camera.heuristics.question_search

import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.services.GoogleSearchService
import ocrtest.camera.utils.ConsoleLogStream
import okhttp3.ResponseBody

/**
 * Heuristic to generate histogram of answer appearance
 */
class QuestionSearchHeuristic(
        val service: GoogleSearchService?,
        val consoleLogStream: ConsoleLogStream) : Heuristic {

    val EMPTY = HeuristicOutput(ImmutableMap.of<String, Int>())

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val longestUniqueWords = selectUniqueLongestSubstring(input.answers)
        if (service == null || longestUniqueWords.contains(null)) {
            consoleLogStream.write("QuestionSearch failed.")
            consoleLogStream.writeDivider()
            return Observable.just(EMPTY)
        }

        val answerToWords = input.answers.withIndex().associateBy (
                {indexedValue -> indexedValue.value},
                {indexedValue -> longestUniqueWords.get(indexedValue.index)})

        return service.search(input.question)
                .subscribeOn(Schedulers.computation())
                .map(object : Function<ResponseBody, HeuristicOutput>{
                    override fun apply(body: ResponseBody): HeuristicOutput {
                        val lowerCase = body.string().toLowerCase()
                        val results = input.answers.associateBy(
                                {it -> it},
                                {it -> lowerCase.split(answerToWords.get(it)?:"").size - 1})
                        consoleLogStream.write("question search results: " + results)
                        consoleLogStream.writeDivider()
                        return HeuristicOutput(ImmutableMap.copyOf(results))
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