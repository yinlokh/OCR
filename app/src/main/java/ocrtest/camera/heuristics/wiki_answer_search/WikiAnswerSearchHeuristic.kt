package ocrtest.camera.heuristics.wiki_answer_search

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.functions.Function
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.services.WikipediaSearchService
import ocrtest.camera.utils.CommonWords
import ocrtest.camera.utils.ConsoleLogStream
import ocrtest.camera.utils.WordCounter

/**
 * Heuristic to look up each answer individually to check how many question key words match.
 */
class WikiAnswerSearchHeuristic(
        val wikipediaSearchService: WikipediaSearchService?,
        val consoleLogStream: ConsoleLogStream) : Heuristic {
    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val keywords = getKeywordsFromQuestion(input.question)
        val wordcounter = WordCounter()
        val searches = input.answers.map {
            answer -> wikipediaSearchService?.search(answer)
                ?.map { response -> wordcounter.countWords(response.string(), keywords) } }
        return Observable.combineLatest(searches,
                object : Function<Array<Any>, HeuristicOutput> {
                    override fun apply(t: Array<Any>): HeuristicOutput {
                        val builder = ImmutableList.builder<Int>()
                        for (item in t) {
                            if (item is Int) {
                                builder.add(item)
                            }
                        }
                        val scores = builder.build()
                        if (scores.size != input.answers.size) {
                            consoleLogStream.write("WikiAnswer search failed")
                            consoleLogStream.writeDivider()
                            return HeuristicOutput(ImmutableMap.of<String, Int>())
                        }

                        val results = input.answers
                                .withIndex()
                                .associateBy({it.value}, {scores[it.index]})
                        consoleLogStream.write("WikiAnswerr: \n Matching keywords "
                                + keywords + "\nResults: \n"+ results )
                        consoleLogStream.writeDivider()
                        return HeuristicOutput(ImmutableMap.copyOf(results))
                    }
                })
    }

    private fun getKeywordsFromQuestion(question: String): List<String> {
        val commonWords = CommonWords()
        return question
                .split(" ")
                .filter { word -> !commonWords.WORD_SET.contains(word) }
    }
}