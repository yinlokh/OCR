package ocrtest.camera.utils

/**
 * Extractor to pick out names from a question
 */
class KeywordExtractor() {

    fun extractKeywords(content: String) : List<String> {
        val tokens = content
                .replace('?', ' ')
                .split(" ")
                .filter { word -> word.length > 0 }
        val list = ArrayList<String>()
        var inQuotes = false
        var keywordBuilder = StringBuilder()
        for (token in tokens) {
             if (token.startsWith('"') || token.startsWith('\'')) {
                inQuotes = true
                keywordBuilder.append(token)
                if (token.endsWith('"') || token.endsWith('\'')) {
                    list.add(keywordBuilder.toString())
                    keywordBuilder = StringBuilder()
                    inQuotes = false
                }
            } else if (token.endsWith('"') || token.endsWith('\'')) {
                keywordBuilder.append(" ")
                keywordBuilder.append(token)
                list.add(keywordBuilder.toString())
                keywordBuilder = StringBuilder()
                inQuotes = false
            } else if (token[0] >= 'A' && token[0] <= 'Z') {
                if (keywordBuilder.length > 0) {
                    keywordBuilder.append(" ")
                }
                keywordBuilder.append(token)
            } else if (inQuotes) {
                 keywordBuilder.append(" ")
                 keywordBuilder.append(token)
            } else if (keywordBuilder.length > 0) {
                list.add(keywordBuilder.toString())
                keywordBuilder = StringBuilder()
            }
        }
        if (keywordBuilder.length > 0) {
            list.add(keywordBuilder.toString())
        }

        val commonWords = CommonWords()
        return list.filter { word -> !commonWords.WORD_SET.contains(word.toLowerCase()) }
                .map { word -> word.replace("\'s", "") }
    }

    fun sanitizeKeyword(keyword: String) : String {
        return keyword.replace("\'s", "")
    }
}
