const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { checkAndUpdateSubscriptionStatus } = require('../services/subscriptionService');

const router = express.Router();

// ------------------------------------------
// GET /api/subscription
// ------------------------------------------
router.get('/', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id;
    const subStatus = await checkAndUpdateSubscriptionStatus(userId);

    const freeLimit = parseInt(process.env.FREE_ANALYSIS_LIMIT || '3', 10);
    const usedCount = req.user.free_analysis_count || 0;

    res.json({
      success: true,
      isPremium: subStatus.isPremium,
      plan: subStatus.plan,
      status: subStatus.status,
      subscriptionDetails: subStatus.isPremium ? {
        startDate: subStatus.startDate,
        endDate: subStatus.endDate
      } : null,
      freeUsage: {
        limit: freeLimit,
        used: usedCount,
        remaining: subStatus.isPremium ? 'UNLIMITED' : Math.max(0, freeLimit - usedCount)
      }
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
