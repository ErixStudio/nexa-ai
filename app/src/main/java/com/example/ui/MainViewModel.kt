package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AnalysisEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.HealthResponse
import com.example.data.remote.LiveTicker
import com.example.data.remote.MarketPriceService
import com.example.data.repository.NexaRepository
import com.example.util.AppLanguage
import com.example.util.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH, HOME, DASHBOARD, HISTORY, PRICING, PROFILE, AUTH, FAQ
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = NexaRepository(application, db)

    // Current Screen - Starts at SPLASH
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Live Ticker Market Prices Flow
    private val _liveTickers = MutableStateFlow<List<LiveTicker>>(emptyList())
    val liveTickers: StateFlow<List<LiveTicker>> = _liveTickers.asStateFlow()

    // Server Health Status State
    private val _serverHealth = MutableStateFlow<HealthResponse?>(null)
    val serverHealth: StateFlow<HealthResponse?> = _serverHealth.asStateFlow()

    // Language State
    private val _language = MutableStateFlow(AppLanguage.FA)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Current Active User Flow
    val currentUser: StateFlow<UserEntity?> = repository.currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getCurrentUserFlow(id) else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // User Analysis History Flow
    val userHistory: StateFlow<List<AnalysisEntity>> = repository.currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getUserHistoryFlow(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chart Analysis State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _latestAnalysisResult = MutableStateFlow<AnalysisEntity?>(null)
    val latestAnalysisResult: StateFlow<AnalysisEntity?> = _latestAnalysisResult.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Auth Messages & Toast Errors
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _checkoutUrl = MutableStateFlow<String?>(null)
    val checkoutUrl: StateFlow<String?> = _checkoutUrl.asStateFlow()

    init {
        // Fetch user profile and history if token exists
        viewModelScope.launch {
            repository.fetchUserProfile()
            repository.syncAnalysisHistory()
            checkServerHealth()
        }

        // Start polling live market prices
        viewModelScope.launch {
            while (true) {
                val tickers = MarketPriceService.fetchLiveTickers()
                if (tickers.isNotEmpty()) {
                    _liveTickers.value = tickers
                }
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            val result = repository.checkServerHealth()
            result.onSuccess { health ->
                _serverHealth.value = health
            }.onFailure {
                _serverHealth.value = HealthResponse(status = "error", db = "disconnected")
            }
        }
    }

//    fun navigateTo(screen: AppScreen) {
//        _currentScreen.value = screen
//    }

    fun navigateTo(screen: AppScreen) {
        // Authentication is required for all main app screens.
        // Splash and Auth are always accessible.
        if (screen != AppScreen.SPLASH &&
            screen != AppScreen.AUTH &&
            repository.currentUserId.value == null
        ) {
            _currentScreen.value = AppScreen.AUTH
            return
        }

        _currentScreen.value = screen
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA
    }

    fun clearAnalysisError() {
        _analysisError.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.loginUser(email, pass)
            result.onSuccess {
                _currentScreen.value = AppScreen.DASHBOARD
            }.onFailure { ex ->
                val msg = ex.message ?: "Incorrect email or password."
                _authError.value = msg
            }
        }
    }

    fun signup(email: String, pass: String, onOtpRequired: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.registerUser(email, pass)
            result.onSuccess { response ->
                if (response.requireOtp) {
                    onOtpRequired()
                } else {
                    _currentScreen.value = AppScreen.DASHBOARD
                }
            }.onFailure { ex ->
                val msg = ex.message ?: "Registration failed."
                _authError.value = msg
            }
        }
    }

    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.verifyOtp(email, code)
            result.onSuccess {
                _currentScreen.value = AppScreen.DASHBOARD
            }.onFailure { ex ->
                val msg = ex.message ?: "Invalid OTP verification code."
                _authError.value = msg
            }
        }
    }

    fun resendOtp(email: String) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.resendOtp(email)
            result.onFailure { ex ->
                _authError.value = ex.message ?: "Resend OTP failed."
            }
        }
    }

    fun runChartAnalysis(
        symbol: String,
        timeframe: String,
        bitmap: Bitmap?,
        imageUriStr: String?
    ) {
        val user = currentUser.value

        // Check local quota if user is free
        if (user != null && !user.isPremium && user.freeAnalysisCount >= 3) {
            _analysisError.value = "DAILY_LIMIT_REACHED"
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null

            val result = repository.executeChartAnalysis(symbol, timeframe, bitmap, imageUriStr)
            result.onSuccess { entity ->
                _latestAnalysisResult.value = entity
            }.onFailure { exception ->
                _latestAnalysisResult.value = null
                val msg = exception.message ?: ""
                if (msg.startsWith("INVALID_CHART_IMAGE")) {
                    val customMsg = msg.substringAfter("INVALID_CHART_IMAGE:").ifEmpty {
                        Strings.get("invalid_chart_image", _language.value)
                    }
                    _analysisError.value = customMsg
                } else if (msg == "DAILY_LIMIT_REACHED") {
                    _analysisError.value = "DAILY_LIMIT_REACHED"
                } else {
                    _analysisError.value = msg.ifEmpty { "An error occurred during analysis." }
                }
            }
            _isAnalyzing.value = false
        }
    }

    fun subscribePlan(planKey: String, amountToman: Int) {
        viewModelScope.launch {
            val result = repository.checkoutPlan(planKey)
            result.onSuccess { checkoutRes ->
                _checkoutUrl.value = checkoutRes.checkoutUrl
                _currentScreen.value = AppScreen.DASHBOARD
            }.onFailure { ex ->
                _analysisError.value = ex.message ?: "Payment checkout failed."
            }
        }
    }

    fun verifyMyketPurchase(
        packageName: String,
        sku: String,
        purchaseToken: String,
        planKey: String,
        orderId: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.verifyMyketPurchase(
                packageName = packageName,
                sku = sku,
                purchaseToken = purchaseToken,
                planKey = planKey,
                orderId = orderId
            )
            result.onSuccess {
                _currentScreen.value = AppScreen.DASHBOARD
                onSuccess?.invoke()
            }.onFailure { ex ->
                _analysisError.value = ex.message ?: "خطا در تایید خرید مایکت."
            }
        }
    }

    fun deleteAnalysisHistory(id: String) {
        viewModelScope.launch {
            repository.deleteAnalysis(id)
        }
    }

    fun selectHistoryItem(item: AnalysisEntity) {
        _latestAnalysisResult.value = item
        _currentScreen.value = AppScreen.DASHBOARD
    }

    suspend fun verifyServerHealth(): Result<HealthResponse> {
        val result = repository.checkServerHealth()
        result.onSuccess { health ->
            _serverHealth.value = health
        }.onFailure {
            _serverHealth.value = HealthResponse(status = "error", db = "disconnected")
        }
        return result
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = AppScreen.AUTH
    }
}
