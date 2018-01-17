package ocrtest.camera.heuristics.wiki_question_search

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.heuristics.ResultsMerger
import ocrtest.camera.services.WikipediaSearchService
import ocrtest.camera.utils.ConsoleLogStream
import ocrtest.camera.utils.KeywordExtractor
import ocrtest.camera.utils.OccurranceCounter

/**
 * Heuristic for searching using keywords from question in wikipedia
 */
class WikiQuestionSearchHeuristic(
        val wikipediaSearchService: WikipediaSearchService?,
        val consoleLogStream: ConsoleLogStream) : Heuristic {

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val keywordExtractor = KeywordExtractor()
        val keywords = keywordExtractor.extractKeywords(input.question).map { keyword -> keyword.replace("\"", "") }
        val counter = OccurranceCounter()
        if (keywords.isEmpty()) {
            return Observable.just(HeuristicOutput(ImmutableMap.of()))
        }

        val searches : List<Observable<List<Double>>> =
                keywords.map { keyword -> wikipediaSearchService!!.search(keyword)
                        .subscribeOn(Schedulers.computation())
                        .map { body -> body.string() }
                        .map { content -> input.answers.map {
                            answer ->
                            counter.countOccurrances(
                                    content.toLowerCase(),
                                    answer.toLowerCase().split(" ").toSet()) } }
                        .onErrorReturn { input.answers.map { 0.0 } }}
        return Observable.combineLatest(searches, object: Function<Array<Any>, HeuristicOutput> {
            override fun apply(t: Array<Any>): HeuristicOutput {
                val resultsMerger = ResultsMerger()
                val totals = input.answers.mapIndexed { index, s ->
                    t.fold (0.0, {total, any -> (any as List<Double>).get(index)})}
                val results = resultsMerger.mergeResults(totals, input)
                consoleLogStream.write("WikiQuestionSearch: \nUsing question keywords "
                        + keywords
                        + "\n\nResults: \n"
                        + results.scores )
                return results
            }
        })
    }

}