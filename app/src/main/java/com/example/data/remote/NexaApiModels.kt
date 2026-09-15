package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ResendOtpRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class ApiUserData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String,
    @Json(name = "isVerified") val isVerifiedCamel: Boolean? = null,
    @Json(name = "is_verified") val isVerifiedSnake: Boolean? = null,
    @Json(name = "isPremium") val isPremiumCamel: Boolean? = null,
    @Json(name = "is_premium") val isPremiumSnake: Boolean? = null,
    @Json(name = "subscriptionPlan") val subscriptionPlanCamel: String? = null,
    @Json(name = "subscription_plan") val subscriptionPlanSnake: String? = null,
    @Json(name = "freeAnalysisCount") val freeAnalysisCountCamel: Int? = null,
    @Json(name = "free_analysis_count") val freeAnalysisCountSnake: Int? = null,
    @Json(name = "last_analysis_date") val lastAnalysisDate: String? = null,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val isVerified: Boolean get() = isVerifiedCamel ?: isVerifiedSnake ?: false
    val isPremium: Boolean get() = isPremiumCamel ?: isPremiumSnake ?: false
    val subscriptionPlan: String get() = subscriptionPlanCamel ?: subscriptionPlanSnake ?: "FREE"
    val freeAnalysisCount: Int get() = freeAnalysisCountCamel ?: freeAnalysisCountSnake ?: 0
}

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "requireOtp") val requireOtpCamel: Boolean? = null,
    @Json(name = "require_otp") val requireOtpSnake: Boolean? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "refreshToken") val refreshTokenCamel: String? = null,
    @Json(name = "refresh_token") val refreshTokenSnake: String? = null,
    @Json(name = "user") val user: ApiUserData? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val requireOtp: Boolean get() = requireOtpCamel ?: requireOtpSnake ?: false
    val refreshToken: String? get() = refreshTokenCamel ?: refreshTokenSnake
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class UserMeResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "user") val user: ApiUserData? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class AnalyzeChartRequest(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "timeframe") val timeframe: String,
    @Json(name = "base64Image") val base64Image: String,
    @Json(name = "mimeType") val mimeType: String = "image/jpeg"
)

@JsonClass(generateAdapter = true)
data class ApiAnalysisData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "timeframe") val timeframe: String = "",
    @Json(name = "signal") val signal: String = "NEUTRAL",
    @Json(name = "confidence") val confidence: Int = 0,
    @Json(name = "reasons") val reasons: Any? = null, // Can be List<String> or String or null
    @Json(name = "entry") val entryCamel: String? = null,
    @Json(name = "entry_price") val entryPriceSnake: String? = null,
    @Json(name = "stopLoss") val stopLossCamel: String? = null,
    @Json(name = "stop_loss") val stopLossSnake: String? = null,
    @Json(name = "takeProfit") val takeProfitCamel: String? = null,
    @Json(name = "take_profit") val takeProfitSnake: String? = null,
    @Json(name = "riskLevel") val riskLevelCamel: String? = null,
    @Json(name = "risk_level") val riskLevelSnake: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "isChart") val isChartCamel: Boolean? = null,
    @Json(name = "is_chart") val isChartSnake: Boolean? = null,
    @Json(name = "invalidReason") val invalidReasonCamel: String? = null,
    @Json(name = "invalid_reason") val invalidReasonSnake: String? = null
) {
    val entryPrice: String get() = entryCamel ?: entryPriceSnake ?: "-"
    val stopLoss: String get() = stopLossCamel ?: stopLossSnake ?: "-"
    val takeProfit: String get() = takeProfitCamel ?: takeProfitSnake ?: "-"
    val riskLevel: String get() = riskLevelCamel ?: riskLevelSnake ?: "LOW"
    val isChart: Boolean get() = isChartCamel ?: isChartSnake ?: true
    val invalidReason: String? get() = invalidReasonCamel ?: invalidReasonSnake
}

@JsonClass(generateAdapter = true)
data class AnalyzeChartResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "analysis") val analysis: ApiAnalysisData? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "isChart") val isChartCamel: Boolean? = null,
    @Json(name = "is_chart") val isChartSnake: Boolean? = null,
    @Json(name = "invalidReason") val invalidReasonCamel: String? = null,
    @Json(name = "invalid_reason") val invalidReasonSnake: String? = null
) {
    val isChart: Boolean get() = isChartCamel ?: isChartSnake ?: true
    val invalidReason: String? get() = invalidReasonCamel ?: invalidReasonSnake
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class AnalysisHistoryResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "history") val historyCamel: List<ApiAnalysisData>? = null,
    @Json(name = "analyses") val historySnake: List<ApiAnalysisData>? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val history: List<ApiAnalysisData> get() = historyCamel ?: historySnake ?: emptyList()
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class CheckoutRequest(
    @Json(name = "plan") val plan: String
)

@JsonClass(generateAdapter = true)
data class CheckoutResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "checkout_url") val checkoutUrl: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class ApiPlanData(
    @Json(name = "plan_key") val planKey: String,
    @Json(name = "name") val name: String,
    @Json(name = "price_toman") val priceToman: Int,
    @Json(name = "duration_days") val durationDays: Int,
    @Json(name = "discount_percent") val discountPercent: Int = 0
)

@JsonClass(generateAdapter = true)
data class PricingPlansResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "plans") val plans: List<ApiPlanData>? = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "status") val status: String = "OFFLINE",
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "db") val db: String? = null
)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}

@JsonClass(generateAdapter = true)
data class VerifyMyketPurchaseRequest(
    @Json(name = "packageName") val packageName: String,
    @Json(name = "sku") val sku: String,
    @Json(name = "purchaseToken") val purchaseToken: String,
    @Json(name = "planKey") val planKey: String,
    @Json(name = "orderId") val orderId: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyMyketPurchaseResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "isPremium") val isPremium: Boolean = false,
    @Json(name = "subscriptionPlan") val subscriptionPlan: String? = null,
    @Json(name = "planName") val planName: String? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val errorDetail: ApiErrorDetail? = null
) {
    val error: String? get() = errorDetail?.message ?: errorDetail?.code
}
