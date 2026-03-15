package com.twinmindx.data.remote.transcription.network

import com.google.gson.annotations.SerializedName

data class WhisperTranscriptionResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("error")
    val error: WhisperError? = null
)

data class WhisperError(
    @SerializedName("message")
    val message: String,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("code")
    val code: String? = null
)
