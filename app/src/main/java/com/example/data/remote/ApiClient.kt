package com.example.data.remote

import android.content.Context
import com.example.data.local.DatabaseConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var apiServiceInstance: NexaApiService? = null
    private var tokenManagerInstance: TokenManager? = null

    fun getTokenManager(context: Context): TokenManager {
        if (tokenManagerInstance == null) {
            tokenManagerInstance = TokenManager(context.applicationContext)
        }
        return tokenManagerInstance!!
    }

    fun getApiService(context: Context): NexaApiService {
        if (apiServiceInstance == null) {
            val tokenManager = getTokenManager(context)
            val baseUrl = DatabaseConfig.BASE_URL

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (com.example.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                redactHeader("Authorization")
            }

            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val token = tokenManager.getAccessToken()

                val requestBuilder = originalRequest.newBuilder()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                requestBuilder.header("Accept", "application/json")
                requestBuilder.header("User-Agent", "NEXA-AI-Android/2.0 (Linux; Android; Mobile)")

                chain.proceed(requestBuilder.build())
            }

            val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
                if (response.countPriorResponses() >= 2) {
                    return@Authenticator null
                }

                val refreshToken = tokenManager.getRefreshToken() ?: return@Authenticator null

                synchronized(this) {
                    val currentAccessToken = tokenManager.getAccessToken()
                    val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                    if (!currentAccessToken.isNullOrEmpty() && currentAccessToken != requestToken) {
                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $currentAccessToken")
                            .header("User-Agent", "NEXA-AI-Android/2.0 (Linux; Android; Mobile)")
                            .build()
                    }

                    val refreshClient = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val req = chain.request().newBuilder()
                                .header("Accept", "application/json")
                                .header("User-Agent", "NEXA-AI-Android/2.0 (Linux; Android; Mobile)")
                                .build()
                            chain.proceed(req)
                        }
                        .build()

                    val refreshRetrofit = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(refreshClient)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build()

                    val refreshService = refreshRetrofit.create(NexaApiService::class.java)

                    try {
                        val refreshResponse = refreshService.refreshTokenSync(RefreshTokenRequest(refreshToken)).execute()
                        if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                            val authData = refreshResponse.body()!!
                            val newAccess = authData.token ?: ""
                            val newRefresh = authData.refreshToken ?: refreshToken

                            tokenManager.saveTokens(newAccess, newRefresh)

                            return@Authenticator response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccess")
                                .build()
                        } else {
                            tokenManager.clearTokens()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        tokenManager.clearTokens()
                    }
                }
                null
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .authenticator(tokenAuthenticator)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiServiceInstance = retrofit.create(NexaApiService::class.java)
        }

        return apiServiceInstance!!
    }

    private fun Response.countPriorResponses(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
