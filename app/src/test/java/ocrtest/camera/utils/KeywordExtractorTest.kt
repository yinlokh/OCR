package ocrtest.camera.utils

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import junit.framework.Assert
import org.junit.Test

/**
 * Created by eric on 1/17/2018.
 */
class KeywordExtractorTest() {

    var EXPECTED_RESULTS: ImmutableMap<String, Set<String>> = ImmutableMap.of(
            "'Single Quote Item' test", ImmutableSet.of("'Single Quote Item'"),
            "\"Double Quote Item\" test", ImmutableSet.of("\"Double Quote Item\""),
            "Consecutive Capital Words test", ImmutableSet.of("Consecutive Capital Words"),
            "KeywordExtractor's test", ImmutableSet.of("KeywordExtractor"))

    @Test
    fun test1() {
        val keywordExtractor = KeywordExtractor()
        for (input in EXPECTED_RESULTS.keys) {
            System.out.print(input)
            Assert.assertEquals(keywordExtractor.extractKeywords(input).toSet(), (EXPECTED_RESULTS.get(input)))
        }
    }
}