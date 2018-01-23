package ocrtest.camera.heuristics.question_search

import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.heuristics.ResultsMerger
import ocrtest.camera.services.GoogleSearchService
import ocrtest.camera.utils.CommonWords
import ocrtest.camera.utils.ConsoleLogStream
import ocrtest.camera.utils.OccurranceCounter
import okhttp3.ResponseBody

/**
 * Heuristic to generate histogram of answer appearance
 */
class QuestionSearchHeuristic(
        val service: GoogleSearchService?,
        val consoleLogStream: ConsoleLogStream) : Heuristic {

    val EMPTY = HeuristicOutput(ImmutableMap.of<String, Double>())

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val longestUniqueWords = selectUniqueLongestSubstrings(input.answers)
        val counter = OccurranceCounter()
        if (service == null || longestUniqueWords.contains(null)) {
            consoleLogStream.write("QuestionSearch failed.")
            return Observable.just(EMPTY)
        }

        val answerToWords = input.answers.withIndex().associateBy (
                {indexedValue -> indexedValue.value},
                {indexedValue -> longestUniqueWords.get(indexedValue.index)})

        return service.search(input.question)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(object : Function<ResponseBody, HeuristicOutput>{
                    override fun apply(body: ResponseBody): HeuristicOutput {
                        val lowerCase = body.string().toLowerCase()
                        val scores = input.answers
                                .map{it -> counter.countOccurrances(lowerCase, answerToWords.get(it))}
                        val resultMerger = ResultsMerger()
                        val results = resultMerger.mergeResults(scores, input)
                        consoleLogStream.write((
                                "QuestionOnly Search (Terms: "
                                        + answerToWords
                                        + ")\n\nQuestionOnly Search Results:"
                                        + "\n" + results.scores))
                        return results
                    }
                })
    }

    /**
     * Answers may contain words separated by spaces, in which case these words may not ever be
     * a full substring inside search results.  Instead a better approach is to extract the longest
     * unique word from the answer to use as an indicator.  e.g. Mt Evererst will result in unique
     * substring of "everest"
     */
    private fun selectUniqueLongestSubstrings(answers: List<String>): List<Set<String>?> {
        val answersAsWords = answers.map { answer ->
            answer.toLowerCase()
                    .split(" ")
                    .distinct()
        }

        val histogram = HashMap<String, Int>()
        val commonwords = CommonWords()
        answersAsWords.flatten().forEach({ word -> histogram.put(word, histogram.get(word) ?: 0 + 1) })

        return answersAsWords.map { words ->
            words
                    .sortedByDescending { word -> word.length }
                    .filter({ word -> histogram.get(word) ?: 0 == 1 })
                    .filter({ word -> !commonwords.WORD_SET.contains(word)})
                    .take(3)
                    .toSet()
        }
    }
}