package com.twinmindx.data.remote.summary.network

import com.google.gson.annotations.SerializedName


data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("stream")
    val stream: Boolean,
    @SerializedName("messages")
    val messages: List<Message>
)

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)


data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("object")
    val obj: String? = null,
    @SerializedName("created")
    val created: Long? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("choices")
    val choices: List<Choice>? = null,
    @SerializedName("error")
    val error: OpenAIError? = null
)

data class Choice(
    @SerializedName("index")
    val index: Int? = null,
    @SerializedName("message")
    val message: Message? = null,
    @SerializedName("delta")
    val delta: Delta? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class Delta(
    @SerializedName("content")
    val content: String? = null
)


data class OpenAIError(
    @SerializedName("message")
    val message: String,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("code")
    val code: String? = null
)

data class OpenAIErrorResponse(
    @SerializedName("error")
    val error: OpenAIError? = null
)
