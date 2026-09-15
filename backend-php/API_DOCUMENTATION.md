# NEXA AI PHP Backend API Documentation

این مستندات تمام **Endpointهای REST API** نسخه PHP 8.x + MySQL را با ساختار دقیقی که اپلیکیشن Android انتظار دارد ارائه می‌دهد.

---

## 1. Authentication Endpoints

### `POST /api/auth/register`
**توضیحات:** ثبت‌نام کاربر جدید.

* **Header:** `Content-Type: application/json`
* **Body:**
```json
{
  "email": "user@example.com",
  "password": "secretpassword"
}
```
* **پاسخ موفق (201 Created):**
```json
{
  "success": true,
  "message": "ثبت‌نام با موفقیت انجام شد.",
  "token": "eyJhbGciOiJIUzI1NiI...",
  "refreshToken": "eyJhbGciOiJIUzI1NiI...",
  "user": {
    "id": "usr_a1b2c3d4e5f6g7h8",
    "email": "user@example.com",
    "isVerified": true,
    "isPremium": false,
    "subscriptionPlan": "FREE",
    "freeAnalysisCount": 0
  }
}
```

---

### `POST /api/auth/login`
**توضیحات:** ورود به حساب کاربری.

* **Body:**
```json
{
  "email": "user@example.com",
  "password": "secretpassword"
}
```
* **پاسخ موفق (200 OK):**
```json
{
  "success": true,
  "message": "ورود با موفقیت انجام شد.",
  "token": "eyJhbGciOiJIUzI1NiI...",
  "refreshToken": "eyJhbGciOiJIUzI1NiI...",
  "user": {
    "id": "usr_a1b2c3d4e5f6g7h8",
    "email": "user@example.com",
    "isVerified": true,
    "isPremium": false,
    "subscriptionPlan": "FREE",
    "freeAnalysisCount": 0
  }
}
```

---

### `POST /api/auth/refresh`
**توضیحات:** دریافت Access Token جدید با استفاده از Refresh Token (به همراه Refresh Token Rotation).

* **Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiI..."
}
```
* **پاسخ موفق (200 OK):**
```json
{
  "success": true,
  "token": "NEW_ACCESS_TOKEN",
  "refreshToken": "NEW_REFRESH_TOKEN"
}
```

---

### `GET /api/auth/me`
**توضیحات:** دریافت مشخصات حساب جاری کاربر.

* **Header:** `Authorization: Bearer <ACCESS_TOKEN>`
* **پاسخ موفق (200 OK):**
```json
{
  "success": true,
  "user": {
    "id": "usr_a1b2c3d4e5f6g7h8",
    "email": "user@example.com",
    "isVerified": true,
    "isPremium": false,
    "subscriptionPlan": "FREE",
    "freeAnalysisCount": 0,
    "createdAt": "2026-08-10 16:00:00"
  }
}
```

---

## 2. Chart Analysis API

### `POST /api/analyze`
**توضیحات:** ارسال تصویر چارت و دریافت تحلیل هوش مصنوعی Gemini 2.5.

* **Header:** `Authorization: Bearer <ACCESS_TOKEN>`
* **Body:**
```json
{
  "symbol": "BTCUSDT",
  "timeframe": "15m",
  "base64Image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "mimeType": "image/jpeg"
}
```

* **پاسخ موفق چارت (200 OK):**
```json
{
  "success": true,
  "analysis": {
    "id": "ana_x9y8z7w6v5u4",
    "symbol": "BTCUSDT",
    "timeframe": "15m",
    "signal": "LONG",
    "confidence": 85,
    "entry": "68,450",
    "stopLoss": "67,800",
    "takeProfit": "70,200",
    "reasons": [
      "شکست الگوی پرچم صعودی با حجم معاملات بالا",
      "تایید اندیکاتور RSI بالای سطح ۵۰",
      "پولبک به سطح حمایت استاتیک"
    ],
    "riskLevel": "LOW"
  },
  "remainingFreeAnalyses": 2
}
```

* **پاسخ غیر چارت (400 Bad Request):**
```json
{
  "success": false,
  "message": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست.",
  "error": {
    "code": "INVALID_CHART_IMAGE",
    "message": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست."
  },
  "analysis": {
    "isChart": false,
    "invalidReason": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست."
  }
}
```

---

## 3. User & History APIs

### `GET /api/user/me`
**توضیحات:** دریافت اطلاعات پروفایل کاربر به همراه سقف باقی‌مانده تحلیلهای رایگان.

* **Header:** `Authorization: Bearer <ACCESS_TOKEN>`
* **پاسخ (200 OK):**
```json
{
  "success": true,
  "user": {
    "id": "usr_a1b2c3d4e5f6g7h8",
    "email": "user@example.com",
    "isPremium": false,
    "freeAnalysisCount": 1,
    "freeAnalysisLimit": 3,
    "remainingFreeAnalyses": 2
  }
}
```

---

### `GET /api/user/analyses` یا `GET /api/analysis/history`
**توضیحات:** دریافت تاریخچه تحلیلهای قبلی همین کاربر.

* **Header:** `Authorization: Bearer <ACCESS_TOKEN>`
* **پاسخ (200 OK):**
```json
{
  "success": true,
  "count": 1,
  "analyses": [
    {
      "id": "ana_x9y8z7w6v5u4",
      "symbol": "BTCUSDT",
      "timeframe": "15m",
      "signal": "LONG",
      "confidence": 85,
      "reasons": ["شکست الگوی پرچم صعودی..."],
      "entryPrice": "68,450",
      "stopLoss": "67,800",
      "takeProfit": "70,200",
      "riskLevel": "LOW",
      "createdAt": "2026-08-10 16:30:00"
    }
  ]
}
```

---

## 4. Subscription & Payments

### `GET /api/subscription`
**توضیحات:** دریافت وضعیت اشتراک کاربر.

* **Header:** `Authorization: Bearer <ACCESS_TOKEN>`

---

### `POST /api/payments`
**توضیحات:** ثبت درخواست پرداخت.

* **Body:**
```json
{
  "planName": "MONTHLY_PRO",
  "amountToman": 290000
}
```
