function errorHandler(err, req, res, next) {
  console.error('[Error Handler Log]:', {
    message: err.message,
    statusCode: err.statusCode,
    stack: process.env.NODE_ENV === 'development' ? err.stack : undefined
  });

  const statusCode = err.statusCode || 500;
  const isProd = process.env.NODE_ENV === 'production';

  // Standard safe error response (no credentials or SQL leaks in production)
  let userMessage = err.message || 'خطای داخلی سرور رخ داده است.';

  if (isProd && statusCode === 500) {
    userMessage = 'خطایی در پردازش درخواست شما رخ داد. لطفاً دوباره تلاش کنید.';
  }

  res.status(statusCode).json({
    success: false,
    message: userMessage,
    ...(isProd ? {} : { debugError: err.message })
  });
}

module.exports = errorHandler;
