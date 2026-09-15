# راهنمای جامع نصب و استقرار بک‌اند PHP هوش مصنوعی NEXA AI روی cPanel

این راهنما مراحل گام‌به‌گام راه‌اندازی کامل بک‌اند NEXA AI و سیستم تایید ایمیل واقعی (OTP) را روی **هاست‌های معمولی cPanel (بدون نیاز به Node.js یا دسترسی Root)** توضیح می‌دهد.

---

## 1. پیش‌نیازها
* یک هاست cPanel معمولی با پشتیبانی از **PHP 8.1 / 8.2 / 8.3**
* افزونه‌های فعال PHP: `pdo_mysql` ، `curl` ، `json` ، `mbstring` ، `openssl` (ماژول `openssl` برای سوکت‌های SMTP با SSL ضروری است).
* پایگاه داده **MySQL 8.0+** یا **MariaDB 10.5+** (از طریق phpMyAdmin در cPanel).

---

## 2. ایجاد دیتابیس MySQL در cPanel

1. وارد cPanel شوید و به بخش **MySQL Databases** بروید.
2. یک دیتابیس جدید بسازید (مثلاً: `h410448_NEXAAI`).
3. یک کاربر جدید دیتابیس بسازید (مثلاً: `h410448_admin`) و رمز عبور قوی انتخاب کنید.
4. کاربر را به دیتابیس متصل کرده و تمام دسترسی‌ها (**ALL PRIVILEGES**) را اعطا کنید.
5. وارد **phpMyAdmin** شوید، دیتابیس ساخته شده را انتخاب کرده و فایل `sql/schema.sql` را در زبانه **Import** یا **SQL** اجرا کنید.

---

## 3. آپلود فایل‌ها روی هاست

تمام فایل‌های پوشه `backend-php` را به مسیر اصلی دامنه یا ساب‌دامین آپلود کنید (`public_html` یا پوشه مربوط به `erixstudio.shop`).

### ساختار فایل‌ها روی cPanel:
```text
public_html/
│
├── .htaccess
├── .env
├── index.php
│
├── config/
│   ├── config.php
│   └── database.php
│
├── middleware/
│   ├── auth.php
│   ├── cors.php
│   └── response.php
│
├── services/
│   ├── auth_service.php
│   ├── email_service.php
│   ├── gemini_service.php
│   ├── jwt_service.php
│   ├── payment_service.php
│   └── subscription_service.php
│
├── routes/
│   ├── analysis.php
│   ├── auth.php
│   ├── payments.php
│   ├── subscription.php
│   └── user.php
│
└── sql/
    └── schema.sql
```

---

## 4. تنظیم فایل `.env` (محل قرارگیری Password واقعی SMTP)

یک فایل با نام `.env` در مسیر ریشه آپلود ایجاد کرده و اطلاعات زیر را در آن قرار دهید.

> ⚠️ **مهم:** رمز عبور واقعی اکانت ایمیل SMTP را **فقط و فقط در متغیر `MAIL_PASSWORD` داخل فایل `.env` روی سرور** قرار دهید. این رمز عبور هرگز نباید در سورس کد، لایت‌کات اندروید یا گیتهاب قرار گیرد.

```env
APP_ENV=production
APP_DEBUG=false

# سیستم تایید کد ایمیل OTP
OTP_ENABLED=true
OTP_EXPIRY_MINUTES=10

# تنظیمات ارسال ایمیل واقعی cPanel SMTP
MAIL_ENABLED=true
MAIL_HOST=mail.erixstudio.shop
MAIL_PORT=465
MAIL_USERNAME=nexaai@erixstudio.shop
MAIL_PASSWORD=YOUR_ACTUAL_EMAIL_PASSWORD_HERE
MAIL_ENCRYPTION=ssl
MAIL_FROM=nexaai@erixstudio.shop
MAIL_FROM_NAME=NEXA AI

# تنظیمات دیتابیس MySQL
DB_HOST=localhost
DB_PORT=3306
DB_NAME=h410448_NEXAAI
DB_USER=h410448_admin
DB_PASSWORD=YOUR_ACTUAL_MYSQL_PASSWORD

# کلید هوش مصنوعی Google Gemini
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.1-pro-preview

# امنیت کلید JWT
JWT_SECRET=SUPER_SECURE_RANDOM_LONG_SECRET_KEY_NEXA_AI_2026
JWT_EXPIRES_IN=604800
REFRESH_TOKEN_EXPIRES_IN=2592000

# قوانین اپلیکیشن
FREE_ANALYSIS_LIMIT=3
CORS_ORIGIN=*
```

---

## 5. عیب‌یابی و امنیت فایل `.env`

* فایل `.htaccess` موجود در پروژه دسترسی مستقیم مرورگر به `.env` را کاملاً مسدود می‌کند.
* مجوز فایل `.env` را روی **0600** یا **0644** قرار دهید.
* مجوز تمامی پوشه‌ها روی **0755** و فایل‌های `.php` روی **0644** باشد.

---

## 6. تست سیستم OTP و APIها

پس از تنظیم فایل `.env` روی سرور، آدرس‌های زیر قابل تست هستند:

1. **بررسی سلامت سرور:**
   `GET https://erixstudio.shop/api/health`

2. **ثبت‌نام و دریافت ایمیل OTP:**
   `POST https://erixstudio.shop/api/auth/register`
   پاسخ موفق شامل `"requireOtp": true` خواهد بود و یک ایمیل شامل کد ۶ رقمی به آدرس ایمیل کاربر ارسال می‌شود.

3. **تایید کد OTP:**
   `POST https://erixstudio.shop/api/auth/verify-otp`
   با ارسال ایمیل و کد ۶ رقمی، حساب تایید شده و توکن‌های ورود صادر می‌گردند.

4. **درخواست مجدد کد OTP:**
   `POST https://erixstudio.shop/api/auth/resend-otp`
   دارای محدودیت Rate Limit (حداقل ۶۰ ثانیه فاصله بین درخواست‌ها).
