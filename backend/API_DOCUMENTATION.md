# NEXA AI Backend API Documentation (v2.0.0)

مستندات کامل APIهای بک‌اند برنامه **NEXA AI** برای اتصال موبایل و وب.

---

## ۱. احراز هویت (Auth Endpoints)

### `POST /api/auth/register`
ثبت‌نام کاربر جدید.
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "secretpassword"
}
```
- **Response (OTP_ENABLED=false):**
```json
{
  "success": true,
  "message": "ثبت‌نام با موفقیت انجام شد.",
  "token": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "user": {
    "id": "usr_123456789",
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
ورود کاربر.
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "secretpassword"
}
```
- **Response:**
```json
{
  "success": true,
  "message": "ورود با موفقیت انجام شد.",
  "token": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "user": {
    "id": "usr_123456789",
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
بازنشانی توکن با امنیت **Refresh Token Rotation**. (توکن قبلی باطل می‌شود).
- **Request Body:**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```
- **Response:**
```json
{
  "success": true,
  "token": "NEW_ACCESS_TOKEN",
  "refreshToken": "NEW_REFRESH_TOKEN"
}
```

---

### `POST /api/auth/send-otp`
ارسال یا تولید کد ۶ رقمی تایید ایمیل.
- **Request Body:**
```json
{
  "email": "user@example.com"
}
```

---

### `POST /api/auth/verify-otp`
تایید کد OTP.
- **Request Body:**
```json
{
  "email": "user@example.com",
  "otpCode": "123456"
}
```

---

### `GET /api/auth/me`
دریافت پروفایل کاربر جاری (نیازمند Header: `Authorization: Bearer <token>`).

---

## ۲. تحلیل چارت (Chart Analysis Endpoints)

### `POST /api/analyze`
ارسال تصویر چارت برای تحلیل هوش مصنوعی Gemini.
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "symbol": "BTCUSDT",
  "timeframe": "15m",
  "base64Image": "iVBORw0KGgoAAAANSUhEUg...",
  "mimeType": "image/png"
}
```
- **Response (موفق):**
```json
{
  "success": true,
  "analysis": {
    "id": "ana_987654321",
    "symbol": "BTCUSDT",
    "timeframe": "15m",
    "signal": "LONG",
    "confidence": 85,
    "entry": "64,250",
    "stopLoss": "63,800",
    "takeProfit": "65,500",
    "reasons": [
      "شکست سطح مقاومت محلی در تایم‌فریم ۱۵ دقیقه همراه با رشد حجم",
      "تشکیل الگوی کندلی چکش صعودی روی ناحیه حمایتی ۶۴,۱۰۰",
      "رعایت ساختار سقف‌ها و کف‌های بالاتر (Higher Highs)"
    ],
    "riskLevel": "MEDIUM"
  },
  "remainingFreeAnalyses": 2
}
```
- **Response (تصویر چارت نیست):**
```json
{
  "success": false,
  "analysis": {
    "isChart": false,
    "invalidReason": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست. لطفاً اسکرین‌شات چارت قیمتی ارسال کنید."
  },
  "message": "تصویر ارسال شده یک چارت معاملاتی معتبر نیست."
}
```

---

### `GET /api/user/analyses` یا `GET /api/analysis/history`
دریافت تاریخچه تحلیلهای کاربر جاری.

---

## ۳. اشتراک و پرداخت (Subscription & Payments)

### `GET /api/subscription`
بررسی وضعیت دقیق اشتراک و اعتبارات رایگان کاربر.
- **Headers:** `Authorization: Bearer <token>`
- **Response:**
```json
{
  "success": true,
  "isPremium": false,
  "plan": "FREE",
  "status": "INACTIVE",
  "subscriptionDetails": null,
  "freeUsage": {
    "limit": 3,
    "used": 1,
    "remaining": 2
  }
}
```

---

### `POST /api/payments`
ایجاد سفارش پرداخت (Payment Intent).
- **Request Body:**
```json
{
  "planName": "PRO_MONTHLY",
  "amountToman": 250000
}
```

---

### `GET /api/payments`
مشاهده لیست تراکنشهای پرداخت کاربر.
