const express = require('express');
const { pool } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { createPaymentIntent } = require('../services/paymentService');

const router = express.Router();

// ------------------------------------------
// 1. POST /api/payments (Create Payment Intent)
// ------------------------------------------
router.post('/', authenticateToken, async (req, res, next) => {
  try {
    const { planName, amountToman, transactionRef } = req.body;
    const userId = req.user.id;

    if (!planName || !amountToman || isNaN(amountToman)) {
      return res.status(400).json({
        success: false,
        message: 'نام پلن و مبلغ به تومان الزامی است.'
      });
    }

    const ref = transactionRef || `REF_${Date.now()}_${Math.floor(Math.random() * 1000)}`;

    const paymentRecord = await createPaymentIntent(userId, planName, Number(amountToman), ref);

    res.status(201).json({
      success: true,
      message: 'سفارش پرداخت با موفقیت ثبت شد. در انتظار هدایت به درگاه پرداخت.',
      payment: paymentRecord
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 2. GET /api/payments (List User Payments)
// ------------------------------------------
router.get('/', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id;

    const [rows] = await pool.query(
      `SELECT id, plan_name, amount_toman, payment_status, transaction_ref, created_at 
       FROM payments 
       WHERE user_id = ? 
       ORDER BY created_at DESC`,
      [userId]
    );

    res.json({
      success: true,
      payments: rows
    });
  } catch (error) {
    next(error);
  }
});

// ------------------------------------------
// 3. GET /api/payments/:id (Get Payment Details)
// ------------------------------------------
router.get('/:id', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;

    const [rows] = await pool.query(
      'SELECT id, plan_name, amount_toman, payment_status, transaction_ref, created_at FROM payments WHERE id = ? AND user_id = ?',
      [id, userId]
    );

    if (rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'سفارش پرداخت یافت نشد.'
      });
    }

    res.json({
      success: true,
      payment: rows[0]
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
