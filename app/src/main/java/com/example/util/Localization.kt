package com.example.util

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val displayName: String, val layoutDirection: LayoutDirection) {
    FA("fa", "فارسی", LayoutDirection.Rtl),
    EN("en", "English", LayoutDirection.Ltr)
}

object Strings {
    private val fa = mapOf(
        "app_title" to "NEXA AI",
        "app_subtitle" to "دستیار هوشمند تحلیل چارت ترید",
        "nav_home" to "خانه",
        "nav_dashboard" to "تحلیل چارت",
        "nav_history" to "تاریخچه",
        "nav_pricing" to "اشتراک",
        "nav_profile" to "پروفایل",
        "nav_faq" to "راهنما و سوالات",
        
        // Hero / Home
        "hero_title" to "تحلیل هوشمند چارت‌های کریپتو و فارکس با موتور هوش مصنوعی NEXA AI",
        "hero_desc" to "چارت قیمت را آپلود کنید تا هوش مصنوعی الگوهای کندلی، خطوط حمایت/مقاومت، و نقاط ورود/حد ضرر/حد سود را در چند ثانیه استخراج کند.",
        "get_started" to "شروع تحلیل چارت",
        "view_pricing" to "مشاهده پلن‌های اشتراک",
        "feature_1_title" to "تحلیل مولتی‌مدال پیشرفته",
        "feature_1_desc" to "بررسی دقیق الگوهای کلاسیک و پرایس اکشن با پردازش تصویر چارت",
        "feature_2_title" to "سیگنال شفاف با SL / TP",
        "feature_2_desc" to "ارائه نقطه ورود، حد ضرر، حد سود و درصد اطمینان برای هر تحلیل",
        "feature_3_title" to "تاریخچه و مدیریت ریسک",
        "feature_3_desc" to "ذخیره تمام تحلیلهای قبلی در دیتابیس با امکان جستجو و بررسی",

        // Auth
        "login" to "ورود به حساب",
        "signup" to "ثبت نام حساب جدید",
        "email" to "پست الکترونیک (ایمیل)",
        "password" to "رمز عبور",
        "confirm_password" to "تکرار رمز عبور",
        "password_strength" to "قدرت رمز عبور: ",
        "weak" to "ضعیف",
        "medium" to "متوسط",
        "strong" to "قوی",
        "send_otp" to "ارسال کد تایید",
        "enter_otp" to "ورود کد ۶ رقمی تایید",
        "otp_sent_msg" to "کد ۶ رقمی به ایمیل شما ارسال شد",
        "otp_code" to "کد ۶ رقمی تایید (OTP)",
        "check_server_health" to "بررسی آنلاین سلامت سرور",
        "verify" to "تایید و فعال‌سازی حساب",
        "resend_code" to "ارسال مجدد کد",
        "forgot_password" to "فراموشی رمز عبور؟",
        "reset_password" to "بازنشانی رمز عبور",
        "logout" to "خروج از حساب",
        "dont_have_account" to "حساب کاربری ندارید؟ ثبت نام کنید",
        "already_have_account" to "قبلاً ثبت نام کرده‌اید؟ وارد شوید",

        // Dashboard
        "upload_chart" to "آپلود تصویر چارت",
        "drop_chart_hint" to "تصویر چارت معاملاتی را انتخاب یا به اینجا بکشید",
        "use_sample_chart" to "استفاده از چارت نمونه بیت‌کوین",
        "select_symbol" to "انتخاب نماد / جفت‌ارز",
        "select_timeframe" to "تایم‌فریم (Timeframe)",
        "analyze_btn" to "تحلیل کن با هوش مصنوعی",
        "scanning_title" to "در حال اسکن و آنالیز چارت با هوش مصنوعی NEXA AI...",
        "analyzing_hint" to "شناسایی الگوهای کندلی، حمایت و مقاومت، واگرایی‌ها...",
        "analysis_result" to "خروجی تحلیل هوشمند",
        "signal" to "سیگنال نهایی",
        "signal_long" to "خرید (LONG)",
        "signal_short" to "فروش (SHORT)",
        "confidence" to "درصد اطمینان",
        "reasons_title" to "دلایل و شواهد فنی تحلیل",
        "suggested_points" to "نقاط پیشنهادی معامله",
        "entry_price" to "نقطه ورود (Entry)",
        "stop_loss" to "حد ضرر (Stop Loss)",
        "take_profit" to "حد سود (Take Profit)",
        "risk_level" to "سطح ریسک",
        "risk_low" to "کم (Low)",
        "risk_medium" to "متوسط (Medium)",
        "risk_high" to "زیاد (High)",
        "share_export" to "اشتراک‌گذاری تحلیل",
        "saved_to_history" to "در تاریخچه ذخیره شد",
        "disclaimer" to "هشدار مهم: این تحلیل صرفاً جنبه آموزشی و کمکی دارد و مسئولیت کلیه تصمیمات معاملاتی بر عهده شخص کاربر است.",

        // Pricing / Subscription
        "pricing_title" to "پلن‌های اشتراک پریمیوم NEXA AI",
        "pricing_sub" to "برای دسترسی نامحدود به تحلیل چارت، یکی از پلن‌های زیر را انتخاب کنید:",
        "plan_monthly" to "اشتراک ماهانه",
        "plan_quarterly" to "اشتراک ۳ ماهه (فصلی)",
        "plan_semiannual" to "اشتراک ۶ ماهه",
        "plan_annual" to "اشتراک ۱ ساله (سالانه)",
        "price_monthly" to "۲۵۹,۰۰۰ تومان",
        "price_quarterly" to "۵۹۹,۰۰۰ تومان",
        "price_quarterly_ref" to "۷۷۷,۰۰۰ تومان",
        "price_semiannual" to "۹۹۹,۰۰۰ تومان",
        "price_semiannual_ref" to "۱,۵۵۴,۰۰۰ تومان",
        "price_annual" to "۱,۵۹۹,۰۰۰ تومان",
        "price_annual_ref" to "۳,۱۰۸,۰۰۰ تومان",
        "tag_discount_23" to "۲۳٪ تخفیف",
        "tag_discount_36" to "۳۶٪ تخفیف",
        "tag_discount_49" to "۴۹٪ تخفیف (بیشترین سود)",
        "tag_popular" to "پیشنهادی (۲۳٪ تخفیف)",
        "tag_best_value" to "بیشترین صرفه‌جویی (۴۹٪ تخفیف)",
        "free_quota_left" to "سهمیه رایگان باقی‌مانده: ",
        "quota_exhausted_title" to "پایان سهمیه رایگان امروز",
        "quota_exhausted_msg" to "سهمیه ۳ تحلیل رایگان امروز شما به پایان رسید. جهت ادامه و استفاده نامحدود، اشتراک پریمیوم تهیه کنید یا تا فردا صبر نمایید.",
        "buy_plan" to "خرید و فعال‌سازی اشتراک",
        "zarinpal_sim" to "پرداخت درون‌برنامه‌ای مایکت (Myket IAB)",
        "payment_success" to "پرداخت با موفقیت انجام شد! اشتراک شما فعال گردید.",

        // Profile & Settings
        "account_info" to "اطلاعات حساب کاربری",
        "subscription_status" to "وضعیت اشتراک فعلی",
        "plan_free_active" to "کاربر رایگان (Free)",
        "plan_premium_active" to "اشتراک پریمیوم فعال",
        "expiry_date" to "تاریخ انقضا: ",
        "db_connection_status" to "وضعیت اتصال دیتابیس",
        "db_info" to "دیتابیس لایه پایدار: Room SQLite (قابلیت سوئیچ به PostgreSQL/Supabase در DB_CONFIG)",
        "switch_lang" to "تغییر زبان (Fa / En)",
        "server_guide_btn" to "مشاهده کدهای سرور و SQL دیتابیس",

        // Splash & Diagnostics
        "splash_check_title" to "بررسی وضعیت اولیه سیستم",
        "check_network" to "اتصال به شبکه اینترنت و سرور",
        "check_db" to "دیتابیس محلی (Room SQLite)",
        "check_ai_engine" to "موتور پردازش هوشمند NEXA AI",
        "net_disconnected" to "قطع اتصال اینترنت",
        "offline_warning" to "برای دریافت تحلیل زنده و قیمت‌های آنلاین نیاز به اتصال اینترنت دارید.",
        "retry_connection" to "تلاش مجدد اتصال",
        "continue_offline" to "ورود به برنامه در حالت آفلاین",

        // Camera & Upload Validation
        "take_photo_camera" to "ثبت عکس با دوربین",
        "pick_gallery" to "انتخاب عکس از گالری",
        "camera_permission_required" to "جهت گرفتن عکس از چارت، دسترسی دوربین الزامی است.",
        "image_required_error" to "هشدار: لطفا ابتدا تصویر چارت را با دوربین گرفته یا از گالری آپلود کنید!",
        "live_ticker_header" to "قیمت‌های زنده بازار کریپتو (Binance Live API)",

        // Daily Limit & Chart Validation
        "daily_limit_reached_title" to "تکمیل سهمیه ۳ تحلیل رایگان امروز",
        "daily_limit_reached_msg" to "شما به حد مجاز ۳ تحلیل رایگان امروز رسیده‌اید. برای دریافت تحلیل نامحدود اشتراک VIP تهیه کنید یا فردا مراجعه فرمایید.",
        "invalid_chart_image" to "تصویر ارسال شده یک چارت معاملاتی معتبر نیست. لطفاً اسکرین‌شات چارت قیمتی یا عکس واضح از نمادهای مالی ارسال کنید.",
        "daily_free_status" to "تحلیل‌های رایگان امروز: %d از ۳",
        "vip_unlimited" to "اشتراک VIP فعال - نامحدود",

        // FAQ & Guide
        "faq_title" to "سوالات متداول و راهنمای NEXA AI",
        "q1" to "NEXA AI چطور چارت‌ها را تحلیل می‌کند؟",
        "a1" to "با استفاده از موتور اختصاصی پردازش تصویر NEXA AI، چارت قیمتی مانند یک تحلیل‌گر حرفه‌ای اسکن شده و تمام الگوها و اندیکاتورها آنالیز می‌شوند.",
        "q2" to "آیا این سیگنال‌ها تضمینی هستند؟",
        "a2" to "خیر، بازار مالی همراه با ریسک است. تحلیلهای این اپلیکیشن جنبه کمکی و دستیار تصمیم‌گیری دارند."
    )

    private val en = mapOf(
        "app_title" to "NEXA AI",
        "app_subtitle" to "AI Trading Analysis Assistant",
        "nav_home" to "Home",
        "nav_dashboard" to "Chart Analyzer",
        "nav_history" to "History",
        "nav_pricing" to "Subscription",
        "nav_profile" to "Profile",
        "nav_faq" to "Guide & FAQ",

        // Hero / Home
        "hero_title" to "Smart Crypto & Forex Chart Analysis Powered by NEXA AI Engine",
        "hero_desc" to "Upload any trading chart screenshot to extract candlestick patterns, support/resistance levels, entry, stop loss, and take profit targets in seconds.",
        "get_started" to "Start Chart Analysis",
        "view_pricing" to "View Pricing Plans",
        "feature_1_title" to "Multimodal Vision AI",
        "feature_1_desc" to "Deep recognition of price action, chart patterns, and technical trends.",
        "feature_2_title" to "Clear Signals & SL/TP",
        "feature_2_desc" to "Get entry price, stop loss, take profit targets, and confidence score.",
        "feature_3_title" to "History & Risk Control",
        "feature_3_desc" to "Save and manage all past chart analyses in a local persistent database.",

        // Auth
        "login" to "Sign In",
        "signup" to "Create Account",
        "email" to "Email Address",
        "password" to "Password",
        "confirm_password" to "Confirm Password",
        "password_strength" to "Password Strength: ",
        "weak" to "Weak",
        "medium" to "Medium",
        "strong" to "Strong",
        "send_otp" to "Send Verification Code",
        "enter_otp" to "Enter 6-Digit OTP Code",
        "otp_sent_msg" to "A 6-digit verification code has been sent to your email",
        "otp_code" to "6-Digit Verification Code (OTP)",
        "check_server_health" to "Check Backend Server Health",
        "verify" to "Verify & Activate Account",
        "resend_code" to "Resend Code",
        "forgot_password" to "Forgot Password?",
        "reset_password" to "Reset Password",
        "logout" to "Sign Out",
        "dont_have_account" to "Don't have an account? Sign Up",
        "already_have_account" to "Already have an account? Sign In",

        // Dashboard
        "upload_chart" to "Upload Chart Image",
        "drop_chart_hint" to "Click or drag your chart screenshot here",
        "use_sample_chart" to "Use Sample BTC Chart",
        "select_symbol" to "Select Symbol / Pair",
        "select_timeframe" to "Timeframe",
        "analyze_btn" to "Analyze Chart with AI",
        "scanning_title" to "Scanning & Analyzing Chart with NEXA AI Engine...",
        "analyzing_hint" to "Extracting candlestick patterns, support/resistance, RSI divergences...",
        "analysis_result" to "AI Analysis Results",
        "signal" to "Final Signal",
        "signal_long" to "BUY (LONG)",
        "signal_short" to "SELL (SHORT)",
        "confidence" to "Confidence Score",
        "reasons_title" to "Technical Evidence & Pattern Reasons",
        "suggested_points" to "Suggested Trade Execution Points",
        "entry_price" to "Entry Price",
        "stop_loss" to "Stop Loss (SL)",
        "take_profit" to "Take Profit (TP)",
        "risk_level" to "Risk Level",
        "risk_low" to "Low Risk",
        "risk_medium" to "Medium Risk",
        "risk_high" to "High Risk",
        "share_export" to "Share Analysis",
        "saved_to_history" to "Saved to Analysis History",
        "disclaimer" to "Important Notice: This analysis is strictly for educational and assistance purposes. All trading decisions and risks belong solely to the user.",

        // Pricing / Subscription
        "pricing_title" to "NEXA AI Premium Subscription Plans",
        "pricing_sub" to "Choose a plan to unlock unlimited chart analysis:",
        "plan_monthly" to "Monthly Plan",
        "plan_quarterly" to "Quarterly Plan (3 Months)",
        "plan_semiannual" to "Semi-Annual Plan (6 Months)",
        "plan_annual" to "Annual Plan (12 Months)",
        "price_monthly" to "259,000 Toman",
        "price_quarterly" to "599,000 Toman",
        "price_quarterly_ref" to "777,000 Toman",
        "price_semiannual" to "999,000 Toman",
        "price_semiannual_ref" to "1,554,000 Toman",
        "price_annual" to "1,599,000 Toman",
        "price_annual_ref" to "3,108,000 Toman",
        "tag_discount_23" to "23% OFF",
        "tag_discount_36" to "36% OFF",
        "tag_discount_49" to "49% OFF (Best Value)",
        "tag_popular" to "Recommended (23% OFF)",
        "tag_best_value" to "Best Value (49% OFF)",
        "free_quota_left" to "Remaining Free Analyses: ",
        "quota_exhausted_title" to "Free Quota Exhausted Today",
        "quota_exhausted_msg" to "You have used your 3 free chart analyses for today. Upgrade to a Premium plan to continue or wait until tomorrow.",
        "buy_plan" to "Subscribe & Activate Online",
        "zarinpal_sim" to "ZarinPal Payment Gateway (Simulator)",
        "payment_success" to "Payment Successful! Your subscription is now active.",

        // Profile & Settings
        "account_info" to "User Account Information",
        "subscription_status" to "Current Subscription Status",
        "plan_free_active" to "Free User",
        "plan_premium_active" to "Premium Subscription Active",
        "expiry_date" to "Expiration Date: ",
        "db_connection_status" to "Database Connection Status",
        "db_info" to "Backend Database: Local Room SQLite (Switchable in DB_CONFIG)",
        "switch_lang" to "Switch Language (En / Fa)",
        "server_guide_btn" to "View Server Code & SQL Schema",

        // Splash & Diagnostics
        "splash_check_title" to "Initial System Diagnostics",
        "check_network" to "Internet & Server Network Status",
        "check_db" to "Local Database (Room SQLite)",
        "check_ai_engine" to "NEXA AI Smart Engine",
        "net_disconnected" to "Internet Connection Offline",
        "offline_warning" to "Internet connection required for live AI chart analysis and online prices.",
        "retry_connection" to "Retry Connection",
        "continue_offline" to "Continue in Offline Mode",

        // Camera & Upload Validation
        "take_photo_camera" to "Take Photo with Camera",
        "pick_gallery" to "Pick from Gallery",
        "camera_permission_required" to "Camera permission required to capture chart photos.",
        "image_required_error" to "Warning: Please capture or select a chart image first before analyzing!",
        "live_ticker_header" to "Live Crypto Prices (Binance Public API)",

        // Daily Limit & Chart Validation
        "daily_limit_reached_title" to "Daily 3 Free Limit Reached",
        "daily_limit_reached_msg" to "You have reached your limit of 3 free chart analyses for today. Upgrade to VIP for unlimited access or wait until tomorrow.",
        "invalid_chart_image" to "The provided image is not a valid trading chart. Please upload a clear financial chart screenshot.",
        "daily_free_status" to "Daily Free Analyses: %d of 3",
        "vip_unlimited" to "VIP Active - Unlimited Access",

        // FAQ & Guide
        "faq_title" to "NEXA AI Guide & FAQ",
        "q1" to "How does NEXA AI analyze charts?",
        "a1" to "By leveraging the advanced NEXA AI vision engine, chart screenshots are scanned for price action patterns, trendlines, and key technical indicators.",
        "q2" to "Are these signals guaranteed?",
        "a2" to "No financial market analysis is guaranteed. NEXA AI signals serve as decision support tools."
    )

    fun get(key: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.FA -> fa[key] ?: key
            AppLanguage.EN -> en[key] ?: key
        }
    }
}
