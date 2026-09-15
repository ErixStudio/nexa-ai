const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { v4: uuidv4 } = require('uuid');
const { pool } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { authLimiter } = require('../middleware/rateLimiter');
const { checkAndUpdateSubscriptionStatus } = require('../services/subscriptionService');

const router = express.Router();

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

function generateTokens(userId, email) {
  const secret = process.env.JWT_SECRET || 'DEFAULT_NEXA_SECRET_KEY_CHANGE_IN_ENV';
  const expiresIn = process.env.JWT_EXPIRES_IN || '7d';
  const refreshExpiresIn = process.env.REFRESH_TOKEN_EXPIRES_IN || '30d';

  const accessToken = jwt.sign({ userId, email }, secret, { expiresIn });
  const refreshToken = jwt.sign({ userId, email, type: 'refresh', nonce: uuidv4() }, secret, { expiresIn: refreshExpiresIn });

  return { accessToken, refreshToken };
}

// Helper to store hashed refresh token in DB
async function saveRefreshToken(userId, refreshToken) {
  const tokenHash = hashToken(refreshToken);
  const tokenId = `rt_${uuidv4().replace(/-/g, '').substring(0, 16)}`;
  const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days

  await pool.query(
    'INSERT INTO refresh_tokens (id, user_id, token_hash, is_revoked, expires_at) VALUES (?, ?, ?, 0, ?)',
    [tokenId, userId, tokenHash, expiresAt]
  );
}

// ------------------------------------------
// 1. POST /api/auth/register
// ------------------------------------------
router.post('/register', authLimiter, async (req, res, next) => {
  try {
    const { email, password } = req.body;

    if (!email || !email.includes('@')) {
      return res.status(400).json({
        success: false,
        message: 'ایمیل وارد شده معتبر نیست.'
      });
    }

    if (!password || password.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'رمز عبور باید حداقل ۶ کاراکتر باشد.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();

    // Check duplicate
    const [existing] = await pool.query('SELECT id FROM users WHERE email = ?', [cleanEmail]);
    if (existing.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'این ایمیل قبلاً در سیستم ثبت‌نام شده است.'
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const userId = `usr_${uuidv4().replace(/-/g, '').substring(0, 16)}`;
    const isOtpEnabled = process.env.OTP_ENABLED === 'true';
    const initialVerifiedStatus = isOtpEnabled ? 0 : 1;

    await pool.query(
      'INSERT INTO users (id, email, password_hash, is_verified, is_premium, subscription_plan, free_analysis_count) VALUES (?, ?, ?, ?, 0, "FREE", 0)',
      [userId, cleanEmail, passwordHash, initialVerifiedStatus]
    );

    if (isOtpEnabled) {
      // Generate OTP
      const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
      const otpHash = hashToken(otpCode);
      const otpExpiryMinutes = parseInt(process.env.OTP_EXPIRY_MINUTES || '10', 10);
      const expiresAt = new Date(Date.now() + otpExpiryMinutes * 60 * 1000);
      const otpId = `otp_${uuidv4().replace(/-/g, '').substring(0, 16)}`;

      await pool.query(
        'INSERT INTO otp_requests (id, email, otp_hash, is_used, expires_at) VALUES (?, ?, ?, 0, ?)',
        [otpId, cleanEmail, otpHash, expiresAt]
      );

      console.log(`[OTP Verification] Code generated for ${cleanEmail}: ${otpCode}`);

      return res.status(201).json({
        success: true,
        message: 'ثبت‌نام انجام شد. کد تایید به ایمیل شما ارسال گردید.',
        requireOtp: true,
        email: cleanEmail
      });
    }

    // Direct Login tokens if OTP is disabled
    const tokens = generateTokens(userId, cleanEmail);
    await saveRefreshToken(userId, tokens.refreshToken);

    res.status(201).json({
      success: true,
      message: 'ثبت‌نام با موفقیت انجام شد.',
      token: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: {
        id: userId,
        email: cleanEmail,
        isVerified: true,
        isPremium: false,
        subscriptionPlan: 'FREE',
        freeAnalysisCount: 0
      }
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 2. POST /api/auth/login
// ------------------------------------------
router.post('/login', authLimiter, async (req, res, next) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'لطفاً ایمیل و رمز عبور را وارد کنید.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();

    const [rows] = await pool.query(
      'SELECT id, email, password_hash, is_verified, is_premium, subscription_plan, free_analysis_count FROM users WHERE email = ?',
      [cleanEmail]
    );

    if (rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'ایمیل یا رمز عبور اشتباه است.'
      });
    }

    const user = rows[0];
    const isPasswordValid = await bcrypt.compare(password, user.password_hash);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'ایمیل یا رمز عبور اشتباه است.'
      });
    }

    // Check dynamic subscription expiry
    const subStatus = await checkAndUpdateSubscriptionStatus(user.id);

    const tokens = generateTokens(user.id, user.email);
    await saveRefreshToken(user.id, tokens.refreshToken);

    res.json({
      success: true,
      message: 'ورود با موفقیت انجام شد.',
      token: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: {
        id: user.id,
        email: user.email,
        isVerified: Boolean(user.is_verified),
        isPremium: subStatus.isPremium,
        subscriptionPlan: subStatus.plan,
        freeAnalysisCount: user.free_analysis_count
      }
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 3. POST /api/auth/refresh (Rotation & Revocation)
// ------------------------------------------
router.post('/refresh', async (req, res, next) => {
  try {
    const { refreshToken } = req.body;

    if (!refreshToken) {
      return res.status(400).json({
        success: false,
        message: 'توکن بازنشانی (Refresh Token) الزامی است.'
      });
    }

    const secret = process.env.JWT_SECRET || 'DEFAULT_NEXA_SECRET_KEY_CHANGE_IN_ENV';
    let decoded;

    try {
      decoded = jwt.verify(refreshToken, secret);
    } catch (err) {
      return res.status(401).json({
        success: false,
        message: 'توکن بازنشانی منقضی یا نامعتبر شده است.'
      });
    }

    if (decoded.type !== 'refresh') {
      return res.status(400).json({
        success: false,
        message: 'نوع توکن ارسال شده نامعتبر است.'
      });
    }

    const incomingHash = hashToken(refreshToken);

    // Look up hashed token in DB
    const [rtRows] = await pool.query(
      'SELECT id, user_id FROM refresh_tokens WHERE token_hash = ? AND is_revoked = 0 AND expires_at > NOW()',
      [incomingHash]
    );

    if (rtRows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'توکن بازنشانی باطل شده یا یافت نشد.'
      });
    }

    const matchedToken = rtRows[0];

    // Revoke old token
    await pool.query('UPDATE refresh_tokens SET is_revoked = 1 WHERE id = ?', [matchedToken.id]);

    // Issue new pair
    const tokens = generateTokens(matchedToken.user_id, decoded.email);
    await saveRefreshToken(matchedToken.user_id, tokens.refreshToken);

    res.json({
      success: true,
      token: tokens.accessToken,
      refreshToken: tokens.refreshToken
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 4. POST /api/auth/send-otp
// ------------------------------------------
router.post('/send-otp', authLimiter, async (req, res, next) => {
  try {
    const { email } = req.body;
    if (!email || !email.includes('@')) {
      return res.status(400).json({
        success: false,
        message: 'ایمیل معتبر نیست.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = hashToken(otpCode);
    const otpExpiryMinutes = parseInt(process.env.OTP_EXPIRY_MINUTES || '10', 10);
    const expiresAt = new Date(Date.now() + otpExpiryMinutes * 60 * 1000);
    const otpId = `otp_${uuidv4().replace(/-/g, '').substring(0, 16)}`;

    await pool.query(
      'INSERT INTO otp_requests (id, email, otp_hash, is_used, expires_at) VALUES (?, ?, ?, 0, ?)',
      [otpId, cleanEmail, otpHash, expiresAt]
    );

    console.log(`[OTP Service] Code generated for ${cleanEmail}: ${otpCode}`);

    res.json({
      success: true,
      message: 'کد تایید با موفقیت تولید و ارسال گردید.'
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 5. POST /api/auth/verify-otp
// ------------------------------------------
router.post('/verify-otp', authLimiter, async (req, res, next) => {
  try {
    const { email, otpCode } = req.body;

    if (!email || !otpCode) {
      return res.status(400).json({
        success: false,
        message: 'ایمیل و کد تایید الزامی هستند.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const incomingHash = hashToken(otpCode.trim());

    const [rows] = await pool.query(
      'SELECT id FROM otp_requests WHERE email = ? AND otp_hash = ? AND is_used = 0 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1',
      [cleanEmail, incomingHash]
    );

    if (rows.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'کد تایید وارد شده اشتباه یا منقضی شده است.'
      });
    }

    // Mark OTP used
    await pool.query('UPDATE otp_requests SET is_used = 1 WHERE id = ?', [rows[0].id]);

    // Mark user verified
    await pool.query('UPDATE users SET is_verified = 1 WHERE email = ?', [cleanEmail]);

    const [uRows] = await pool.query('SELECT id, email, is_premium, subscription_plan, free_analysis_count FROM users WHERE email = ?', [cleanEmail]);
    if (uRows.length === 0) {
      return res.status(404).json({ success: false, message: 'کاربر یافت نشد.' });
    }

    const user = uRows[0];
    const tokens = generateTokens(user.id, user.email);
    await saveRefreshToken(user.id, tokens.refreshToken);

    res.json({
      success: true,
      message: 'حساب کاربری با موفقیت تایید گردید.',
      token: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: {
        id: user.id,
        email: user.email,
        isVerified: true,
        isPremium: Boolean(user.is_premium),
        subscriptionPlan: user.subscription_plan,
        freeAnalysisCount: user.free_analysis_count
      }
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 6. GET /api/auth/me
// ------------------------------------------
router.get('/me', authenticateToken, async (req, res, next) => {
  try {
    const subStatus = await checkAndUpdateSubscriptionStatus(req.user.id);

    res.json({
      success: true,
      user: {
        id: req.user.id,
        email: req.user.email,
        isVerified: Boolean(req.user.is_verified),
        isPremium: subStatus.isPremium,
        subscriptionPlan: subStatus.plan,
        freeAnalysisCount: req.user.free_analysis_count,
        createdAt: req.user.created_at
      }
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
