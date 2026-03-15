package com.twinmindx.data.remote.summary.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletionStreaming(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}
