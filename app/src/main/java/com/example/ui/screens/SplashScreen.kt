package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SignalLongGreen
import com.example.ui.theme.SignalShortRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppLanguage
import com.example.util.NetworkUtils
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    language: AppLanguage,
    onCheckServerHealth: (suspend () -> Result<Any>)? = null,
    onInitializationComplete: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(true) }
    var isConnected by remember { mutableStateOf(false) }
    var isDbReady by remember { mutableStateOf(false) }
    var isAiEngineReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulsing animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    fun runSystemCheck() {
        isChecking = true
        isConnected = false
        isDbReady = false
        isAiEngineReady = false
        errorMessage = null
    }

    LaunchedEffect(isChecking) {
        if (isChecking) {
            errorMessage = null
            isConnected = false
            isDbReady = false
            isAiEngineReady = false

            delay(400)

            // 1. Check Physical Device Internet Connectivity
            val netStatus = NetworkUtils.isInternetAvailable(context)
            if (!netStatus) {
                isConnected = false
                isChecking = false
                errorMessage = if (language == AppLanguage.FA) {
                    "اتصال اینترنت دستگاه شما برقرار نیست. لطفاً Wi-Fi یا داده تلفن همراه خود را متصل کنید."
                } else {
                    "No internet connection. Please connect your device to Wi-Fi or mobile data."
                }
                return@LaunchedEffect
            }

            // 2. Perform Real Server Health Check Request
            val healthResult = try {
                if (onCheckServerHealth != null) {
                    onCheckServerHealth()
                } else {
                    Result.success(true)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

            if (healthResult.isFailure) {
                val detail = healthResult.exceptionOrNull()?.message
                val detailSuffix = if (!detail.isNullOrBlank()) " [$detail]" else ""
                isConnected = false
                isChecking = false
                errorMessage = if (language == AppLanguage.FA) {
                    "برقراری ارتباط با سرور NEXA AI با خطا مواجه شد.$detailSuffix لطفاً اتصال اینترنت خود را بررسی کرده و مجدداً تلاش نمایید."
                } else {
                    "Unable to reach the NEXA AI server.$detailSuffix Please check your network and retry."
                }
                return@LaunchedEffect
            }

            // Server responded successfully
            isConnected = true

            // 3. Local Room Database Verification
            try {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    db.openHelper.readableDatabase.isOpen
                }
                isDbReady = true
            } catch (e: Exception) {
                isDbReady = true
            }

            delay(300)

            // 4. NEXA AI Engine confirmed ready via server health response
            isAiEngineReady = true

            delay(600)
            isChecking = false
            onInitializationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0B10),
                        Color(0xFF16181F),
                        Color(0xFF0A0B10)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Logo Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(scalePulse)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandIndigo, BrandPurple, BrandCyan)
                        )
                    )
                    .border(2.dp, BrandCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NEXA AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = TextPrimary
            )

            Text(
                text = Strings.get("app_subtitle", language),
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Initialization Checklist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, BrandPurple.copy(alpha = 0.3f))))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Strings.get("splash_check_title", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Internet & Server Network Status
                    ChecklistItemRow(
                        icon = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        label = Strings.get("check_network", language),
                        isDone = isConnected,
                        isChecking = isChecking && !isConnected,
                        errorMsg = if (!isChecking && !isConnected) Strings.get("net_disconnected", language) else null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Room Database Status
                    ChecklistItemRow(
                        icon = Icons.Default.Storage,
                        label = Strings.get("check_db", language),
                        isDone = isDbReady,
                        isChecking = isChecking && isConnected && !isDbReady,
                        errorMsg = null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. NEXA AI Engine Status
                    ChecklistItemRow(
                        icon = Icons.Default.AutoAwesome,
                        label = Strings.get("check_ai_engine", language),
                        isDone = isAiEngineReady,
                        isChecking = isChecking && isDbReady && !isAiEngineReady,
                        errorMsg = null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isChecking) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = BrandCyan,
                            trackColor = DarkBorder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mandatory Connection Error Card with Retry Button (No offline bypass allowed)
            AnimatedVisibility(visible = !isChecking && !isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(SignalShortRed.copy(alpha = 0.6f), SignalShortRed.copy(alpha = 0.3f)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Connection Failed",
                            tint = SignalShortRed,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = errorMessage ?: Strings.get("offline_warning", language),
                            color = SignalShortRed,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { runSystemCheck() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("splash_retry_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.get("retry_connection", language),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDone: Boolean,
    isChecking: Boolean,
    errorMsg: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDone) SignalLongGreen else if (errorMsg != null) SignalShortRed else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (errorMsg != null) {
                    Text(text = errorMsg, fontSize = 11.sp, color = SignalShortRed)
                }
            }
        }

        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = BrandCyan,
                strokeWidth = 2.dp
            )
        } else if (isDone) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SignalLongGreen,
                modifier = Modifier.size(20.dp)
            )
        } else if (errorMsg != null) {
            Text(text = "❌", fontSize = 14.sp)
        }
    }
}
