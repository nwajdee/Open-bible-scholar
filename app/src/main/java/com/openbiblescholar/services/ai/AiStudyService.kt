package com.openbiblescholar.services.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.openbiblescholar.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val role: String,   // "system" | "user" | "assistant"
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    val temperature: Double = 0.3,
    val stream: Boolean = false
)

data class ChatResponse(
    val choices: List<Choice>
) {
    data class Choice(val message: MessageContent)
    data class MessageContent(val content: String)
}

@Singleton
class AiStudyService @Inject constructor() {

    private val gson = Gson()

    private fun buildClient() = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Core API call ─────────────────────────────────────────────────────────

    suspend fun askAi(
        config: AiConfig,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(Exception("No API key configured. Go to Settings → AI Settings to add your free API key."))
            }

            val client = buildClient()
            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userPrompt)
                ),
                maxTokens = config.maxTokens,
                temperature = config.temperature.toDouble(),
                stream = false
            )

            val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("${config.provider.baseUrl}chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (config.provider == AiProvider.OPENROUTER) {
                        addHeader("HTTP-Referer", "https://openbiblescholar.app")
                        addHeader("X-Title", "OpenBible Scholar")
                    }
                }
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("AI API error ${response.code}: ${response.body?.string()}")
                )
            }

            val parsed = gson.fromJson(response.body?.string(), ChatResponse::class.java)
            Result.success(parsed.choices.firstOrNull()?.message?.content ?: "No response")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Study Prompts ─────────────────────────────────────────────────────────

    suspend fun getVerseInsight(config: AiConfig, verse: BibleVerse): Result<String> {
        val system = """You are a knowledgeable Bible scholar. Provide concise, clear, faithful 
            explanations grounded in the original languages and historical context. 
            Be accurate, edifying, and non-denominational unless asked.""".trimIndent()

        val user = """Explain ${verse.book} ${verse.chapter}:${verse.verse} (${verse.translation}):
            
            "${verse.text}"
            
            Provide: (1) historical/cultural context, (2) key word meanings, 
            (3) theological significance, (4) practical application. Keep it concise."""

        return askAi(config, system, user)
    }

    suspend fun getCrossReferences(config: AiConfig, verse: BibleVerse): Result<String> {
        val system = "You are a Bible cross-reference expert. List related passages with brief explanations."
        val user = """Find 5-8 key cross-references for ${verse.book} ${verse.chapter}:${verse.verse}:
            "${verse.text}"
            Format each as: [Book Chapter:Verse] - Brief reason it connects.
            Focus on thematic, typological, and doctrinal connections."""

        return askAi(config, system, user)
    }

    suspend fun getWordStudy(config: AiConfig, word: String, strongsNumber: String, context: BibleVerse): Result<String> {
        val system = "You are a Hebrew and Greek lexicographer specializing in Biblical languages."
        val user = """Word study for "$word" (Strong's $strongsNumber) 
            in ${context.book} ${context.chapter}:${context.verse}.
            
            Provide: original word, transliteration, root meaning, usage across Scripture,
            how it's used in this context, and key insights for Bible study."""

        return askAi(config, system, user)
    }

    suspend fun getPassageGuide(config: AiConfig, book: String, chapter: Int, verses: List<BibleVerse>): Result<String> {
        val system = """You are a pastoral Bible teacher creating a study guide. 
            Be scholarly yet accessible, covering background, structure, theology, and application.""".trimIndent()

        val passageText = verses.joinToString("\n") { "[${it.verse}] ${it.text}" }
        val user = """Create a passage study guide for $book $chapter:
            
            $passageText
            
            Include: (1) Background & Context, (2) Literary Structure & Flow, 
            (3) Key Themes & Theological Insights, (4) Notable Words & Phrases,
            (5) Application for Today. Format with clear headings."""

        return askAi(config, system, user)
    }

    suspend fun getHistoricalBackground(config: AiConfig, book: String, chapter: Int): Result<String> {
        val system = "You are a Biblical historian and archaeologist with expertise in the ancient Near East and Greco-Roman world."
        val user = """Provide the historical and cultural background for $book chapter $chapter.
            Cover: time period, author, original audience, historical setting, 
            archaeological insights, and why this context matters for understanding the text."""

        return askAi(config, system, user)
    }

    suspend fun getSermonOutline(config: AiConfig, verse: BibleVerse): Result<String> {
        val system = "You are a homiletics expert who creates clear, expository sermon outlines."
        val user = """Create an expository sermon outline for ${verse.book} ${verse.chapter}:${verse.verse}:
            "${verse.text}"
            
            Include: Title, Big Idea, Introduction hook, 3-4 main points with sub-points,
            illustrations suggestions, and conclusion/application."""

        return askAi(config, system, user)
    }

    suspend fun getDailyDevotional(config: AiConfig, verse: BibleVerse): Result<String> {
        val system = "You are a devotional writer creating warm, personal, spiritually enriching daily devotionals."
        val user = """Write a short daily devotional (300-400 words) for ${verse.book} ${verse.chapter}:${verse.verse}:
            "${verse.text}"
            
            Structure: Opening thought, verse meaning, personal reflection question, prayer."""

        return askAi(config, system, user)
    }

    fun streamInsight(
        config: AiConfig,
        systemPrompt: String,
        userPrompt: String
    ): Flow<String> = flow {
        // Streaming implementation using SSE
        if (config.apiKey.isBlank()) {
            emit("ERROR: No API key configured. Please add your free API key in Settings → AI Settings.")
            return@flow
        }
        try {
            val client = buildClient()
            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userPrompt)
                ),
                maxTokens = config.maxTokens,
                temperature = config.temperature.toDouble(),
                stream = true
            )
            val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("${config.provider.baseUrl}chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            val reader = response.body?.charStream()?.buffered() ?: run {
                emit("ERROR: Empty response from AI service")
                return@flow
            }

            reader.lineSequence().forEach { line ->
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEach
                    try {
                        val chunk = gson.fromJson(data, StreamChunk::class.java)
                        val delta = chunk.choices?.firstOrNull()?.delta?.content ?: ""
                        if (delta.isNotEmpty()) emit(delta)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            emit("ERROR: ${e.message}")
        }
    }

    private data class StreamChunk(
        val choices: List<StreamChoice>?
    )
    private data class StreamChoice(val delta: StreamDelta?)
    private data class StreamDelta(val content: String?)
}
