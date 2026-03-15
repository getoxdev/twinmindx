package com.twinmindx.di

import com.google.gson.Gson
import com.twinmindx.BuildConfig
import com.twinmindx.data.remote.summary.OpenAiSummaryService
import com.twinmindx.data.remote.summary.network.OpenAIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SummaryModule {

    private const val BASE_URL = "https://api.openai.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIApi(retrofit: Retrofit): OpenAIApiService {
        return retrofit.create(OpenAIApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenAiSummaryService(
        openAIApiService: OpenAIApiService,
        gson: Gson
    ): OpenAiSummaryService {
        return OpenAiSummaryService(
            openAIApiService = openAIApiService,
            apiKey = BuildConfig.OPENAI_API_KEY,
            gson = gson
        )
    }
}
