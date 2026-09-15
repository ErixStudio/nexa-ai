package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.AnalysisEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.ScanningAnimationOverlay
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SignalLongGreen
import com.example.ui.theme.SignalLongGreenContainer
import com.example.ui.theme.SignalShortRed
import com.example.ui.theme.SignalShortRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.util.AppLanguage
import com.example.util.ImageProcessingUtils
import com.example.util.Strings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    language: AppLanguage,
    currentUser: UserEntity?,
    isAnalyzing: Boolean,
    analysisResult: AnalysisEntity?,
    analysisError: String?,
    onAnalyzeClick: (symbol: String, timeframe: String, bitmap: Bitmap?, uriStr: String?) -> Unit,
    onShareClick: (AnalysisEntity) -> Unit,
    onUpgradeClick: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Comprehensive Crypto & Forex Symbols list
    val symbolsList = remember {
        listOf(
            // Top Crypto Pairs
            "BTC/USDT", "ETH/USDT", "SOL/USDT", "BNB/USDT", "XRP/USDT",
            "ADA/USDT", "DOGE/USDT", "AVAX/USDT", "DOT/USDT", "LINK/USDT",
            "MATIC/USDT", "NEAR/USDT", "SHIB/USDT", "PEPE/USDT", "TON/USDT",
            "SUI/USDT", "LTC/USDT", "BCH/USDT", "UNI/USDT", "APT/USDT",
            "TRX/USDT", "ETC/USDT", "ATOM/USDT", "FIL/USDT", "ARBITRUM/USDT",
            "OP/USDT", "INJ/USDT", "TIA/USDT", "NOT/USDT", "RENDER/USDT",
            // Major Forex Pairs
            "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD",
            "USD/CHF", "NZD/USD", "EUR/GBP", "EUR/JPY", "GBP/JPY",
            "AUD/JPY", "CAD/JPY", "CHF/JPY", "EUR/AUD", "GBP/CAD",
            // Commodities & Metals
            "XAU/USD (Gold)", "XAG/USD (Silver)", "OIL/USD (Crude Oil)",
            // Indices & Global Markets
            "US30 (Dow Jones)", "NAS100 (Nasdaq)", "SPX500 (S&P 500)",
            "AAPL (Apple)", "NVDA (Nvidia)", "TSLA (Tesla)", "MSFT (Microsoft)"
        )
    }

    var selectedSymbol by remember { mutableStateOf("BTC/USDT") }
    var symbolSearchQuery by remember { mutableStateOf("") }
    var symbolDropdownExpanded by remember { mutableStateOf(false) }

    // Timeframes
    val timeframes = listOf("M1", "M5", "M15", "M30", "H1", "H4", "D1", "W1")
    var selectedTimeframe by remember { mutableStateOf("H1") }

    val coroutineScope = rememberCoroutineScope()

    // Selected Chart Image State (Durable URI with rememberSaveable + in-memory Bitmap)
    var selectedImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageErrorMsg by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }

    // Camera Capture Launcher (Full-Resolution via FileProvider with durable pending URI)
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // Automatically recover Bitmap from durable URI if Compose recomposes or reloads
    LaunchedEffect(selectedImageUriString) {
        val uriStr = selectedImageUriString
        if (uriStr != null && selectedBitmap == null) {
            val recovered = ImageProcessingUtils.loadBitmapFromUri(context, Uri.parse(uriStr))
            if (recovered != null) {
                selectedBitmap = recovered
            }
        }
    }

    val fullCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        val uriStr = pendingCameraUriString
        if (isSuccess && uriStr != null) {
            coroutineScope.launch {
                isImageProcessing = true
                val processed = ImageProcessingUtils.processChartImage(context, Uri.parse(uriStr))
                if (processed != null) {
                    selectedBitmap = processed.bitmap
                    selectedImageUriString = processed.fileUri.toString()
                    imageErrorMsg = null
                    onClearError()
                } else {
                    imageErrorMsg = Strings.get("invalid_chart_image", language)
                }
                isImageProcessing = false
            }
        }
    }

    val launchFullCamera = {
        val newUri = ImageProcessingUtils.createCameraImageUri(context)
        pendingCameraUriString = newUri.toString()
        fullCameraLauncher.launch(newUri)
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchFullCamera()
        } else {
            Toast.makeText(context, Strings.get("camera_permission_required", language), Toast.LENGTH_LONG).show()
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isImageProcessing = true
                val processed = ImageProcessingUtils.processChartImage(context, it)
                if (processed != null) {
                    selectedBitmap = processed.bitmap
                    selectedImageUriString = processed.fileUri.toString()
                    imageErrorMsg = null
                    onClearError()
                } else {
                    imageErrorMsg = Strings.get("invalid_chart_image", language)
                }
                isImageProcessing = false
            }
        }
    }

    // Daily Limit Alert Dialog
    if (analysisError == "DAILY_LIMIT_REACHED") {
        AlertDialog(
            onDismissRequest = onClearError,
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = WarningGold,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = Strings.get("daily_limit_reached_title", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = Strings.get("daily_limit_reached_msg", language),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearError()
                        onUpgradeClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = Strings.get("nav_pricing", language), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onClearError,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "بستن", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141722)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row with Title and Daily Free Quota Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.get("nav_dashboard", language),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            // Daily Quota Status Badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { if (currentUser?.isPremium != true) onUpgradeClick() },
                color = if (currentUser?.isPremium == true) SignalLongGreenContainer.copy(alpha = 0.3f) else BrandPurple.copy(alpha = 0.2f),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BrandCyan, BrandPurple)))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentUser?.isPremium == true) Icons.Default.WorkspacePremium else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (currentUser?.isPremium == true) SignalLongGreen else BrandCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentUser?.isPremium == true) {
                            Strings.get("vip_unlimited", language)
                        } else {
                            val count = currentUser?.freeAnalysisCount ?: 0
                            String.format(Strings.get("daily_free_status", language), count)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentUser?.isPremium == true) SignalLongGreen else BrandCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Non-Chart Image Validation Error Warning Card
        if (analysisError != null && analysisError != "DAILY_LIMIT_REACHED") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("chart_validation_error_card"),
                colors = CardDefaults.cardColors(containerColor = SignalShortRedContainer.copy(alpha = 0.25f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SignalShortRed, WarningGold)))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SignalShortRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "هشدار عدم تشخیص چارت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SignalShortRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = analysisError,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                    IconButton(onClick = onClearError) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            }
        }

        // Image Drop Zone / Upload Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { galleryLauncher.launch("image/*") }
                .testTag("chart_upload_box"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BrandPurple.copy(alpha = 0.5f), BrandCyan.copy(alpha = 0.5f))))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isImageProcessing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrandCyan,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (language == AppLanguage.FA) "در حال پردازش و بهینه‌سازی کیفیت تصویر چارت..." else "Processing and optimizing chart image...",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected Chart",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (isAnalyzing) {
                        ScanningAnimationOverlay(modifier = Modifier.fillMaxSize())
                    }
                } else if (selectedImageUriString != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrandCyan,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.FA) "در حال بازیابی تصویر..." else "Loading image...",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BrandPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = Strings.get("upload_chart", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        Text(
                            text = Strings.get("drop_chart_hint", language),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Row: Camera Capture vs Gallery Pick
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        launchFullCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("camera_btn"),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, BrandCyan.copy(alpha = 0.6f))))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Strings.get("take_photo_camera", language), fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gallery_btn"),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, BrandPurple.copy(alpha = 0.6f))))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Strings.get("pick_gallery", language), fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                }
            }
        }

        if (imageErrorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = imageErrorMsg!!,
                color = SignalShortRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Symbol Selector with Search & Custom Pair Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Strings.get("select_symbol", language),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedSymbol,
                    onValueChange = { input ->
                        selectedSymbol = input
                        symbolSearchQuery = input
                        symbolDropdownExpanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symbol_selector"),
                    placeholder = { Text("جستجو یا تایپ نماد (مثلا BTC/USDT یا EUR/USD)", fontSize = 11.sp, color = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { symbolDropdownExpanded = !symbolDropdownExpanded }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Symbol", tint = BrandCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val filteredSymbols = remember(symbolSearchQuery) {
                    if (symbolSearchQuery.isBlank()) symbolsList
                    else symbolsList.filter { it.contains(symbolSearchQuery, ignoreCase = true) }
                }

                DropdownMenu(
                    expanded = symbolDropdownExpanded && (filteredSymbols.isNotEmpty() || symbolSearchQuery.isNotBlank()),
                    onDismissRequest = { symbolDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(260.dp)
                        .background(DarkSurfaceVariant)
                ) {
                    if (symbolSearchQuery.isNotBlank() && !symbolsList.contains(symbolSearchQuery)) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "افزودن و انتخاب: \"${symbolSearchQuery.uppercase()}\"",
                                    color = BrandCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                selectedSymbol = symbolSearchQuery.uppercase()
                                symbolDropdownExpanded = false
                            }
                        )
                    }

                    filteredSymbols.forEach { sym ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sym,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    val cat = when {
                                        sym.contains("USDT") -> "کریپتو"
                                        sym.contains("/") -> "فارکس"
                                        else -> "سهام/شاخص"
                                    }
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        color = if (cat == "کریپتو") BrandCyan else WarningGold
                                    )
                                }
                            },
                            onClick = {
                                selectedSymbol = sym
                                symbolSearchQuery = ""
                                symbolDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeframe Pills
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Strings.get("select_timeframe", language),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(timeframes) { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(BrandPurple, BrandCyan))
                                else Brush.linearGradient(listOf(DarkSurface, DarkSurface))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BrandCyan else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTimeframe = tf }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tf,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Analyze Button
        Button(
            onClick = {
                if (isImageProcessing) {
                    return@Button
                }
                var bmpToUse = selectedBitmap
                val uriStr = selectedImageUriString
                if (bmpToUse == null && uriStr != null) {
                    bmpToUse = ImageProcessingUtils.loadBitmapFromUri(context, Uri.parse(uriStr))
                    if (bmpToUse != null) {
                        selectedBitmap = bmpToUse
                    }
                }
                if (bmpToUse == null) {
                    val errMsg = Strings.get("image_required_error", language)
                    imageErrorMsg = errMsg
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                } else {
                    imageErrorMsg = null
                    onAnalyzeClick(selectedSymbol, selectedTimeframe, bmpToUse, uriStr)
                }
            },
            enabled = !isAnalyzing && !isImageProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("analyze_chart_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isAnalyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Analyzing...", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("analyze_btn", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Analysis Result Card
        if (analysisResult != null) {
            val isLong = analysisResult.signal == "LONG"
            val signalColor = if (isLong) SignalLongGreen else SignalShortRed
            val signalBg = if (isLong) SignalLongGreenContainer.copy(alpha = 0.3f) else SignalShortRedContainer.copy(alpha = 0.3f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analysis_result_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(signalColor.copy(alpha = 0.8f), DarkBorder)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Result Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("analysis_result", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = { onShareClick(analysisResult) },
                            modifier = Modifier.testTag("share_analysis_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = BrandCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signal Badge & Confidence Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Signal Badge
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            color = signalBg,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(signalColor, signalColor)))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLong) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = signalColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLong) Strings.get("signal_long", language) else Strings.get("signal_short", language),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = signalColor,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Confidence Gauge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${Strings.get("confidence", language)}: ${analysisResult.confidence}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { analysisResult.confidence / 100f },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = signalColor,
                                trackColor = DarkBorder
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Execution Parameters (Entry, SL, TP)
                    Text(
                        text = Strings.get("suggested_points", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Entry
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF161925)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = Strings.get("entry_price", language), fontSize = 10.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = analysisResult.entryPrice, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                        }

                        // SL
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF161925)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = Strings.get("stop_loss", language), fontSize = 10.sp, color = SignalShortRed)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = analysisResult.stopLoss, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SignalShortRed)
                            }
                        }

                        // TP
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF161925)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = Strings.get("take_profit", language), fontSize = 10.sp, color = SignalLongGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = analysisResult.takeProfit, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SignalLongGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reasons List
                    Text(
                        text = Strings.get("reasons_title", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val reasons = analysisResult.reasonsJson.split("||")
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Disclaimer
                    Text(
                        text = Strings.get("disclaimer", language),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
