package ocrtest.camera.utils

import com.google.common.collect.ImmutableList
import ocrtest.camera.models.TriviaQuestion

/**
 * Utility to get question based on full OCR text.
 */
class RawTextToQuestion {

    fun getQuestion(fullText: String) : TriviaQuestion {
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