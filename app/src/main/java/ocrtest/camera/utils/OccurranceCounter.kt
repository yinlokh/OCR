package ocrtest.camera.utils

/**
 * Counter to count number of occurrances of a set of words in a content string
 */
class OccurranceCounter {

    fun countOccurrances(content: String, words: Set<String>?): Double {
        if (words == null) {
            return 0.0
        }
        return words.fold(0, operation = { count, word -> count + content.split(word).size - 1 }).toDouble()
    }
}