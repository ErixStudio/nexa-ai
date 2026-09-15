const { pool } = require('../config/database');

/**
 * Checks and dynamically synchronizes user's premium subscription status based on database end_date.
 * Automatically marks expired subscriptions as 'EXPIRED' and updates user's is_premium state.
 * @param {string} userId
 * @returns {Promise<Object>} Subscription state object
 */
async function checkAndUpdateSubscriptionStatus(userId) {
  try {
    // 1. First, expire any subscriptions where end_date <= NOW()
    await pool.query(
      `UPDATE subscriptions 
       SET status = 'EXPIRED' 
       WHERE user_id = ? AND status = 'ACTIVE' AND end_date IS NOT NULL AND end_date <= NOW()`,
      [userId]
    );

    // 2. Fetch the latest active subscription for the user
    const [activeSubs] = await pool.query(
      `SELECT id, plan_name, status, start_date, end_date 
       FROM subscriptions 
       WHERE user_id = ? AND status = 'ACTIVE' AND (end_date IS NULL OR end_date > NOW()) 
       ORDER BY end_date DESC LIMIT 1`,
      [userId]
    );

    if (activeSubs.length > 0) {
      const activeSub = activeSubs[0];

      // Ensure user record reflects active premium status
      await pool.query(
        'UPDATE users SET is_premium = 1, subscription_plan = ? WHERE id = ?',
        [activeSub.plan_name, userId]
      );

      return {
        isPremium: true,
        plan: activeSub.plan_name,
        status: 'ACTIVE',
        startDate: activeSub.start_date,
        endDate: activeSub.end_date
      };
    } else {
      // No active subscription -> demote user to FREE
      await pool.query(
        'UPDATE users SET is_premium = 0, subscription_plan = "FREE" WHERE id = ?',
        [userId]
      );

      return {
        isPremium: false,
        plan: 'FREE',
        status: 'INACTIVE',
        startDate: null,
        endDate: null
      };
    }
  } catch (error) {
    console.error(`[SubscriptionService] Error updating subscription status for user ${userId}:`, error.message);
    throw error;
  }
}

module.exports = {
  checkAndUpdateSubscriptionStatus
};
