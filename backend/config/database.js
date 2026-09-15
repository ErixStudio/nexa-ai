const mysql = require('mysql2/promise');
const dotenv = require('dotenv');

dotenv.config();

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '3306', 10),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'h410448_NEXAAI',
  waitForConnections: true,
  connectionLimit: 15,
  queueLimit: 0,
  charset: 'utf8mb4'
});

// Test initial connection on server start
async function testDbConnection() {
  try {
    const connection = await pool.getConnection();
    console.log(`[Database] Connected successfully to MySQL DB: ${process.env.DB_NAME || 'h410448_NEXAAI'}`);
    connection.release();
  } catch (error) {
    console.error('[Database Connection Error]:', error.message);
  }
}

module.exports = {
  pool,
  testDbConnection
};
