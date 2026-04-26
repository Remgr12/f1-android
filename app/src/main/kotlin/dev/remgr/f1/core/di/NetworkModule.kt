package dev.remgr.f1.core.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.BuildConfig
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.core.network.interceptor.OpenF1FilterInterceptor
import dev.remgr.f1.core.network.interceptor.OpenF1NoResultsInterceptor
import dev.remgr.f1.core.network.interceptor.OpenF1RateLimitInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://f1api.remgr.dev/v1/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues  = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        rateLimitInterceptor: OpenF1RateLimitInterceptor,
        noResultsInterceptor: OpenF1NoResultsInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(rateLimitInterceptor)
        .addInterceptor(OpenF1FilterInterceptor())
        .addInterceptor(noResultsInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "f1-android/${BuildConfig.VERSION_NAME}")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            },
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideOpenF1Service(retrofit: Retrofit): OpenF1Service =
        retrofit.create(OpenF1Service::class.java)
}
