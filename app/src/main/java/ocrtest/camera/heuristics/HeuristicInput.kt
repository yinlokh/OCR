package ocrtest.camera.heuristics

import ocrtest.camera.models.TriviaQuestion

/**
 * Input to a single Heuristic
 */
data class HeuristicInput(val question: String, val answers: List<String>) {

    constructor(question: TriviaQuestion): this(
            question.question,
            question.choices.map { choice -> choice.toLowerCase() })
}