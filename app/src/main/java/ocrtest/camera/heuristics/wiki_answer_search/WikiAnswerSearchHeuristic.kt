package ocrtest.camera.heuristics.wiki_answer_search

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.heuristics.ResultsMerger
import ocrtest.camera.services.WikipediaSearchService
import ocrtest.camera.utils.CommonWords
import ocrtest.camera.utils.ConsoleLogStream
import ocrtest.camera.utils.KeywordExtractor
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
                ?.map { response -> wordcounter.countWords(response.string().toLowerCase(), keywords) }
                ?.onErrorReturn { 0.0 }
                ?.subscribeOn(Schedulers.computation())}
        return Observable.combineLatest(searches,
                object : Function<Array<Any>, HeuristicOutput> {
                    override fun apply(t: Array<Any>): HeuristicOutput {
                        val builder = ImmutableList.builder<Double>()
                        for (item in t) {
                            if (item is Double) {
                                builder.add(item)
                            }
                        }
                        val scores = builder.build()
                        if (scores.size != input.answers.size) {
                            consoleLogStream.write("WikiAnswer search failed")
                            return HeuristicOutput(ImmutableMap.of<String, Double>())
                        }

                        val resultsMerger = ResultsMerger()

                        val results = resultsMerger.mergeResults(scores, input)
                        consoleLogStream.write("WikiAnswer Search: \n Matching keywords "
                                + keywords + "\n\nResults: \n"+ results.scores )
                        return results
                    }
                })
    }

    private fun getKeywordsFromQuestion(question: String): List<String> {
        val commonWords = CommonWords()
        val keywordExtractor = KeywordExtractor()
        val keywords = keywordExtractor.extractKeywords(question)
                .map { word -> word.replace("\"", "").toLowerCase() }
        if (keywords.isEmpty()) {
            return question.replace('?', ' ')
                    .toLowerCase()
                    .split(" "
                    ).filter { word -> word.length > 2 && !commonWords.WORD_SET.contains(word) }
        }
        return keywords
    }
}