package com.twinmindx.di

import com.twinmindx.BuildConfig
import com.twinmindx.data.summary.OpenAiSummaryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SummaryModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // generous for streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiSummaryService(okHttpClient: OkHttpClient): OpenAiSummaryService {
        return OpenAiSummaryService(
            okHttpClient = okHttpClient,
            apiKey = BuildConfig.OPENAI_API_KEY
        )
    }
}
