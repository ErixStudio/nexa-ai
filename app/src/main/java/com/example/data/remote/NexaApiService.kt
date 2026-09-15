package com.example.data.remote

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NexaApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<GenericResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("auth/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): Call<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserMeResponse>

    @POST("analyze")
    suspend fun analyzeChart(@Body request: AnalyzeChartRequest): Response<AnalyzeChartResponse>

    @GET("analysis/history")
    suspend fun getAnalysisHistory(): Response<AnalysisHistoryResponse>

    @DELETE("analysis/history/{id}")
    suspend fun deleteAnalysisHistory(@Path("id") id: String): Response<GenericResponse>

    @GET("payment/pricing")
    suspend fun getPricingPlans(): Response<PricingPlansResponse>

    @POST("payment/checkout")
    suspend fun checkout(@Body request: CheckoutRequest): Response<CheckoutResponse>

    @POST("payments/verify-myket")
    suspend fun verifyMyketPurchase(@Body request: VerifyMyketPurchaseRequest): Response<VerifyMyketPurchaseResponse>

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}
