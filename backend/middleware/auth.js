const jwt = require('jsonwebtoken');
const { pool } = require('../config/database');

async function authenticateToken(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'توکن امنیتی (Bearer Token) یافت نشد. لطفاً ابتدا وارد حساب کاربری خود شوید.'
      });
    }

    const token = authHeader.split(' ')[1];
    const secret = process.env.JWT_SECRET || 'DEFAULT_NEXA_SECRET_KEY_CHANGE_IN_ENV';

    let decoded;
    try {
      decoded = jwt.verify(token, secret);
    } catch (jwtErr) {
      return res.status(401).json({
        success: false,
        message: 'توکن امنیتی شما منقضی شده یا نامعتبر است.'
      });
    }

    // Fetch user from DB using parameterized query
    const [rows] = await pool.query(
      'SELECT id, email, is_verified, is_premium, subscription_plan, free_analysis_count, created_at FROM users WHERE id = ?',
      [decoded.userId]
    );

    if (rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'کاربر صاحب این توکن یافت نشد.'
      });
    }

    req.user = rows[0];
    next();
  } catch (error) {
    next(error);
  }
}

module.exports = {
  authenticateToken
};
