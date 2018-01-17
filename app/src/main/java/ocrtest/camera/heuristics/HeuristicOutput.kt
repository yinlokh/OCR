package ocrtest.camera.heuristics

import com.google.common.collect.ImmutableMap

/**
 * Output from a single Heuristic
 */
data class HeuristicOutput(val scores: ImmutableMap<String, Double>)