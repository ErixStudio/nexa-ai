const { v4: uuidv4 } = require('uuid');
const { pool } = require('../config/database');

/**
 * Creates a PENDING payment intent for a user.
 * @param {string} userId
 * @param {string} planName
 * @param {number} amountToman
 * @param {string} transactionRef
 * @returns {Promise<Object>} Payment record
 */
async function createPaymentIntent(userId, planName, amountToman, transactionRef) {
  const paymentId = `pay_${uuidv4().replace(/-/g, '').substring(0, 16)}`;

  await pool.query(
    `INSERT INTO payments (id, user_id, plan_name, amount_toman, payment_status, transaction_ref) 
     VALUES (?, ?, ?, ?, 'PENDING', ?)`,
    [paymentId, userId, planName, amountToman, transactionRef]
  );

  return {
    id: paymentId,
    userId,
    planName,
    amountToman,
    status: 'PENDING',
    transactionRef
  };
}

/**
 * Server-Side Secure Function to activate user subscription ONLY after verified payment gateway callback.
 * CANNOT be triggered directly by untrusted client request.
 * @param {string} paymentId
 * @param {string} verifiedRefId
 * @returns {Promise<Object>} Subscription details
 */
async function activateSubscriptionAfterVerifiedPayment(paymentId, verifiedRefId) {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    // 1. Fetch payment record
    const [payments] = await connection.query(
      'SELECT id, user_id, plan_name, amount_toman, payment_status FROM payments WHERE id = ? FOR UPDATE',
      [paymentId]
    );

    if (payments.length === 0) {
      throw new Error('رکورد پرداخت یافت نشد.');
    }

    const payment = payments[0];

    if (payment.payment_status === 'COMPLETED') {
      await connection.commit();
      return { message: 'پرداخت قبلاً تایید و فعال شده است.' };
    }

    // 2. Mark payment as COMPLETED
    await connection.query(
      'UPDATE payments SET payment_status = "COMPLETED", transaction_ref = ? WHERE id = ?',
      [verifiedRefId, paymentId]
    );

    // 3. Create active subscription for 30 days
    const subId = `sub_${uuidv4().replace(/-/g, '').substring(0, 16)}`;
    const startDate = new Date();
    const endDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days

    await connection.query(
      `INSERT INTO subscriptions (id, user_id, plan_name, status, start_date, end_date) 
       VALUES (?, ?, ?, 'ACTIVE', ?, ?)`,
      [subId, payment.user_id, payment.plan_name, startDate, endDate]
    );

    // 4. Update user premium status
    await connection.query(
      'UPDATE users SET is_premium = 1, subscription_plan = ? WHERE id = ?',
      [payment.plan_name, payment.user_id]
    );

    await connection.commit();

    return {
      success: true,
      subscriptionId: subId,
      planName: payment.plan_name,
      startDate,
      endDate
    };
  } catch (error) {
    await connection.rollback();
    console.error('[PaymentService] Error activating subscription:', error.message);
    throw error;
  } finally {
    connection.release();
  }
}

module.exports = {
  createPaymentIntent,
  activateSubscriptionAfterVerifiedPayment
};
