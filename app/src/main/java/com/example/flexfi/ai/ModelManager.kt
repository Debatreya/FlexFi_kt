package com.example.flexfi.ai

import android.content.Context
import android.util.Log
import com.example.flexfi.BuildConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class ModelDownloadStatus {
    IDLE,
    DOWNLOADING,
    RETRYING,
    LOADING,
    READY,
    FAILED
}

data class ModelDownloadState(
    val status: ModelDownloadStatus = ModelDownloadStatus.IDLE,
    val progressPercent: Int = 0,
    val message: String = "Idle"
)

/**
 * Manages the lifecycle of on-device LLM models.
 * Handles downloading, initialization, and caching of MediaPipe LLM models.
 *
 * Singleton pattern: one instance per app lifetime.
 */
class ModelManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_DIR_NAME = "ai_models"
        private const val MODEL_FILE_NAME = "gemma-3-1b-it-int4.task"
        private const val WARMUP_PROMPT = "Hi"

        // Fallback URL used only when BuildConfig.MODEL_DOWNLOAD_URL is empty.
        private const val DEFAULT_MODEL_DOWNLOAD_URL = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task?download=true"
        private const val MAX_DOWNLOAD_RETRIES = 3

        @Volatile
        private var instance: ModelManager? = null

        fun getInstance(context: Context): ModelManager {
            return instance ?: synchronized(this) {
                instance ?: ModelManager(context).also { instance = it }
            }
        }
    }

    private val modelDir: File = File(context.filesDir, MODEL_DIR_NAME)
    private val modelFile: File = File(modelDir, MODEL_FILE_NAME)
    private val modelDownloadUrl: String =
        BuildConfig.MODEL_DOWNLOAD_URL.ifBlank { DEFAULT_MODEL_DOWNLOAD_URL }
    private val huggingFaceToken: String = BuildConfig.HUGGING_FACE_TOKEN

    @Volatile
    private var llmInference: LlmInference? = null
        private set

    @Volatile
    private var warmupTriggered: Boolean = false

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inferenceMutex = Mutex()

    private val _downloadState = MutableStateFlow(ModelDownloadState())
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    /**
     * Initializes the model manager and loads/downloads model if needed.
     * Non-blocking: happens in background via coroutines.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing ModelManager")
            _downloadState.value = ModelDownloadState(
                status = ModelDownloadStatus.IDLE,
                progressPercent = 0,
                message = "Checking model"
            )

            // Ensure model directory exists
            if (!modelDir.exists()) {
                modelDir.mkdirs()
                Log.d(TAG, "Created model directory: ${modelDir.absolutePath}")
            }

            // Check if model already exists
            if (modelFile.exists()) {
                Log.d(TAG, "Model file found locally, loading...")
                _downloadState.value = ModelDownloadState(
                    status = ModelDownloadStatus.LOADING,
                    progressPercent = 100,
                    message = "Loading local model"
                )
                loadModel()
            } else {
                Log.d(TAG, "Model not found, downloading...")
                downloadAndLoadModel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ModelManager initialization failed", e)
            _downloadState.value = ModelDownloadState(
                status = ModelDownloadStatus.FAILED,
                progressPercent = 0,
                message = "Model unavailable. Using fallback insights"
            )
            // Gracefully degrade: set llmInference to null, fall back to rule-based insights
        }
    }

    /**
     * Downloads the quantized Gemma 1B model from CDN.
     * In production, integrate with actual download service.
     */
    private suspend fun downloadAndLoadModel() {
        var lastError: Exception? = null
        for (attempt in 1..MAX_DOWNLOAD_RETRIES) {
            try {
                _downloadState.value = ModelDownloadState(
                    status = if (attempt == 1) ModelDownloadStatus.DOWNLOADING else ModelDownloadStatus.RETRYING,
                    progressPercent = 0,
                    message = if (attempt == 1) {
                        "Downloading model"
                    } else {
                        "Retrying download ($attempt/$MAX_DOWNLOAD_RETRIES)"
                    }
                )

                Log.d(TAG, "Downloading model from: $modelDownloadUrl")
                Log.d(TAG, "Model target: ${modelFile.absolutePath}")

                val tmpFile = File(modelDir, "${MODEL_FILE_NAME}.tmp")
                downloadFile(modelDownloadUrl, tmpFile)

                if (modelFile.exists()) {
                    modelFile.delete()
                }
                if (!tmpFile.renameTo(modelFile)) {
                    throw IllegalStateException("Failed to finalize downloaded model file")
                }

                Log.d(TAG, "Model download complete")
                _downloadState.value = ModelDownloadState(
                    status = ModelDownloadStatus.LOADING,
                    progressPercent = 100,
                    message = "Loading downloaded model"
                )
                loadModel()
                return
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Model download attempt $attempt failed", e)
                if (attempt < MAX_DOWNLOAD_RETRIES) {
                    val backoffMs = 1000L * (1L shl (attempt - 1))
                    delay(backoffMs)
                }
            }
        }

        _downloadState.value = ModelDownloadState(
            status = ModelDownloadStatus.FAILED,
            progressPercent = 0,
            message = "Model download failed after retries"
        )
        throw IllegalStateException("Model download failed after retries", lastError)
    }

    /**
     * Loads the model file into memory and initializes LLM inference.
     */
    private suspend fun loadModel() {
        try {
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                return
            }

            Log.d(TAG, "Loading model from: ${modelFile.absolutePath}")

            inferenceMutex.withLock {
                llmInference?.close()
                val builder = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(256)
                    .setMaxTopK(40)
                applyOptionalFloatOption(builder, "setTemperature", 0.2f)
                applyOptionalIntOption(builder, "setRandomSeed", 42)
                val options = builder.build()
                llmInference = LlmInference.createFromOptions(context, options)
            }

            Log.d(TAG, "Model loaded successfully")
            _downloadState.value = ModelDownloadState(
                status = ModelDownloadStatus.READY,
                progressPercent = 100,
                message = "Model ready"
            )
            triggerWarmupIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Model loading failed", e)
            _downloadState.value = ModelDownloadState(
                status = ModelDownloadStatus.FAILED,
                progressPercent = 100,
                message = "Model failed to load"
            )
            throw e
        }
    }

    /**
     * Checks if model is available and ready for inference.
     */
    fun isModelReady(): Boolean {
        return llmInference != null && modelFile.exists()
    }

    /**
     * Generates text using the loaded LLM model.
     * Returns null if model is not ready (fallback to rule-based).
     *
     * @param prompt Input prompt for generation
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0 - 1.0)
     * @param topK Top-K sampling parameter
     * @return Generated text response or null on error
     */
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.2f,
        topK: Int = 40
    ): String? = withContext(Dispatchers.Default) {
        inferenceMutex.withLock {
            val engine = llmInference
            if (engine == null || !isModelReady()) {
                Log.w(TAG, "Model not ready for inference")
                return@withLock null
            }

            try {
                // Rebuild engine if runtime options differ from default.
                if (maxTokens != 256 || temperature != 0.2f || topK != 40) {
                    val runtimeBuilder = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(maxTokens)
                        .setMaxTopK(topK)
                    applyOptionalFloatOption(runtimeBuilder, "setTemperature", temperature)
                    applyOptionalIntOption(runtimeBuilder, "setRandomSeed", 42)
                    val runtimeOptions = runtimeBuilder.build()
                    llmInference?.close()
                    llmInference = LlmInference.createFromOptions(context, runtimeOptions)
                }

                llmInference?.generateResponse(prompt)
            } catch (e: Exception) {
                Log.e(TAG, "Text generation failed", e)
                null
            }
        }
    }

    /**
     * Clears downloaded model to free up space.
     * User can trigger this from settings.
     */
    suspend fun clearModel() = withContext(Dispatchers.IO) {
        try {
            inferenceMutex.withLock {
                llmInference?.close()
                llmInference = null
            }
            warmupTriggered = false
            if (modelFile.exists()) {
                modelFile.delete()
                Log.d(TAG, "Model file deleted")
            }
            _downloadState.value = ModelDownloadState(
                status = ModelDownloadStatus.IDLE,
                progressPercent = 0,
                message = "Model cleared"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing model", e)
        }
    }

    /**
     * Gets the size of the downloaded model in bytes.
     */
    fun getModelSizeBytes(): Long {
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    /**
     * Gets human-readable model size (KB, MB, GB).
     */
    fun getModelSizeFormatted(): String {
        val bytes = getModelSizeBytes()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun downloadFile(url: String, target: File) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 300_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "FlexFi-Android/1.0")
                if (huggingFaceToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $huggingFaceToken")
                }
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw IllegalStateException(
                    "Model URL unauthorized (HTTP 401). Configure HUGGING_FACE_TOKEN or use a public MODEL_DOWNLOAD_URL."
                )
            }
            if (responseCode !in 200..299) {
                throw IllegalStateException("Model download failed with HTTP $responseCode")
            }

            val contentLength = connection.contentLengthLong.takeIf { it > 0L }
            val buffer = ByteArray(64 * 1024)
            var totalRead = 0L

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        totalRead += read

                        if (contentLength != null) {
                            val pct = ((totalRead * 100L) / contentLength).toInt().coerceIn(0, 99)
                            _downloadState.value = ModelDownloadState(
                                status = ModelDownloadStatus.DOWNLOADING,
                                progressPercent = pct,
                                message = "Downloading model: $pct%"
                            )
                        }
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun applyOptionalFloatOption(target: Any, methodName: String, value: Float) {
        runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Float::class.javaPrimitiveType || it.parameterTypes[0] == java.lang.Float::class.java)
            }
            method?.invoke(target, value)
        }.onFailure {
            Log.d(TAG, "Optional option not supported: $methodName")
        }
    }

    private fun applyOptionalIntOption(target: Any, methodName: String, value: Int) {
        runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Int::class.javaPrimitiveType || it.parameterTypes[0] == java.lang.Integer::class.java)
            }
            method?.invoke(target, value)
        }.onFailure {
            Log.d(TAG, "Optional option not supported: $methodName")
        }
    }

    private fun triggerWarmupIfNeeded() {
        if (warmupTriggered) return
        warmupTriggered = true

        warmupScope.launch {
            try {
                // Reuse safe inference path to avoid concurrent native access.
                generateText(WARMUP_PROMPT, maxTokens = 16, temperature = 0.2f, topK = 20)
                Log.d(TAG, "Model warmup completed")
            } catch (e: Exception) {
                Log.w(TAG, "Model warmup failed", e)
            }
        }
    }
}
