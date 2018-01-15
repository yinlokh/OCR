package ocrtest.camera.heuristics.partial_search

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput
import ocrtest.camera.services.GoogleSearchService

/**
 * A heuristic which ranks answers based on matches from a Google search query combining the
 * question and an answer choice.
 */
class PartialSearchHeuristic(val service: GoogleSearchService?) : Heuristic {

    val EMPTY = HeuristicOutput(ImmutableMap.of<String, Int>())

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        if (service == null) {
            return Observable.just(EMPTY)
        }

        val helper = GoogleSearchHelper(service)
        val searchObservables : List<ObservableSource<Int>> = input.answers.map{
            answer -> helper.getResultCount(input.question + " " + answer)}
        return Observable.combineLatest<Int, HeuristicOutput>(
                searchObservables,
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
                            return HeuristicOutput(ImmutableMap.of<String, Int>())
                        }
                        return HeuristicOutput(ImmutableMap.copyOf(
                                input.answers
                                        .withIndex()
                                        .associateBy({it.value}, {scores[it.index]})))
                    }
                })
    }
}