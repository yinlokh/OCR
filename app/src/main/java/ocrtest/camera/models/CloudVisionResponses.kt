package ocrtest.camera.models

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Observable
import ocrtest.camera.heuristics.HeuristicOutput

data class CloudVisionResponses(val responses: List<CloudVisionResponse>) {

    fun toTriviaQuestion() : TriviaQuestion {
        val fullText = responses.get(0)?.textAnnotations?.get(0)?.description ?: ""
        val lines = fullText.split('\n').filter { text -> text.length > 0 }
        if (lines.size < 3) {
            return TriviaQuestion("", ImmutableList.of())
        }

        val answers = lines.subList(lines.size - 3, lines.size)
        var questionBuilder = StringBuilder()
        for (line in lines.subList(0, lines.size - 3)) {
            questionBuilder.append(line)
            questionBuilder.append(" ")
        }
        val question = questionBuilder.toString()
        return TriviaQuestion(question, answers)
    }
}