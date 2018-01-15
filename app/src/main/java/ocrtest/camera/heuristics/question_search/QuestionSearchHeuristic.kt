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
        if (service == null) {
            return Observable.just(EMPTY)
        }

        return service.search(input.question)
                .subscribeOn(Schedulers.computation())
                .map(object : Function<ResponseBody, HeuristicOutput>{
                    override fun apply(body: ResponseBody): HeuristicOutput {
                        val lowerCase = body.string().toLowerCase()
                        return HeuristicOutput(ImmutableMap.copyOf(
                                input.answers.associateBy(
                                    {it : String -> it},
                                    {it -> lowerCase.split(it).size})))
                    }
                })
    }
}