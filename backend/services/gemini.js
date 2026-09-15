const dotenv = require('dotenv');

dotenv.config();

/**
 * Detects MIME type from Base64 string header or defaults to image/jpeg
 * @param {string} base64String
 * @param {string} [providedMimeType]
 * @returns {{ cleanedBase64: string, mimeType: string }}
 */
function extractImageMetaData(base64String, providedMimeType) {
  let mimeType = providedMimeType ? providedMimeType.trim().toLowerCase() : null;
  let cleanedBase64 = base64String.trim();

  const dataUriMatch = cleanedBase64.match(/^data:(image\/(png|jpeg|jpg|webp));base64,/i);
  if (dataUriMatch) {
    mimeType = dataUriMatch[1].toLowerCase();
    if (mimeType === 'image/jpg') mimeType = 'image/jpeg';
    cleanedBase64 = cleanedBase64.replace(/^data:image\/(png|jpeg|jpg|webp);base64,/i, '');
  }

  const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];
  if (!mimeType || !allowedMimeTypes.includes(mimeType)) {
    mimeType = 'image/jpeg';
  }

  return { cleanedBase64, mimeType };
}

/**
 * Performs professional financial chart analysis using supported Google Gemini API models.
 * @param {string} symbol - Trading pair (e.g., BTCUSDT, EURUSD)
 * @param {string} timeframe - Chart timeframe (e.g., 15m, 1h, 4h)
 * @param {string} base64Image - Pure base64 image or Data URI
 * @param {string} [mimeType] - Optional explicit MIME type (e.g. image/png)
 * @returns {Promise<Object>} Formatted & validated chart analysis result
 */
async function analyzeChartWithGemini(symbol, timeframe, base64Image, mimeType) {
  const apiKey = process.env.GEMINI_API_KEY;

  if (!apiKey || apiKey === 'YOUR_GEMINI_API_KEY') {
    throw new Error('کلید GEMINI_API_KEY در فایل تنظیمات .env سرور پیکربندی نشده است.');
  }

  const { cleanedBase64, mimeType: finalMimeType } = extractImageMetaData(base64Image, mimeType);

  // Validate Base64 payload size (Limit ~20MB)
  if (cleanedBase64.length > 28 * 1024 * 1024) {
    throw new Error('حجم تصویر ارسال شده بیش از حد مجاز (حداکثر ۲۰ مگابایت) است.');
  }

  const promptText = `
You are NEXA AI, an elite quantitative financial market analyst and technical chart expert.

CRITICAL STEP 1 - CHART IMAGE VALIDATION:
Check the provided image carefully.
Verify if this image is an actual financial trading chart (candlesticks, price line, TradingView screenshot, MT4/MT5, crypto/forex chart).
IF THE IMAGE IS NOT A TRADING CHART (e.g. selfie, face, object, animal, document, non-financial app):
Return strictly:
{
  "isChart": false,
  "invalidReason": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست. لطفاً اسکرین‌شات چارت قیمتی ارسال کنید."
}

CRITICAL STEP 2 - TECHNICAL ANALYSIS (Only if isChart is true):
Analyze symbol '${symbol}' on '${timeframe}' timeframe.
Carefully inspect visible market structures:
- Candlestick patterns
- Support and Resistance levels
- Trendlines & market direction
- Visible technical indicators (RSI, MACD, Volume, Fibonacci) - DO NOT hallucinate or guess values for indicators that are NOT visible on the chart.
- Calculate Entry, Stop Loss (SL), and Take Profit (TP) matching the exact price scale shown on the chart.
- Choose signal: "LONG", "SHORT", or "NO_TRADE" (if market structure is choppy or unclear, prefer NO_TRADE).
- Provide a realistic confidence percentage based on signal strength (10 to 98).
- Provide 3-4 professional technical analysis reasons written in PERSIAN.

Return ONLY a JSON object matching this schema:
{
  "isChart": true,
  "invalidReason": null,
  "signal": "LONG" | "SHORT" | "NO_TRADE",
  "confidence": integer between 10 and 98,
  "reasons": [
    "دلیل تکنیکال اول به فارسی",
    "دلیل تکنیکال دوم به فارسی",
    "دلیل تکنیکال سوم به فارسی"
  ],
  "entry": "قیمت ورود به فارسی یا عدد",
  "stopLoss": "حد ضرر به فارسی یا عدد",
  "takeProfit": "حد سود به فارسی یا عدد",
  "riskLevel": "LOW" | "MEDIUM" | "HIGH"
}
`;

  const requestPayload = {
    contents: [
      {
        parts: [
          { text: promptText },
          {
            inlineData: {
              mimeType: finalMimeType,
              data: cleanedBase64
            }
          }
        ]
      }
    ],
    generationConfig: {
      responseMimeType: 'application/json'
    }
  };

  // Supported production model fallbacks
  const supportedModels = ['gemini-3.1-pro-preview', 'gemini-2.5-flash'];
  let responseData = null;
  let lastError = null;

  for (const model of supportedModels) {
    try {
      const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestPayload)
      });

      if (response.ok) {
        responseData = await response.json();
        break; // Success
      } else {
        const errorText = await response.text();
        console.warn(`[Gemini API] Model '${model}' failed (${response.status}):`, errorText);
        lastError = new Error(`Gemini API Error (${response.status}): ${errorText}`);
      }
    } catch (err) {
      console.warn(`[Gemini API] Network exception calling '${model}':`, err.message);
      lastError = err;
    }
  }

  if (!responseData) {
    throw lastError || new Error('ارتباط با سرویس هوش مصنوعی Gemini برقرار نشد.');
  }

  // Safe JSON extraction & schema validation
  try {
    const candidates = responseData.candidates;
    if (!candidates || candidates.length === 0 || !candidates[0].content || !candidates[0].content.parts) {
      throw new Error('پاسخی از هوش مصنوعی دریافت نشد.');
    }

    const rawText = candidates[0].content.parts[0].text;
    const cleanedText = rawText
      .trim()
      .replace(/^```json/i, '')
      .replace(/^```/, '')
      .replace(/```$/, '')
      .trim();

    const parsedJson = JSON.parse(cleanedText);

    // If AI indicated non-chart image
    if (parsedJson.isChart === false) {
      return {
        isChart: false,
        invalidReason: parsedJson.invalidReason || 'تصویر ارسال شده چارت معاملاتی نیست.'
      };
    }

    // Validate essential analysis response fields
    const validSignal = ['LONG', 'SHORT', 'NO_TRADE'].includes(parsedJson.signal) ? parsedJson.signal : 'NO_TRADE';
    const validConfidence = typeof parsedJson.confidence === 'number' ? Math.max(10, Math.min(98, parsedJson.confidence)) : 50;
    const validReasons = Array.isArray(parsedJson.reasons) && parsedJson.reasons.length > 0 
      ? parsedJson.reasons 
      : ['الگوی پرایس اکشن و ساختار چارت تحلیل شد.'];
    const validRisk = ['LOW', 'MEDIUM', 'HIGH'].includes(parsedJson.riskLevel) ? parsedJson.riskLevel : 'MEDIUM';

    return {
      isChart: true,
      invalidReason: null,
      signal: validSignal,
      confidence: validConfidence,
      reasons: validReasons,
      entry: parsedJson.entry || 'قیمت مارکت',
      stopLoss: parsedJson.stopLoss || '-',
      takeProfit: parsedJson.takeProfit || '-',
      riskLevel: validRisk
    };
  } catch (parseError) {
    console.error('[Gemini Parse Error] Raw text parsing failed:', parseError);
    const err = new Error('پاسخ دریافت شده از هوش مصنوعی قالب استاندارد تحلیلی را نداشت.');
    err.statusCode = 502; // Bad Gateway
    throw err;
  }
}

module.exports = {
  analyzeChartWithGemini
};
