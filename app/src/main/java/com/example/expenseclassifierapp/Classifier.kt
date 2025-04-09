package com.example.expenseclassifierapp

import android.content.Context
import android.util.Log
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
        private const val TEXT_VECTOR_LENGTH = 100 // Matches tfidf_vocab size
    }

    init {
        interpreter = Interpreter(loadModelFile(MODEL_FILE))
        tfidfVocab = loadVocabulary(VOCAB_FILE)
        labels = loadLabels(LABELS_FILE)
    }

    fun classify(text: String, amount: Float): String {
        val tfidfVector = textToTfidfVector(text)

        if (tfidfVector.all { it == 0f }) {
            Log.w("Classifier", "TF-IDF vector is all zeros — unknown text?")
            return "Unknown"
        }

        val input1 = Array(1) { tfidfVector }         // shape: [1][100]
        val input2 = Array(1) { floatArrayOf(amount) } // shape: [1][1]
        val output = mutableMapOf<Int, Any>(0 to Array(1) { FloatArray(labels.size) })

        return try {
            interpreter.runForMultipleInputsOutputs(arrayOf(input1, input2), output)
            val predictions = output[0] as Array<FloatArray>
            val predictionIndex = predictions[0].indices.maxByOrNull { predictions[0][it] } ?: -1
            labels.getOrElse(predictionIndex) { "Unknown" }
        } catch (e: Exception) {
            e.printStackTrace()
            "Prediction failed"
        }
    }

    fun getLabels(): List<String> {
        return labels
    }

    private fun textToTfidfVector(text: String): FloatArray {
        val vector = FloatArray(TEXT_VECTOR_LENGTH)
        val tokens = text.lowercase().split(Regex("\\s+"))
        val tokenCounts = tokens.groupingBy { it }.eachCount()

        for ((token, count) in tokenCounts) {
            val index = tfidfVocab[token]
            if (index != null && index in vector.indices) {
                vector[index] = count.toFloat()
            }
        }
        return vector
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
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
