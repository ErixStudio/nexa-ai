const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { pool } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { analyzeLimiter } = require('../middleware/rateLimiter');
const { analyzeChartWithGemini } = require('../services/gemini');
const { checkAndUpdateSubscriptionStatus } = require('../services/subscriptionService');

const router = express.Router();

// ------------------------------------------
// Chart Analysis Controller Handler
// ------------------------------------------
const handleChartAnalysis = async (req, res, next) => {
  const connection = await pool.getConnection();

  try {
    const { symbol, timeframe, base64Image, mimeType } = req.body;
    const userId = req.user.id;

    // 1. Input Validation
    if (!symbol || typeof symbol !== 'string' || symbol.trim().length > 32) {
      return res.status(400).json({
        success: false,
        message: 'نماد معاملاتی (Symbol) معتبر نیست.'
      });
    }

    if (!timeframe || typeof timeframe !== 'string' || timeframe.trim().length > 16) {
      return res.status(400).json({
        success: false,
        message: 'تایم‌فریم (Timeframe) معتبر نیست.'
      });
    }

    if (!base64Image || typeof base64Image !== 'string' || base64Image.length < 50) {
      return res.status(400).json({
        success: false,
        message: 'تصویر چارت ارسال نشده یا فرمت آن ناخونا است.'
      });
    }

    // 2. Check dynamic subscription status
    const subStatus = await checkAndUpdateSubscriptionStatus(userId);
    const freeLimit = parseInt(process.env.FREE_ANALYSIS_LIMIT || '3', 10);
    const isPremium = subStatus.isPremium;

    let reservedFree = false;

    // 3. Atomic Quota Reservation for Free Users
    if (!isPremium) {
      await connection.beginTransaction();

      const [userRows] = await connection.query(
        'SELECT free_analysis_count FROM users WHERE id = ? FOR UPDATE',
        [userId]
      );

      const currentUsed = userRows.length > 0 ? userRows[0].free_analysis_count : 0;

      if (currentUsed >= freeLimit) {
        await connection.rollback();
        return res.status(403).json({
          success: false,
          message: 'سقف تحلیلهای رایگان شما به پایان رسیده است. برای ادامه، اشتراک ویژه تهیه کنید.',
          quotaExceeded: true,
          freeLimit: freeLimit,
          usedCount: currentUsed
        });
      }

      // Reserve quota atomically
      await connection.query(
        'UPDATE users SET free_analysis_count = free_analysis_count + 1 WHERE id = ?',
        [userId]
      );

      await connection.commit();
      reservedFree = true;
    }

    // 4. Call Gemini Vision Analysis API
    let result;
    try {
      result = await analyzeChartWithGemini(symbol, timeframe, base64Image, mimeType);
    } catch (geminiError) {
      // Restore reserved quota if Gemini API failed
      if (reservedFree) {
        await pool.query(
          'UPDATE users SET free_analysis_count = GREATEST(0, free_analysis_count - 1) WHERE id = ?',
          [userId]
        );
      }
      throw geminiError;
    }

    // 5. If AI determined image is NOT a trading chart
    if (!result.isChart) {
      // Restore reserved quota
      if (reservedFree) {
        await pool.query(
          'UPDATE users SET free_analysis_count = GREATEST(0, free_analysis_count - 1) WHERE id = ?',
          [userId]
        );
      }

      return res.status(400).json({
        success: false,
        analysis: {
          isChart: false,
          invalidReason: result.invalidReason || 'تصویر ارسال شده چارت معاملاتی معتبر نیست.'
        },
        message: result.invalidReason || 'تصویر ارسال شده یک چارت معاملاتی معتبر نیست.'
      });
    }

    // 6. Save Analysis Record into Database
    const analysisId = `ana_${uuidv4().replace(/-/g, '').substring(0, 16)}`;
    const reasonsJson = JSON.stringify(result.reasons);

    await pool.query(
      `INSERT INTO chart_analyses 
       (id, user_id, symbol, timeframe, signal, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        analysisId,
        userId,
        symbol.trim().toUpperCase(),
        timeframe.trim(),
        result.signal,
        result.confidence,
        reasonsJson,
        result.entry,
        result.stopLoss,
        result.takeProfit,
        result.riskLevel
      ]
    );

    // Fetch updated free analysis count
    const [uRows] = await pool.query('SELECT free_analysis_count FROM users WHERE id = ?', [userId]);
    const finalFreeCount = uRows.length > 0 ? uRows[0].free_analysis_count : 0;
    const remainingFree = isPremium ? 'UNLIMITED' : Math.max(0, freeLimit - finalFreeCount);

    res.status(200).json({
      success: true,
      analysis: {
        id: analysisId,
        symbol: symbol.trim().toUpperCase(),
        timeframe: timeframe.trim(),
        signal: result.signal,
        confidence: result.confidence,
        entry: result.entry,
        stopLoss: result.stopLoss,
        takeProfit: result.takeProfit,
        reasons: result.reasons,
        riskLevel: result.riskLevel
      },
      remainingFreeAnalyses: remainingFree
    });
  } catch (error) {
    next(error);
  } finally {
    connection.release();
  }
};

// Route mounts:
// POST /api/analyze -> router.post('/') when mounted at /api/analyze
// POST /api/analyze/analyze -> router.post('/analyze') when mounted at /api/analyze
router.post('/', authenticateToken, analyzeLimiter, handleChartAnalysis);
router.post('/analyze', authenticateToken, analyzeLimiter, handleChartAnalysis);

// ------------------------------------------
// GET /api/analysis/history
// ------------------------------------------
router.get('/history', authenticateToken, async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      `SELECT id, symbol, timeframe, signal, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level, created_at 
       FROM chart_analyses 
       WHERE user_id = ? 
       ORDER BY created_at DESC`,
      [req.user.id]
    );

    const formattedList = rows.map(r => ({
      id: r.id,
      symbol: r.symbol,
      timeframe: r.timeframe,
      signal: r.signal,
      confidence: r.confidence,
      entry: r.entry_price,
      stopLoss: r.stop_loss,
      takeProfit: r.take_profit,
      reasons: JSON.parse(r.reasons_json || '[]'),
      riskLevel: r.risk_level,
      createdAt: r.created_at
    }));

    res.json({
      success: true,
      analyses: formattedList
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
