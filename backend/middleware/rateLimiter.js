const rateLimit = require('express-rate-limit');

// General API rate limiter
const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 200, // limit each IP to 200 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'تعداد درخواستهای شما بیش از حد مجاز است. لطفاً ۱۵ دقیقه دیگر تلاش کنید.'
  }
});

// Auth sensitive endpoints limiter (Login, Register, OTP)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 15, // Max 15 attempts per 15 minutes
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'تعداد تلاشهای ورود/ثبت‌نام بیش از حد مجاز است. لطفاً ۱۵ دقیقه دیگر تلاش کنید.'
  }
});

// Chart Analysis endpoint limiter (Prevents Gemini API spam)
const analyzeLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10, // Max 10 chart analyses per minute
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'تعداد درخواستهای تحلیل چارت بیش از حد مجاز است. لطفاً ۱ دقیقه دیگر صبر کنید.'
  }
});

module.exports = {
  globalLimiter,
  authLimiter,
  analyzeLimiter
};
