package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DatabaseConfig
import com.example.data.local.entity.UserEntity
import com.example.data.remote.HealthResponse
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SignalLongGreen
import com.example.ui.theme.SignalShortRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.util.AppLanguage
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileScreen(
    language: AppLanguage,
    currentUser: UserEntity?,
    serverHealth: HealthResponse? = null,
    onCheckHealth: () -> Unit = {},
    onLanguageToggle: () -> Unit,
    onLogoutClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val telegramChannelUrl = "https://t.me/NexaAI_Official"
    val telegramSupportUrl = "https://t.me/NexaAiSupport"

    fun openTelegram(url: String) {
        try {
            val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(telegramIntent)
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("nav_profile", language),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BrandPurple.copy(alpha = 0.2f))
                        .border(2.dp, BrandPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.email ?: "user@nexa.ai",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (currentUser?.isPremium == true) Strings.get("plan_premium_active", language) else Strings.get("plan_free_active", language),
                    fontSize = 13.sp,
                    color = if (currentUser?.isPremium == true) WarningGold else BrandCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Status & Expiration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = WarningGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Strings.get("subscription_status", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

//                if (currentUser?.isPremium == true) {
//                    val expDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(currentUser.subscriptionExpiryDate))
//                    Text(
//                        text = "${Strings.get("expiry_date", language)} $expDate",
//                        fontSize = 13.sp,
//                        color = TextSecondary
//                    )
//                } else {
//                    Text(
//                        text = Strings.get("quota_exhausted_msg", language),
//                        fontSize = 13.sp,
//                        color = TextSecondary
//                    )
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(
//                        onClick = onUpgradeClick,
//                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
//                        shape = RoundedCornerShape(12.dp)
//                    ) {
//                        Text(text = Strings.get("buy_plan", language), fontWeight = FontWeight.Bold)
//                    }
//                }

                if (currentUser?.isPremium == true) {

                    val expDate = SimpleDateFormat(
                        "yyyy/MM/dd",
                        Locale.getDefault()
                    ).format(Date(currentUser.subscriptionExpiryDate))

                    Text(
                        text = "${Strings.get("expiry_date", language)} $expDate",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                } else {

                    val usedFreeAnalyses = currentUser?.freeAnalysisCount ?: 0
                    val remainingFreeAnalyses =
                        (3 - usedFreeAnalyses).coerceIn(0, 3)

                    if (remainingFreeAnalyses > 0) {

                        Text(
                            text = if (language == AppLanguage.FA) {
                                "امروز $remainingFreeAnalyses تحلیل رایگان برای شما باقی مانده است."
                            } else {
                                "You have $remainingFreeAnalyses free analyses remaining today."
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                    } else {

                        Text(
                            text = Strings.get("quota_exhausted_msg", language),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onUpgradeClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = Strings.get("buy_plan", language),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Health Check & API Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val isHealthy = serverHealth?.success == true || serverHealth?.status == "ok"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = BrandCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Strings.get("db_connection_status", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isHealthy) SignalLongGreen else SignalShortRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHealthy) "متصل" else "قطع",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHealthy) SignalLongGreen else SignalShortRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isHealthy) "ارتباط با سرور مرکزی برقرار است" else "عدم دسترسی به سرور مرکزی",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHealthy) BrandCyan else TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onCheckHealth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_server_health_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = BrandCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("check_server_health", language),
                            color = BrandCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telegram Community & Support
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.FA) {
                        "ارتباط و پشتیبانی"
                    } else {
                        "Community & Support"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (language == AppLanguage.FA) {
                        "اخبار، اطلاع‌رسانی و پشتیبانی Nexa AI"
                    } else {
                        "News, updates and Nexa AI support"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Telegram Channel
                OutlinedButton(
                    onClick = {
                        openTelegram(telegramChannelUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        BrandCyan.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (language == AppLanguage.FA) {
                                    "کانال اطلاع‌رسانی"
                                } else {
                                    "News & Updates"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (language == AppLanguage.FA) {
                                    "اخبار و آخرین بروزرسانی‌ها"
                                } else {
                                    "News and latest updates"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Telegram Support
                OutlinedButton(
                    onClick = {
                        openTelegram(telegramSupportUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        BrandPurple.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (language == AppLanguage.FA) {
                                    "پشتیبانی تلگرام"
                                } else {
                                    "Telegram Support"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (language == AppLanguage.FA) {
                                    "مشکل یا سوالی دارید؟ با ما در ارتباط باشید"
                                } else {
                                    "Need help? Contact our support team"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switch Option Button

        // Language Switch Option Button
        OutlinedButton(
            onClick = onLanguageToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_lang_toggle_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = BrandCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${Strings.get("switch_lang", language)} (${language.displayName})",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = SignalShortRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("logout", language),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
