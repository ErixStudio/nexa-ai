package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.MyketBillingManager
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.NexaTopAppBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NexaTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.Strings

import com.example.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var billingManager: MyketBillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = MyketBillingManager(this)

        setContent {
            NexaTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val userHistory by viewModel.userHistory.collectAsStateWithLifecycle()
                val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
                val latestAnalysisResult by viewModel.latestAnalysisResult.collectAsStateWithLifecycle()
                val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
                val authError by viewModel.authError.collectAsStateWithLifecycle()
                val liveTickers by viewModel.liveTickers.collectAsStateWithLifecycle()
                val serverHealth by viewModel.serverHealth.collectAsStateWithLifecycle()

                val context = LocalContext.current
                val myketBillingManager =
                    billingManager ?: remember { MyketBillingManager(context) }

                val hideBars = currentScreen == AppScreen.SPLASH || currentScreen == AppScreen.AUTH

                // Dynamic RTL / LTR layout direction provider for Persian and English
                CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.layoutDirection) {
                    Scaffold(
                        topBar = {
                            if (!hideBars) {
                                NexaTopAppBar(
                                    currentLanguage = currentLanguage,
                                    currentUser = currentUser,
                                    onLanguageToggle = { viewModel.toggleLanguage() },
                                    onProfileClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                                    onPricingClick = { viewModel.navigateTo(AppScreen.PRICING) }
                                )
                            }
                        },
                        bottomBar = {
                            if (!hideBars) {
                                NavigationBar(
                                    containerColor = DarkSurface,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.testTag("nexa_bottom_nav")
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.HOME,
                                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text(Strings.get("nav_home", currentLanguage), fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("nav_item_home")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.DASHBOARD,
                                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Dashboard") },
                                        label = { Text(Strings.get("nav_dashboard", currentLanguage), fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("nav_item_dashboard")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.HISTORY,
                                        onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = { Text(Strings.get("nav_history", currentLanguage), fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("nav_item_history")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PRICING,
                                        onClick = { viewModel.navigateTo(AppScreen.PRICING) },
                                        icon = { Icon(Icons.Default.Star, contentDescription = "Pricing") },
                                        label = { Text(Strings.get("nav_pricing", currentLanguage), fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("nav_item_pricing")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PROFILE,
                                        onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                        label = { Text(Strings.get("nav_profile", currentLanguage), fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("nav_item_profile")
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.SPLASH -> SplashScreen(
                                        language = currentLanguage,
                                        onCheckServerHealth = {
                                            viewModel.verifyServerHealth()
                                        },
                                        onInitializationComplete = {
                                            viewModel.navigateTo(AppScreen.HOME)
                                        }
                                    )

                                    AppScreen.HOME -> HomeScreen(
                                        language = currentLanguage,
                                        liveTickers = liveTickers,
                                        onStartAnalysisClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onPricingClick = { viewModel.navigateTo(AppScreen.PRICING) }
                                    )

                                    AppScreen.DASHBOARD -> DashboardScreen(
                                        language = currentLanguage,
                                        currentUser = currentUser,
                                        isAnalyzing = isAnalyzing,
                                        analysisResult = latestAnalysisResult,
                                        analysisError = analysisError,
                                        onAnalyzeClick = { symbol, timeframe, bitmap, uriStr ->
                                            viewModel.runChartAnalysis(symbol, timeframe, bitmap, uriStr)
                                        },
                                        onShareClick = { item ->
                                            try {
                                                val shareText = buildString {
                                                    append("📊 NEXA AI Trading Analysis | گزارش تحلیل چارت نکسوس\n\n")
                                                    append("🔹 نماد / Symbol: ${item.symbol}\n")
                                                    append("⏱ تایم‌فریم / Timeframe: ${item.timeframe}\n")
                                                    append("🎯 سیگنال / Signal: ${item.signal}\n")
                                                    append("📈 درصد اطمینان / Confidence: ${item.confidence}%\n")
                                                    append("🚪 نقطه ورود / Entry: ${item.entryPrice}\n")
                                                    append("🛑 حد ضرر / Stop Loss: ${item.stopLoss}\n")
                                                    append("🎯 حد سود / Take Profit: ${item.takeProfit}\n")
                                                    append("⚠️ سطح ریسک / Risk: ${item.riskLevel}\n\n")
                                                    if (item.reasonsJson.isNotEmpty()) {
                                                        val cleanReasons = item.reasonsJson
                                                            .replace("||", "\n• ")
                                                            .replace("[\"", "• ")
                                                            .replace("\"]", "")
                                                            .replace("\",\"", "\n• ")
                                                        append("📝 دلایل تحلیل:\n$cleanReasons\n\n")
                                                    }
                                                    append("🚀 تحلیل شده توسط دستیار هوش مصنوعی NEXA AI\nhttps://erixstudio.shop")
                                                }

                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری تحلیل NEXA AI")
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onUpgradeClick = { viewModel.navigateTo(AppScreen.PRICING) },
                                        onClearError = { viewModel.clearAnalysisError() }
                                    )

                                    AppScreen.HISTORY -> HistoryScreen(
                                        language = currentLanguage,
                                        historyList = userHistory,
                                        onDeleteAnalysis = { id -> viewModel.deleteAnalysisHistory(id) },
                                        onSelectAnalysis = { item -> viewModel.selectHistoryItem(item) }
                                    )

                                    AppScreen.PRICING -> PricingScreen(
                                        language = currentLanguage,
                                        onSubscribePlan = { planKey, amountToman ->
                                            val activity = context as? Activity
                                            if (activity != null) {
                                                Toast.makeText(context, "در حال اتصال به مایکت...", Toast.LENGTH_SHORT).show()
                                                myketBillingManager.startPurchase(activity, planKey) { purchaseResult ->
                                                    if (purchaseResult.isSuccess) {
                                                        viewModel.verifyMyketPurchase(
                                                            packageName = purchaseResult.packageName,
                                                            sku = purchaseResult.sku,
                                                            purchaseToken = purchaseResult.purchaseToken,
                                                            planKey = planKey,
                                                            orderId = purchaseResult.orderId,
                                                            onSuccess = {
                                                                Toast.makeText(context, "پرداخت مایکت با موفقیت تایید و اشتراک فعال شد!", Toast.LENGTH_LONG).show()
                                                            }
                                                        )
                                                    } else {
                                                        Toast.makeText(context, purchaseResult.errorMessage ?: "فرآیند خرید لغو شد.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                viewModel.subscribePlan(planKey, amountToman)
                                            }
                                        }
                                    )

                                    AppScreen.PROFILE -> ProfileScreen(
                                        language = currentLanguage,
                                        currentUser = currentUser,
                                        serverHealth = serverHealth,
                                        onCheckHealth = { viewModel.checkServerHealth() },
                                        onLanguageToggle = { viewModel.toggleLanguage() },
                                        onLogoutClick = { viewModel.logout() },
                                        onUpgradeClick = { viewModel.navigateTo(AppScreen.PRICING) }
                                    )

                                    AppScreen.AUTH -> AuthScreen(
                                        language = currentLanguage,
                                        onAuthSuccess = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                        onLoginSubmit = { email, pass -> viewModel.login(email, pass) },
                                        onSignupSubmit = { email, pass, onSuccessOtp ->
                                            viewModel.signup(email, pass, onSuccessOtp)
                                        },
                                        onOtpSubmit = { email, code -> viewModel.verifyOtp(email, code) },
                                        onResendOtp = { email -> viewModel.resendOtp(email) },
                                        errorMessage = authError
                                    )

                                    AppScreen.FAQ -> FaqScreen(language = currentLanguage)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        billingManager?.release()
    }
}
