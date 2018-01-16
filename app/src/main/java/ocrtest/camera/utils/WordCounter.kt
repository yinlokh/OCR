package ocrtest.camera.utils

/**
 * Util to count number of times a word appears in a string
 */
class WordCounter {

    fun countWords(content: String, words: Collection<String>) : Int {
        return words.map{word -> content.split(word).size - 1}.sum()
    }
}