package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ContentItem(
    val parts: List<PartItem>
)

@JsonClass(generateAdapter = true)
data class PartItem(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentItem>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: ContentItem? = null
)

@JsonClass(generateAdapter = true)
data class CandidateItem(
    val content: ContentItem? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidateItem>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun checkAnswer(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Connect, read, and write configured with 60 seconds as required in gemini-api GOTCHAS
    private val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun verifyAnswerWithAI(question: String, modelAnswer: String, userAnswer: String): Boolean {
        // Fallback local key check just in case
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback semantic checking via simple keywords if key is unspecified
            val tokens = userAnswer.lowercase().trim().split("\\s+".toRegex())
            val matchedCount = modelAnswer.lowercase().trim().split("\\s+".toRegex()).count { it in tokens }
            return matchedCount >= 1 || userAnswer.trim().length >= 2 && modelAnswer.contains(userAnswer, ignoreCase = true)
        }

        val prompt = """
            Determine if the following Arabic answer entered by a user is semantically correct for the given riddle. 
            User entered: "$userAnswer"
            Expected model meaning: "$modelAnswer"
            Question asked: "$question"
            The user might write slightly different phrasing but with the same core semantic meaning (e.g. "الديك لا يلد" instead of "الديك لا يبيض", or "الساعة متطابقة" or numbers).
            Please respond exactly with either "YES" or "NO", nothing else.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                ContentItem(parts = listOf(PartItem(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = ContentItem(parts = listOf(PartItem(text = "You are an accurate semantic grading assistant for riddle answers. Keep your response extremely brief: only either YES or NO.")))
        )

        return try {
            val response = apiService.checkAnswer(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.uppercase()
            resultText?.contains("YES") == true
        } catch (e: Exception) {
            // Log or fallback safely
            val matchedCount = modelAnswer.lowercase().trim().split("\\s+".toRegex()).count { it in userAnswer.lowercase() }
            matchedCount >= 1 || modelAnswer.contains(userAnswer, ignoreCase = true)
        }
    }
}
