package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AnalysisEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.AnalyzeChartRequest
import com.example.data.remote.ApiAnalysisData
import com.example.data.remote.ApiUserData
import com.example.data.remote.AuthResponse
import com.example.data.remote.CheckoutRequest
import com.example.data.remote.CheckoutResponse
import com.example.data.remote.GenericResponse
import com.example.data.remote.HealthResponse
import com.example.data.remote.LoginRequest
import com.example.data.remote.NexaApiService
import com.example.data.remote.RegisterRequest
import com.example.data.remote.ResendOtpRequest
import com.example.data.remote.TokenManager
import com.example.data.remote.VerifyMyketPurchaseRequest
import com.example.data.remote.VerifyMyketPurchaseResponse
import com.example.data.remote.VerifyOtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class NexaRepository(
    private val context: Context,
    private val db: AppDatabase
) {

    private val apiService: NexaApiService = ApiClient.getApiService(context)
    private val tokenManager: TokenManager = ApiClient.getTokenManager(context)

    private val userDao = db.userDao()
    private val analysisDao = db.analysisDao()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        // Initialize user session from saved JWT
        val savedUserId = tokenManager.getUserId()
        val savedEmail = tokenManager.getUserEmail()
        val token = tokenManager.getAccessToken()
        if (!token.isNullOrEmpty()) {
            _currentUserId.value = savedUserId ?: savedEmail ?: "logged_in_user"
        }
    }

    fun getCurrentUserFlow(userId: String): Flow<UserEntity?> {
        return userDao.getUserById(userId)
    }

    fun getUserHistoryFlow(userId: String): Flow<List<AnalysisEntity>> {
        val email = tokenManager.getUserEmail() ?: ""
        return analysisDao.getAnalysisHistoryByUser(userId, email)
    }

    suspend fun fetchUserProfile(): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiUser = response.body()?.user
                if (apiUser != null) {
                    val userEntity = mapApiUserToEntity(apiUser)
                    userDao.insertOrUpdate(userEntity)
                    _currentUserId.value = userEntity.id
                    tokenManager.saveUserEmail(userEntity.email)
                    tokenManager.saveUserId(userEntity.id)
                    return@withContext Result.success(userEntity)
                }
            } else if (response.code() == 401) {
                logout()
            }
            val errorMsg = response.body()?.error ?: response.body()?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در دریافت اطلاعات کاربری."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun registerUser(email: String, passwordRaw: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val response = apiService.register(RegisterRequest(cleanEmail, passwordRaw))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
//                val token = body.token ?: ""
//                val refreshToken = body.refreshToken ?: ""
//                if (token.isNotEmpty()) {
//                    tokenManager.saveTokens(token, refreshToken)
//                }
//                tokenManager.saveUserEmail(cleanEmail)

                // Do NOT create an authenticated session before email verification.
                if (!body.requireOtp) {
                    val token = body.token ?: ""
                    val refreshToken = body.refreshToken ?: ""

                    if (token.isNotEmpty()) {
                        tokenManager.saveTokens(token, refreshToken)
                    }

                    tokenManager.saveUserEmail(cleanEmail)
                }

                body.user?.let { apiUser ->
                    val userEntity = mapApiUserToEntity(apiUser)
                    userDao.insertOrUpdate(userEntity)
                    if (!body.requireOtp) {
                        _currentUserId.value = userEntity.id
                    }
                } ?: run {
                    if (!body.requireOtp) {
                        _currentUserId.value = cleanEmail
                    }
                }

                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطای ثبت‌نام در سرور."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun verifyOtp(email: String, code: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val response = apiService.verifyOtp(VerifyOtpRequest(cleanEmail, code.trim()))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val token = body.token ?: ""
                val refreshToken = body.refreshToken ?: ""
                if (token.isNotEmpty()) {
                    tokenManager.saveTokens(token, refreshToken)
                }
                tokenManager.saveUserEmail(cleanEmail)

                body.user?.let { apiUser ->
                    val userEntity = mapApiUserToEntity(apiUser)
                    userDao.insertOrUpdate(userEntity)
                    _currentUserId.value = userEntity.id
                    tokenManager.saveUserId(userEntity.id)
                } ?: run {
                    _currentUserId.value = cleanEmail
                }

                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "کد تایید اشتباه یا منقضی شده است."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun loginUser(email: String, passwordRaw: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val response = apiService.login(LoginRequest(cleanEmail, passwordRaw))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val token = body.token ?: ""
                val refreshToken = body.refreshToken ?: ""
                if (token.isNotEmpty()) {
                    tokenManager.saveTokens(token, refreshToken)
                }
                tokenManager.saveUserEmail(cleanEmail)

                body.user?.let { apiUser ->
                    val userEntity = mapApiUserToEntity(apiUser)
                    userDao.insertOrUpdate(userEntity)
                    _currentUserId.value = userEntity.id
                    tokenManager.saveUserId(userEntity.id)
                } ?: run {
                    _currentUserId.value = cleanEmail
                }

                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "نام کاربری یا رمز عبور اشتباه است."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun resendOtp(email: String): Result<GenericResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resendOtp(ResendOtpRequest(email.trim().lowercase()))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در ارسال مجدد کد تایید."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun executeChartAnalysis(
        symbol: String,
        timeframe: String,
        bitmap: Bitmap?,
        imageUriStr: String?
    ): Result<AnalysisEntity> = withContext(Dispatchers.IO) {
        var finalBitmap = bitmap
        if (finalBitmap == null && !imageUriStr.isNullOrEmpty()) {
            finalBitmap = try {
                com.example.util.ImageProcessingUtils.loadBitmapFromUri(context, android.net.Uri.parse(imageUriStr))
            } catch (e: Exception) {
                null
            }
        }

        if (finalBitmap == null) {
            return@withContext Result.failure(Exception("INVALID_CHART_IMAGE:تصویر چارت انتخاب نشده است."))
        }

        try {
            val base64Image = bitmapToBase64(finalBitmap)
            val response = apiService.analyzeChart(AnalyzeChartRequest(symbol, timeframe, base64Image, "image/jpeg"))
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.analysis != null) {
                val apiAnalysis = body.analysis
                val isChart = body.isChart
                if (!isChart) {
                    val reason = body.invalidReason ?: apiAnalysis.invalidReason ?: "تصویر ارسال شده چارت معاملاتی نیست."
                    return@withContext Result.failure(Exception("INVALID_CHART_IMAGE:$reason"))
                }

                val currentUserId = _currentUserId.value ?: tokenManager.getUserId() ?: tokenManager.getUserEmail() ?: "user"
                val entity = mapApiAnalysisToEntity(apiAnalysis, currentUserId, imageUriStr)
                analysisDao.insertAnalysis(entity)

                // Refresh user profile after analysis to sync remaining quota
                fetchUserProfile()

                return@withContext Result.success(entity)
            }

            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در تحلیل چارت توسط سرور."
            if (response.code() == 403 || errorMsg.contains("DAILY_LIMIT", ignoreCase = true) || errorMsg.contains("سقف", ignoreCase = true)) {
                return@withContext Result.failure(Exception("DAILY_LIMIT_REACHED"))
            }

            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun syncAnalysisHistory(): Result<List<AnalysisEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAnalysisHistory()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                val historyList = body.history
                val currentUserId = _currentUserId.value ?: tokenManager.getUserId() ?: tokenManager.getUserEmail() ?: "user"
                val entities = historyList.map { mapApiAnalysisToEntity(it, currentUserId, null) }
                
                // Save synced history to Room local database
                entities.forEach { analysisDao.insertAnalysis(it) }
                return@withContext Result.success(entities)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در همگام‌سازی تاریخچه."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun deleteAnalysis(id: String) = withContext(Dispatchers.IO) {
        try {
            analysisDao.deleteAnalysisById(id)
            apiService.deleteAnalysisHistory(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun checkoutPlan(planKey: String): Result<CheckoutResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkout(CheckoutRequest(planKey))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در فرآیند پرداخت."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun verifyMyketPurchase(
        packageName: String,
        sku: String,
        purchaseToken: String,
        planKey: String,
        orderId: String? = null
    ): Result<VerifyMyketPurchaseResponse> = withContext(Dispatchers.IO) {
        try {
            val request = VerifyMyketPurchaseRequest(
                packageName = packageName,
                sku = sku,
                purchaseToken = purchaseToken,
                planKey = planKey,
                orderId = orderId
            )
            val response = apiService.verifyMyketPurchase(request)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                // Refresh profile immediately to update premium status
                fetchUserProfile()
                return@withContext Result.success(body)
            }
            val errorMsg = body?.error ?: body?.message ?: parseErrorBody(response.errorBody()?.string()) ?: "خطا در تایید خرید مایکت از سمت سرور."
            return@withContext Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun checkServerHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                return@withContext Result.success(body)
            }
            val code = response.code()
            val errorRaw = response.errorBody()?.string()?.take(150) ?: "بدون جزئیات"
            return@withContext Result.failure(Exception("پاسخ سرور ناموفق بود (HTTP $code): $errorRaw"))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleException(e)))
        }
    }

    fun logout() {
        tokenManager.clearTokens()
        _currentUserId.value = null
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        return com.example.util.ImageProcessingUtils.bitmapToBase64(bitmap, 92)
    }

    private fun mapApiUserToEntity(apiUser: ApiUserData): UserEntity {
        val userId = apiUser.id ?: apiUser.email
        return UserEntity(
            id = userId,
            email = apiUser.email,
            passwordHash = "",
            isVerified = apiUser.isVerified,
            isPremium = apiUser.isPremium,
            subscriptionPlan = apiUser.subscriptionPlan,
            subscriptionStartDate = parseDateToMillis(apiUser.startDate),
            subscriptionExpiryDate = parseDateToMillis(apiUser.endDate),
            freeAnalysisCount = apiUser.freeAnalysisCount,
            lastAnalysisDate = apiUser.lastAnalysisDate ?: "",
            createdAt = parseDateToMillis(apiUser.createdAt)
        )
    }

    private fun mapApiAnalysisToEntity(
        api: ApiAnalysisData,
        currentUserId: String,
        imageUriStr: String?
    ): AnalysisEntity {
        val analysisId = api.id ?: ("analysis_" + UUID.randomUUID().toString().take(8))
        val reasonsStr = parseReasonsJson(api.reasons)

        return AnalysisEntity(
            id = analysisId,
            userId = api.userId ?: currentUserId,
            symbol = api.symbol,
            timeframe = api.timeframe,
            imageUri = imageUriStr,
            signal = api.signal,
            confidence = api.confidence,
            reasonsJson = reasonsStr,
            entryPrice = api.entryPrice,
            stopLoss = api.stopLoss,
            takeProfit = api.takeProfit,
            riskLevel = api.riskLevel,
            createdAt = parseDateToMillis(api.createdAt)
        )
    }

    private fun parseReasonsJson(reasons: Any?): String {
        if (reasons == null) return ""
        if (reasons is List<*>) {
            return reasons.joinToString("||")
        }
        if (reasons is String) {
            return reasons
        }
        return reasons.toString()
    }

    private fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val sdf2 = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf2.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun parseErrorBody(errorJson: String?): String? {
        if (errorJson.isNullOrEmpty()) return null
        return try {
            val obj = JSONObject(errorJson)
            if (obj.has("error")) {
                val errObj = obj.optJSONObject("error")
                if (errObj != null) {
                    val msg = errObj.optString("message", "")
                    if (msg.isNotEmpty()) return msg
                    val code = errObj.optString("code", "")
                    if (code.isNotEmpty()) return code
                }
                val errStr = obj.optString("error", "")
                if (errStr.isNotEmpty() && errStr != "null") return errStr
            }
            val msg = obj.optString("message", "")
            if (msg.isNotEmpty()) return msg
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun handleException(e: Throwable): String {
        return when (e) {
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.SocketTimeoutException,
            is java.io.IOException -> "خطای ارتباط با شبکه. لطفاً اتصال اینترنت خود را بررسی کنید."
            is com.squareup.moshi.JsonDataException,
            is com.squareup.moshi.JsonEncodingException,
            is org.json.JSONException -> "خطا در پردازش اطلاعات پاسخ سرور."
            else -> e.message ?: "خطای غیرمنتظره رخ داده است."
        }
    }
}
