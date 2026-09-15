package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SignalLongGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.util.AppLanguage
import com.example.util.Strings

data class SubscriptionPlanItem(
    val planKey: String,
    val titleKey: String,
    val priceKey: String,
    val amountToman: Int,
    val referencePriceKey: String? = null,
    val badgeKey: String? = null,
    val isHighlighted: Boolean = false,
    val durationMonths: Int = 1
)

@Composable
fun PricingScreen(
    language: AppLanguage,
    onSubscribePlan: (planKey: String, amountToman: Int) -> Unit
) {
    val scrollState = rememberScrollState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPlanForPayment by remember { mutableStateOf<SubscriptionPlanItem?>(null) }

    val plans = listOf(
        SubscriptionPlanItem(
            planKey = "MONTHLY",
            titleKey = "plan_monthly",
            priceKey = "price_monthly",
            amountToman = 259000,
            referencePriceKey = null,
            badgeKey = null,
            isHighlighted = false,
            durationMonths = 1
        ),
        SubscriptionPlanItem(
            planKey = "QUARTERLY",
            titleKey = "plan_quarterly",
            priceKey = "price_quarterly",
            amountToman = 599000,
            referencePriceKey = "price_quarterly_ref",
            badgeKey = "tag_discount_23",
            isHighlighted = true,
            durationMonths = 3
        ),
        SubscriptionPlanItem(
            planKey = "SEMI_ANNUAL",
            titleKey = "plan_semiannual",
            priceKey = "price_semiannual",
            amountToman = 999000,
            referencePriceKey = "price_semiannual_ref",
            badgeKey = "tag_discount_36",
            isHighlighted = false,
            durationMonths = 6
        ),
        SubscriptionPlanItem(
            planKey = "ANNUAL",
            titleKey = "plan_annual",
            priceKey = "price_annual",
            amountToman = 1599000,
            referencePriceKey = "price_annual_ref",
            badgeKey = "tag_discount_49",
            isHighlighted = false,
            durationMonths = 12
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VIP Header Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(BrandIndigo.copy(alpha = 0.5f), BrandPurple.copy(alpha = 0.5f))
                    )
                )
                .border(1.dp, BrandCyan.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.FA) "دسترسی نامحدود VIP" else "VIP UNLIMITED ACCESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = Strings.get("pricing_title", language),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = Strings.get("pricing_sub", language),
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Features Checklist Pill Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(DarkBorder, BrandPurple.copy(alpha = 0.3f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val perks = if (language == AppLanguage.FA) {
                    listOf(
                        "تحلیل نامحدود چارت با موتور هوش مصنوعی NEXA",
                        "شناسایی دقیق نقاط ورود، حد سود (TP) و حد ضرر (SL)",
                        "پشتیبانی از تمام بازارهای کریپتو، فارکس و بورس",
                        "اولویت پردازش بالا در سرورهای ابری"
                    )
                } else {
                    listOf(
                        "Unlimited chart analyses with NEXA AI Engine",
                        "Accurate Entry, Take Profit & Stop Loss targets",
                        "Full support for Crypto, Forex & Global Markets",
                        "High priority cloud server processing"
                    )
                }

                perks.forEach { perk ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(SignalLongGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SignalLongGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = perk,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Plans List
        plans.forEach { plan ->
            PricingPlanCard(
                language = language,
                plan = plan,
                onSelect = {
                    selectedPlanForPayment = plan
                    showPaymentDialog = true
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Guarantee footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (language == AppLanguage.FA) "پرداخت امن از طریق درگاه پرداخت درون‌برنامه‌ای مایکت" else "Secure payment powered by Myket In-App Billing",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Payment Gateway Dialog
    if (showPaymentDialog && selectedPlanForPayment != null) {
        val plan = selectedPlanForPayment!!
        Dialog(onDismissRequest = { showPaymentDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(BrandCyan, BrandPurple))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(BrandIndigo, BrandPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = Strings.get("zarinpal_sim", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plan Info Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.get(plan.titleKey, language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = Strings.get(plan.priceKey, language),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onSubscribePlan(plan.planKey, plan.amountToman)
                            showPaymentDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pay_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SignalLongGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Strings.get("buy_plan", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text(
                            text = if (language == AppLanguage.FA) "انصراف" else "Cancel",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PricingPlanCard(
    language: AppLanguage,
    plan: SubscriptionPlanItem,
    onSelect: () -> Unit
) {
    val isPop = plan.isHighlighted
    val cardBrush = if (isPop) {
        Brush.linearGradient(listOf(BrandCyan, BrandPurple, BrandIndigo))
    } else {
        Brush.linearGradient(listOf(DarkBorder, DarkBorder.copy(alpha = 0.5f)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.planKey}")
            .then(
                if (isPop) {
                    Modifier.shadow(12.dp, RoundedCornerShape(22.dp), spotColor = BrandPurple)
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isPop) DarkSurfaceVariant else DarkSurface
        ),
        shape = RoundedCornerShape(22.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = cardBrush,
            width = if (isPop) 2.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Badges Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plan.badgeKey != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPop) {
                                    Brush.linearGradient(listOf(WarningGold.copy(alpha = 0.25f), BrandPurple.copy(alpha = 0.35f)))
                                } else {
                                    Brush.linearGradient(listOf(BrandCyan.copy(alpha = 0.2f), BrandCyan.copy(alpha = 0.1f)))
                                }
                            )
                            .border(
                                1.dp,
                                if (isPop) WarningGold else BrandCyan,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPop) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = WarningGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = Strings.get(plan.badgeKey, language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPop) WarningGold else BrandCyan
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // VIP Status indicator
                Text(
                    text = "VIP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPop) BrandCyan else TextSecondary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Plan Title & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.get(plan.titleKey, language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (plan.referencePriceKey != null) {
                        Text(
                            text = Strings.get(plan.referencePriceKey, language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary.copy(alpha = 0.65f),
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = Strings.get(plan.priceKey, language),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (isPop) BrandCyan else TextPrimary
                    )
                }

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPop) BrandPurple else BrandIndigo
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = Strings.get("buy_plan", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
