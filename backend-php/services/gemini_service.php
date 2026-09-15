<?php
/**
 * NEXA AI Vision Analysis Service via Alibaba Cloud DashScope (Qwen-VL)
 * Compatible with OpenAI-style Chat Completions endpoint.
 */

require_once __DIR__ . '/../config/config.php';

function extractImageMetaData(string $base64String, ?string $providedMimeType = null): array {
    $mimeType = $providedMimeType ? strtolower(trim($providedMimeType)) : null;
    $cleanedBase64 = trim($base64String);

    if (preg_match('/^data:(image\/(png|jpeg|jpg|webp));base64,/i', $cleanedBase64, $matches)) {
        $mimeType = strtolower($matches[1]);
        if ($mimeType === 'image/jpg') $mimeType = 'image/jpeg';
        $cleanedBase64 = preg_replace('/^data:image\/(png|jpeg|jpg|webp);base64,/i', '', $cleanedBase64);
    }

    $allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!$mimeType || !in_array($mimeType, $allowedMimes)) {
        $mimeType = 'image/jpeg';
    }

    return [
        'cleanedBase64' => $cleanedBase64,
        'mimeType' => $mimeType
    ];
}

/**
 * Analyze financial chart with Qwen-VL (DashScope OpenAI-compatible endpoint)
 * Signature and return array preserved for seamless compatibility with analysis router.
 */
function analyzeChartWithGemini(string $symbol, string $timeframe, string $base64Image, ?string $mimeType = null): array {
    set_time_limit(120);
    $apiKey = env('DASHSCOPE_API_KEY');

    if (empty($apiKey) || $apiKey === 'YOUR_DASHSCOPE_API_KEY') {
        throw new Exception('کلید DASHSCOPE_API_KEY در فایل تنظیمات .env سرور تنظیم نشده است.', 500);
    }

    $imageData = extractImageMetaData($base64Image, $mimeType);
    $cleanedBase64 = $imageData['cleanedBase64'];
    $finalMimeType = $imageData['mimeType'];

    // Payload size safety check (max ~20MB)
    if (strlen($cleanedBase64) > 28 * 1024 * 1024) {
        throw new Exception('حجم تصویر ارسال شده بیش از حد مجاز (حداکثر ۲۰ مگابایت) است.', 400);
    }

    $promptText = "
You are NEXA AI, an elite quantitative financial market analyst and technical chart expert.

CRITICAL STEP 1 - STRICT FINANCIAL CHART VALIDATION:
Carefully inspect the provided image.
Verify if this image is a genuine financial or cryptocurrency trading chart (candlestick chart, price line, TradingView screenshot, MT4/MT5, exchange chart).
IF THE IMAGE IS NOT A TRADING CHART (such as a selfie, face, object, animal, non-financial app screenshot, meme, or document):
You MUST strictly return ONLY:
{
  \"isChart\": false,
  \"invalidReason\": \"تصویر ارسال شده یک چارت معاملاتی معتبر نیست. لطفاً اسکرین‌شات چارت قیمتی یا عکس واضح از نمادهای مالی ارسال کنید.\"
}

CRITICAL STEP 2 - COMPREHENSIVE TECHNICAL ANALYSIS (Only if isChart is true):
Analyze symbol '{$symbol}' on '{$timeframe}' timeframe based on visible visual evidence:
1. Examine Market Context:
   - Candlestick structure, patterns (e.g. Pinbar, Engulfing, Morning/Evening Star, Doji), and price action.
   - Key horizontal Support and Resistance levels, dynamic trendlines, chart patterns (Head & Shoulders, Double Top/Bottom, Channels, Triangles).
   - Breakout/breakdown confirmation, momentum, and volume if visible.
   - Visible technical indicators (RSI, MACD, Moving Averages, Bollinger Bands, Fibonacci) ONLY if actually plotted on the chart.
2. STRICT NEGATIVE CONSTRAINTS (NO HALLUCINATIONS):
   - Do NOT invent or guess indicators that are NOT visible on the chart.
   - Do NOT invent exact prices that cannot be reliably read from the chart price scale or candles.
   - If important information is partially obscured or unreadable, explicitly state uncertainty and adjust confidence score downward.
3. TRADE EXECUTION PARAMETERS:
   - Signal: \"LONG\", \"SHORT\", or \"NO_TRADE\" (if market structure is choppy or unclear, use \"NO_TRADE\").
   - Confidence: Integer between 10 and 98 reflecting realistic statistical strength.
   - Entry: Exact realistic price or entry zone.
   - Stop Loss (SL): Realistic price protecting capital.
   - Take Profit (TP): Realistic target price respecting nearest resistance/support.
   - Risk Level: \"LOW\", \"MEDIUM\", or \"HIGH\".
   - Reasons: Exactly 3 to 4 professional technical analysis reasons written in fluent PERSIAN explaining the confluence of indicators, price action, and structure.

Return ONLY a valid JSON object matching this schema:
{
  \"isChart\": true,
  \"invalidReason\": null,
  \"signal\": \"LONG\" | \"SHORT\" | \"NO_TRADE\",
  \"confidence\": integer between 10 and 98,
  \"reasons\": [
    \"دلیل تکنیکال اول به فارسی\",
    \"دلیل تکنیکال دوم به فارسی\",
    \"دلیل تکنیکال سوم به فارسی\"
  ],
  \"entry\": \"قیمت ورود به فارسی یا عدد\",
  \"stopLoss\": \"حد ضرر به فارسی یا عدد\",
  \"takeProfit\": \"حد سود به فارسی یا عدد\",
  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\"
}
";

    $dataUri = "data:{$finalMimeType};base64,{$cleanedBase64}";
    $messages = [
        [
            'role' => 'user',
            'content' => [
                [
                    'type' => 'text',
                    'text' => $promptText
                ],
                [
                    'type' => 'image_url',
                    'image_url' => [
                        'url' => $dataUri
                    ]
                ]
            ]
        ]
    ];

    $envConfigured = env('QWEN_MODEL');
    $configuredModel = !empty($envConfigured) ? trim($envConfigured) : 'qwen-vl-max';
    $configSource = !empty($envConfigured) ? "QWEN_MODEL (.env)" : "Default fallback";

    // Supported vision models: primary preferred model followed by fast fallback
    $supportedModels = array_values(array_unique(array_filter([
        $configuredModel,
        'qwen-vl-max',
        'qwen-vl-plus'
    ])));

    error_log("[Qwen-VL Config] Selected model: '{$configuredModel}' (Source: {$configSource}). Full fallback chain: [" . implode(', ', $supportedModels) . "]");

    $endpointUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";
    $responseData = null;
    $lastError = null;

    foreach ($supportedModels as $model) {
        $requestPayload = [
            'model' => $model,
            'messages' => $messages
        ];

        error_log("[Qwen-VL Request] Dispatching chart analysis request to model: '{$model}' via DashScope endpoint");

        $ch = curl_init($endpointUrl);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_HTTPHEADER     => [
                'Authorization: Bearer ' . $apiKey,
                'Content-Type: application/json'
            ],
            CURLOPT_POSTFIELDS     => json_encode($requestPayload, JSON_UNESCAPED_SLASHES),
            CURLOPT_TIMEOUT        => 100,
            CURLOPT_SSL_VERIFYPEER => true
        ]);

        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($httpCode === 200 && !empty($result)) {
            $responseData = json_decode($result, true);
            error_log("[Qwen-VL Response] Analysis succeeded with model: '{$model}' (HTTP 200)");
            break;
        } else {
            $rawError = $curlError ?: $result;
            $safeError = !empty($apiKey) ? str_replace($apiKey, '***HIDDEN_KEY***', $rawError) : $rawError;
            $lastError = "DashScope API HTTP Error {$httpCode}: " . $safeError;
            error_log("[Qwen-VL Warning] Model '{$model}' failed: {$lastError}");
        }
    }

    if (!$responseData) {
        throw new Exception("خطا در ارتباط با سرویس تحلیل هوش مصنوعی NEXA AI: " . ($lastError ?: "پاسخی دریافت نشد"), 502);
    }

    // Process & Validate JSON Response from choices[0].message.content
    if (!isset($responseData['choices'][0]['message']['content'])) {
        throw new Exception("ساختار پاسخ هوش مصنوعی نامعتبر بود.", 502);
    }

    $rawText = $responseData['choices'][0]['message']['content'];
    $cleanedText = preg_replace('/^```(?:json)?/i', '', trim($rawText));
    $cleanedText = preg_replace('/```$/', '', trim($cleanedText));

    $parsed = json_decode($cleanedText, true);

    if (!is_array($parsed)) {
        throw new Exception("پاسخ هوش مصنوعی قالب استاندارد تحلیلی را نداشت.", 502);
    }

    if (isset($parsed['isChart']) && $parsed['isChart'] === false) {
        return [
            'isChart' => false,
            'invalidReason' => $parsed['invalidReason'] ?? 'تصویر ارسال شده چارت معاملاتی معتبر نیست.'
        ];
    }

    $validSignal = in_array($parsed['signal'] ?? '', ['LONG', 'SHORT', 'NO_TRADE']) ? $parsed['signal'] : 'NO_TRADE';
    $validConfidence = is_numeric($parsed['confidence'] ?? null) ? max(10, min(98, (int)$parsed['confidence'])) : 50;
    $validReasons = is_array($parsed['reasons'] ?? null) && !empty($parsed['reasons']) ? $parsed['reasons'] : ['تحلیل ساختار پرایس اکشن انجام شد.'];
    $validRisk = in_array($parsed['riskLevel'] ?? '', ['LOW', 'MEDIUM', 'HIGH']) ? $parsed['riskLevel'] : 'MEDIUM';

    return [
        'isChart' => true,
        'invalidReason' => null,
        'signal' => $validSignal,
        'confidence' => $validConfidence,
        'reasons' => $validReasons,
        'entry' => $parsed['entry'] ?? 'قیمت مارکت',
        'stopLoss' => $parsed['stopLoss'] ?? '-',
        'takeProfit' => $parsed['takeProfit'] ?? '-',
        'riskLevel' => $validRisk
    ];
}

/**
 * Modern alias for analyzeChartWithGemini
 */
function analyzeChartWithQwen(string $symbol, string $timeframe, string $base64Image, ?string $mimeType = null): array {
    return analyzeChartWithGemini($symbol, $timeframe, $base64Image, $mimeType);
}
