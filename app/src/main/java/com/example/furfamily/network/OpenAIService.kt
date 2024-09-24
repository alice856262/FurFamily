package com.example.furfamily.network

import com.example.furfamily.BuildConfig
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val apiKey = BuildConfig.OPENAI_API_KEY

interface OpenAIService {
    @Headers("Authorization: Bearer $apiKey")
    @POST("/v1/chat/completions")
    suspend fun getChatResponse(@Body requestBody: ChatRequest): ChatResponse
}