# NEXA AI Backend Server Deployment Guide (v2.0)

این راهنما مراحل کامل نصب و راه اندازی بک‌اند **Node.js + Express + MySQL** برنامه **NEXA AI** را روی سرور ابری (Ubuntu/Debian) یا هاستینگهای cPanel توضیح می‌دهد.

---

## ۱. پیش‌نیازهای سرور
1. **Node.js** نسخه `18.x` یا بالاتر
2. **MySQL Database** نسخه `8.0+` یا **MariaDB**
3. **PM2** (Process Manager جهت نگه داشتن سرور همیشه روشن)
4. **Nginx** (به عنوان Reverse Proxy برای SSL و HTTPS)

---

## ۲. مراحل نصب روی سرور لینوکس (Ubuntu / Debian)

### گام اول: آپدیت و نصب Node.js و MySQL
```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs mysql-server nginx git
```

### گام دوم: ساخت دیتابیس و یوزر در MySQL
وارد محیط ترمینال دیتابیس شوید:
```bash
sudo mysql
```
دستورات زیر را برای ایجاد دیتابیس `h410448_NEXAAI` و یوزر دیتابیس اجرا کنید:
```sql
CREATE DATABASE IF NOT EXISTS `h410448_NEXAAI` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'h410448_admin'@'localhost' IDENTIFIED BY 'xR)*h98IJ;rrph,R';
GRANT ALL PRIVILEGES ON `h410448_NEXAAI`.* TO 'h410448_admin'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### گام سوم: اجرای SQL Schema
اسکریپت دیتابیس را روی MySQL اجرا کنید:
```bash
mysql -u h410448_admin -p h410448_NEXAAI < /path/to/backend/sql/schema.sql
```

### گام چهارم: آپلود فایل‌های بک‌اند و نصب Dependencyها
فایل‌های پوشه `backend` را روی سرور کپی کنید (مثلاً در `/var/www/nexa-backend`):
```bash
cd /var/www/nexa-backend
npm install
```

### گام پنجم: تنظیم فایل `.env`
فایل `.env` را ایجاد کنید:
```bash
cp .env.example .env
nano .env
```
مقادیر زیر را تنظیم کنید:
```env
PORT=5000
NODE_ENV=production

DB_HOST=localhost
DB_PORT=3306
DB_NAME=h410448_NEXAAI
DB_USER=h410448_admin
DB_PASSWORD=YOUR_SECURE_PASSWORD

GEMINI_API_KEY=YOUR_GEMINI_API_KEY

JWT_SECRET=SUPER_SECURE_JWT_SECRET_KEY_NEXA_AI_2026_RANDOM_STRING
JWT_EXPIRES_IN=7d
REFRESH_TOKEN_EXPIRES_IN=30d

FREE_ANALYSIS_LIMIT=3
CORS_ORIGIN=*

OTP_ENABLED=false
OTP_EXPIRY_MINUTES=10
```

### گام ششم: اجرای برنامه با PM2
```bash
sudo npm install -g pm2
pm2 start server.js --name "nexa-backend"
pm2 save
pm2 startup
```

---

## ۳. تنظیم Nginx و HTTPS (Certbot)

فایل کانفیگ Nginx:
```bash
sudo nano /etc/nginx/sites-available/nexa-api
```
محتوای Nginx:
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    client_max_body_size 35M;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_cache_bypass $http_upgrade;
    }
}
```
فعال‌سازی و SSL:
```bash
sudo ln -s /etc/nginx/sites-available/nexa-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
sudo certbot --nginx -d api.yourdomain.com
```

---

## ۴. راهنمای نصب روی cPanel (Setup Node.js App)

1. **ایجاد دیتابیس:** در cPanel بخش `MySQL Databases` دیتابیس `h410448_NEXAAI` را بسازید.
2. **اجرای Schema:** در `phpMyAdmin` فایل `sql/schema.sql` را Import کنید.
3. **Setup Node.js App:** 
   - Node.js Version: `18.x` یا `20.x`
   - Application root: `backend`
   - Application startup file: `server.js`
4. **تنظیم Environment Variables:** متغیرهای `.env` را در محیط cPanel وارد کنید.
5. کلید **Run NPM Install** و سپس **Start App** را بزنید.
