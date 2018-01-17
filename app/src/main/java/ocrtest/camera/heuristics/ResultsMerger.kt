package ocrtest.camera.heuristics

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap

/**
 * Tool to merge results and present them as percentages
 */
class ResultsMerger() {

    fun mergeResults(scores: List<Double>, input: HeuristicInput) : HeuristicOutput {
        val total = Math.max(scores.sum(), 1.0)
        val results = input.answers
                .withIndex()
                .associateBy({it.value}, {Math.round(scores[it.index]/total * 1000).toDouble()/10})
        return HeuristicOutput(ImmutableMap.copyOf(results))
    }
}