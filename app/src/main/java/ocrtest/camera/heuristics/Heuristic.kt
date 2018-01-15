package ocrtest.camera.heuristics

import io.reactivex.Observable

/**
 * Interface for a single Heuristic
 */
interface Heuristic {

    fun compute(input : HeuristicInput) : Observable<HeuristicOutput>
}