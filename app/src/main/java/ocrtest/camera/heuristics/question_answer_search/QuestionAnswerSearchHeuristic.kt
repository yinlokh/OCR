package ocrtest.camera.heuristics.question_answer_search

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.heuristics.ResultsMerger
import ocrtest.camera.services.GoogleSearchService
import ocrtest.camera.utils.ConsoleLogStream

/**
 * A heuristic which ranks answers based on matches from a Google search query combining the
 * question and an answer choice.
 */
class QuestionAnswerSearchHeuristic(
        val service: GoogleSearchService?,
        val consoleLogStream: ConsoleLogStream) : Heuristic {

    val EMPTY = HeuristicOutput(ImmutableMap.of<String, Double>())

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        if (service == null) {
            return Observable.just(EMPTY)
        }

        val helper = GoogleSearchHelper(service)
        val searchObservables : List<ObservableSource<Double>> = input.answers.map{
            answer -> helper.getResultCount(input.question + " " + answer)}
        return Observable.combineLatest<Double, HeuristicOutput>(
                searchObservables,
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
                            consoleLogStream.write("Question+Answer search failed")
                            return HeuristicOutput(ImmutableMap.of<String, Double>())
                        }

                        val resultMerger = ResultsMerger()
                        val results = resultMerger.mergeResults(scores, input)
                        consoleLogStream.write("Question+Answer: \n" + results.scores )
                        return results
                    }
                })
    }
}