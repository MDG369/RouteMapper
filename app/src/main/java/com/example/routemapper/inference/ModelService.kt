package com.example.routemapper.inference

import android.content.Context
import android.util.Log
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.*
import kotlin.system.measureNanoTime

class ModelService(
    private val windowSize: Int = 160,
    private val sampleLeeway: Int = 10,
    private val batchSize: Int = 16
) {
    fun loadModel(context: Context, modelName: String): Module {
        val file = File(context.filesDir, modelName)
        if (!file.exists()) {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        Log.i("ModelService", "Model loaded from ${file.absolutePath}")
        return LiteModuleLoader.load(file.absolutePath)
    }

    fun readCsvFile(context: Context, fullPath: String): Pair<List<FloatArray>, List<Float>> {
        val features = mutableListOf<FloatArray>()
        val labels = mutableListOf<Float>()

        BufferedReader(FileReader(fullPath)).use { reader ->
            reader.lineSequence().drop(1).forEach { line ->
                val parts = line.split(",")
                val featureVals = parts.dropLast(1).map { it.toFloat() }.toFloatArray()
                val label = parts.last().toFloat()
                features.add(featureVals)
                labels.add(label)
            }
        }

        return Pair(features, labels)
    }

    fun runInferenceOnFolder(
        context: Context,
        module: Module,
        folderName: String,
        resultsFolder: String
    ) {
        copyAssetsFolderToInternalStorage(context, "data_folder")

        val inputFolder = File(context.filesDir, folderName)
        val outputFolder = File(context.filesDir, resultsFolder)
        if (!outputFolder.exists()) outputFolder.mkdirs()

        val summaryFile = File(outputFolder, "inference_summary.csv")
        BufferedWriter(FileWriter(summaryFile)).use { summaryWriter ->
            summaryWriter.write("filename,total_time_ms,avg_time_per_window_ms\n")

            inputFolder.listFiles { _, name -> name.endsWith(".csv") }?.forEach { file ->
                val (features, labels) = readCsvFile(context, File(inputFolder, file.name).absolutePath)
                val timePerBatch = mutableListOf<Long>()
                val predictions = mutableListOf<Float>()
                var totalTime = 0L
                val totalWindows = features.size - windowSize + 1
                var i = 0

                while (i < totalWindows) {
                    val currentBatchSize = minOf(batchSize, totalWindows - i)
                    val batchWindows = ArrayList<FloatArray>(currentBatchSize)
                    for (j in 0 until currentBatchSize) {
                        val window = features.subList(i + j, i + j + windowSize)
                        batchWindows.add(window.flatMap { it.asList() }.toFloatArray())
                    }
                    val inputTensor = prepareBatchTensor(batchWindows, currentBatchSize)

                    val time = measureNanoTime {
                        val output = module.forward(IValue.from(inputTensor)).toTensor().dataAsFloatArray
                        for (k in 0 until currentBatchSize) {
                            val prediction = if (output[k] > 0.5f) 1f else 0f
                            predictions.add(prediction)
                        }
                    }

                    timePerBatch.add(time)
                    totalTime += time
                    i += currentBatchSize
                }

                // Save per-file timing CSV
                val timingFile = File(outputFolder, "${file.nameWithoutExtension}_timing.csv")
                BufferedWriter(FileWriter(timingFile)).use { timingWriter ->
                    timingWriter.write("batch_index,time_ns\n")
                    timePerBatch.forEachIndexed { idx, t -> timingWriter.write("$idx,$t\n") }
                }

                // Write to summary
                val avgTimeMs = totalTime.toDouble() / totalWindows / 1_000_000.0
                summaryWriter.write("${file.name},${totalTime / 1_000_000},$avgTimeMs\n")
                Log.i("ModelService", "Finished inference for ${file.name}")
            }
        }

        Log.i("ModelService", "Saved overall summary to ${summaryFile.absolutePath}")
    }

    private fun prepareBatchTensor(batchWindows: List<FloatArray>, batchSize: Int): Tensor {
        val featureSize = batchWindows[0].size / windowSize
        val flat = batchWindows.flatMap { it.asList() }.toFloatArray()
        return Tensor.fromBlob(flat, longArrayOf(batchSize.toLong(), windowSize.toLong(), featureSize.toLong()))
    }
}
fun copyAssetsFolderToInternalStorage(context: Context, assetFolderName: String) {
    val assetManager = context.assets
    val files = assetManager.list(assetFolderName) ?: return
    val outDir = File(context.filesDir, assetFolderName)
    if (!outDir.exists()) outDir.mkdirs()

    for (filename in files) {
        val inStream = assetManager.open("$assetFolderName/$filename")
        val outFile = File(outDir, filename)
        val outStream = FileOutputStream(outFile)

        inStream.copyTo(outStream)
        inStream.close()
        outStream.close()
    }
}
