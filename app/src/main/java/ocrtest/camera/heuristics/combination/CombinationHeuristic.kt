package ocrtest.camera.heuristics.combination

import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
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
                { outputs ->
                    combineRankings(outputs
                            .filter { output -> filterEmptyResults(output as HeuristicOutput) }
                            .map { output -> reduceToRankingScore(output as HeuristicOutput) })
                })
    }

    fun filterEmptyResults(output: HeuristicOutput): Boolean {
        return output.scores.values.sum() != 0
    }

    fun reduceToRankingScore(output: HeuristicOutput): Map<String, Int> {
        return output.scores.keys
                .sortedByDescending { key -> output.scores.get(key) }
                .withIndex()
                .associateBy(
                        { indexedValue -> indexedValue.value },
                        { indexedValue -> indexedValue.index })
    }

    fun combineRankings(rankings: List<Map<String, Int>>): HeuristicOutput {
        val totals = HashMap<String, Int>()
        rankings.forEach { ranking ->
            ranking.forEach { key, value -> totals.put(key, totals.get(key) ?: 0 + value) }
        }
        return HeuristicOutput(
                ImmutableMap.copyOf(
                        totals.keys.sortedByDescending { key -> totals.get(key) }
                                .withIndex()
                                .associateBy(
                                        { indexedValue -> indexedValue.value },
                                        { indexedValue -> indexedValue.index }
                                )))
    }
}