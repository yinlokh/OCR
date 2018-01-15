package ocrtest.camera.models

data class CloudVisionTextAnnotation(
        val mid: String,
        val locale: String,
        val description: String,
        val score: String,
        val confidence: String,
        val topicality: String)
