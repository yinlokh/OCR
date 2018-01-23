package ocrtest.camera.heuristics.combination

import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import io.reactivex.functions.Function
import ocrtest.camera.heuristics.Heuristic
import ocrtest.camera.heuristics.HeuristicInput
import ocrtest.camera.heuristics.HeuristicOutput

/**
 * Heuristic to use a combination of other heuristics.
 */
class CombinationHeuristic(val heuristics: List<Heuristic>) : Heuristic {

    override fun compute(input: HeuristicInput): Observable<HeuristicOutput> {
        val observableSources = heuristics.map { heuristic -> heuristic.compute(input) }
        return Observable.combineLatest(
                observableSources,
                object : Function<Array<Any>, HeuristicOutput> {

                    override fun apply(t: Array<Any>): HeuristicOutput {
                        return combineScores(t
                                .map { it -> it as HeuristicOutput }
                                .map { output -> output.scores })
                    }

                })
    }

    fun combineScores(scoresMap: List<Map<String, Double>>): HeuristicOutput {
        val totals = HashMap<String, Double>()
        scoresMap.forEach { scores ->
            scores.forEach { key, value -> totals.put(key, (totals.get(key)?: 0.0) + value) }
        }

        return HeuristicOutput(ImmutableMap.copyOf(totals))
    }
}