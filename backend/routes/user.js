const express = require('express');
const { pool } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { checkAndUpdateSubscriptionStatus } = require('../services/subscriptionService');

const router = express.Router();

// ------------------------------------------
// 1. GET /api/user/me
// ------------------------------------------
router.get('/me', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id; // Strictly bound to token!
    const subStatus = await checkAndUpdateSubscriptionStatus(userId);

    const freeLimit = parseInt(process.env.FREE_ANALYSIS_LIMIT || '3', 10);
    const usedCount = req.user.free_analysis_count || 0;
    const remainingFree = subStatus.isPremium ? 'UNLIMITED' : Math.max(0, freeLimit - usedCount);

    res.json({
      success: true,
      user: {
        id: userId,
        email: req.user.email,
        isVerified: Boolean(req.user.is_verified),
        isPremium: subStatus.isPremium,
        subscriptionPlan: subStatus.plan,
        freeAnalysisCount: usedCount,
        freeAnalysisLimit: freeLimit,
        remainingFreeAnalyses: remainingFree,
        createdAt: req.user.created_at
      }
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 2. GET /api/user/analyses
// ------------------------------------------
router.get('/analyses', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id; // Strictly bound to authenticated token!

    const [rows] = await pool.query(
      `SELECT id, symbol, timeframe, signal, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level, created_at 
       FROM chart_analyses 
       WHERE user_id = ? 
       ORDER BY created_at DESC 
       LIMIT 100`,
      [userId]
    );

    const formattedAnalyses = rows.map(row => {
      let reasons = [];
      try {
        reasons = row.reasons_json ? JSON.parse(row.reasons_json) : [];
      } catch (e) {
        reasons = [row.reasons_json];
      }

      return {
        id: row.id,
        symbol: row.symbol,
        timeframe: row.timeframe,
        signal: row.signal,
        confidence: row.confidence,
        reasons: reasons,
        entryPrice: row.entry_price,
        stopLoss: row.stop_loss,
        takeProfit: row.take_profit,
        riskLevel: row.risk_level,
        createdAt: row.created_at
      };
    });

    res.json({
      success: true,
      count: formattedAnalyses.length,
      analyses: formattedAnalyses
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
