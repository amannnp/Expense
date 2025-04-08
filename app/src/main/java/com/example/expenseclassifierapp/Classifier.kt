package com.example.expenseclassifierapp

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {

    private var interpreter: Interpreter
    private val tfidfVocab: Map<String, Int>
    private val labels: List<String>

    companion object {
        private const val MODEL_FILE = "expense_model.tflite"
        private const val VOCAB_FILE = "tfidf_vocab.json"
        private const val LABELS_FILE = "label_classes.json"
        private const val TEXT_VECTOR_LENGTH = 70 // Must match model input
    }

    init {
        interpreter = Interpreter(loadModelFile(MODEL_FILE))
        tfidfVocab = loadVocabulary(VOCAB_FILE)
        labels = loadLabels(LABELS_FILE)
    }

    fun classify(text: String, amount: Float): String {
        val tfidfInput = textToTfidfVector(text)
        val amountInput = arrayOf(floatArrayOf(amount))

        val inputs = arrayOf(tfidfInput, amountInput)
        val output = Array(1) { FloatArray(labels.size) }

        interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))
        val predictionIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        return labels[predictionIndex]
    }

    fun getLabels(): List<String> {
        return labels
    }

    private fun textToTfidfVector(text: String): Array<FloatArray> {
        val vector = FloatArray(TEXT_VECTOR_LENGTH)
        val tokens = text.lowercase().split(Regex("\\s+"))
        val tokenCounts = tokens.groupingBy { it }.eachCount()

        var i = 0
        for ((token, count) in tokenCounts) {
            val index = tfidfVocab[token]
            if (index != null && index < TEXT_VECTOR_LENGTH) {
                vector[index] = count.toFloat()
                i++
                if (i >= TEXT_VECTOR_LENGTH) break
            }
        }
        return arrayOf(vector)
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocabulary(filename: String): Map<String, Int> {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val map = mutableMapOf<String, Int>()
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.getInt(key)
        }
        return map
    }

    private fun loadLabels(filename: String): List<String> {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}
