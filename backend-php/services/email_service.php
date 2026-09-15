<?php
/**
 * NEXA AI Pure PHP SMTP Email Service (Zero External Dependencies)
 * Designed for cPanel / Shared Hosting compatibility over SSL (Port 465) or TLS (Port 587)
 */

require_once __DIR__ . '/../config/config.php';

class EmailService {

    /**
     * Send OTP Verification Email
     *
     * @param string $toEmail Recipient Email
     * @param string $otpCode 6-digit OTP Code
     * @throws Exception if sending fails
     */
    public static function sendOtpEmail(string $toEmail, string $otpCode): void {
        $mailEnabled = env('MAIL_ENABLED', true) === true;
        if (!$mailEnabled) {
            error_log("[EmailService] MAIL_ENABLED is false. Skipping email send for: {$toEmail}");
            return;
        }

        $host = env('MAIL_HOST', 'mail.erixstudio.shop');
        $port = (int)env('MAIL_PORT', 465);
        $username = env('MAIL_USERNAME', 'nexaai@erixstudio.shop');
        $password = env('MAIL_PASSWORD', '');
        $encryption = strtolower(env('MAIL_ENCRYPTION', 'ssl'));
        $fromEmail = env('MAIL_FROM', 'nexaai@erixstudio.shop');
        $fromName = env('MAIL_FROM_NAME', 'NEXA AI');

        if (empty($password)) {
            error_log("[EmailService Error] MAIL_PASSWORD is missing in .env");
            throw new Exception("تنظیمات ارسال ایمیل سرور پیکربندی نشده است.", 500);
        }

        $subject = "کد تایید حساب کاربری NEXA AI | Verification Code";
        $htmlBody = self::buildOtpEmailHtml($otpCode);

        self::sendSmtpMail(
            $host,
            $port,
            $username,
            $password,
            $encryption,
            $fromEmail,
            $fromName,
            $toEmail,
            $subject,
            $htmlBody
        );
    }

    /**
     * Internal SMTP Transport over SSL/TLS Socket Connection
     */
    private static function sendSmtpMail(
        string $host,
        int $port,
        string $username,
        string $password,
        string $encryption,
        string $fromEmail,
        string $fromName,
        string $toEmail,
        string $subject,
        string $bodyHtml
    ): void {
        $timeout = 15;
        
        if ($encryption === 'ssl' || $port === 465) {
            $remote = "ssl://{$host}:{$port}";
        } else {
            $remote = "tcp://{$host}:{$port}";
        }

        $context = stream_context_create([
            'ssl' => [
                'verify_peer' => true,
                'verify_peer_name' => true,
                'allow_self_signed' => false,
                'peer_name' => $host
            ]
        ]);

        $errno = 0;
        $errstr = '';
        $socket = @stream_socket_client($remote, $errno, $errstr, $timeout, STREAM_CLIENT_CONNECT, $context);

        if (!$socket) {
            error_log("[SMTP Socket Connect Error] {$errstr} ({$errno})");
            throw new Exception("خطا در برقراری ارتباط با سرور ایمیل (SMTP Connect Failed).", 500);
        }

        stream_set_timeout($socket, $timeout);

        try {
            // 1. Read Greeting (220)
            self::readResponse($socket, 220);

            // 2. EHLO Command
            $clientDomain = gethostname() ?: 'erixstudio.shop';
            self::sendCommand($socket, "EHLO {$clientDomain}", 250);

            // 3. STARTTLS if required on Port 587
            if (($encryption === 'tls' || $port === 587) && $encryption !== 'ssl') {
                self::sendCommand($socket, "STARTTLS", 220);
                $cryptoResult = @stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLSv1_2_CLIENT | STREAM_CRYPTO_METHOD_TLSv1_3_CLIENT);
                if (!$cryptoResult) {
                    throw new Exception("خطا در فعال‌سازی رمزنگاری TLS برای ارسال ایمیل.");
                }
                self::sendCommand($socket, "EHLO {$clientDomain}", 250);
            }

            // 4. AUTH LOGIN
            self::sendCommand($socket, "AUTH LOGIN", 334);
            self::sendCommand($socket, base64_encode($username), 334);
            self::sendCommand($socket, base64_encode($password), 235);

            // 5. MAIL FROM
            self::sendCommand($socket, "MAIL FROM: <{$fromEmail}>", 250);

            // 6. RCPT TO
            self::sendCommand($socket, "RCPT TO: <{$toEmail}>", 250);

            // 7. DATA
            self::sendCommand($socket, "DATA", 354);

            // 8. Headers & Body Content
            $encodedSubject = "=?UTF-8?B?" . base64_encode($subject) . "?=";
            $encodedFromName = "=?UTF-8?B?" . base64_encode($fromName) . "?=";
            $messageId = "<" . md5(uniqid(time())) . "@" . $host . ">";
            $date = date('r');

            $headers  = "Date: {$date}\r\n";
            $headers .= "From: {$encodedFromName} <{$fromEmail}>\r\n";
            $headers .= "To: <{$toEmail}>\r\n";
            $headers .= "Subject: {$encodedSubject}\r\n";
            $headers .= "Message-ID: {$messageId}\r\n";
            $headers .= "MIME-Version: 1.0\r\n";
            $headers .= "Content-Type: text/html; charset=UTF-8\r\n";
            $headers .= "Content-Transfer-Encoding: base64\r\n\r\n";

            $encodedBody = chunk_split(base64_encode($bodyHtml));
            
            $rawMessage = $headers . $encodedBody . "\r\n.";

            self::sendCommand($socket, $rawMessage, 250);

            // 9. QUIT
            self::sendCommand($socket, "QUIT", 221);

        } catch (Exception $e) {
            @fclose($socket);
            error_log("[SMTP Exception] Email delivery failed for recipient: " . $e->getMessage());
            throw new Exception("ارسال ایمیل تایید با خطا مواجه شد. (EMAIL_SEND_FAILED)", 500);
        }

        @fclose($socket);
    }

    private static function sendCommand($socket, string $cmd, int $expectedCode): string {
        fwrite($socket, $cmd . "\r\n");
        return self::readResponse($socket, $expectedCode);
    }

    private static function readResponse($socket, int $expectedCode): string {
        $response = "";
        while ($line = fgets($socket, 512)) {
            $response .= $line;
            // SMTP line format: "250-..." means multi-line, "250 ..." means final line
            if (isset($line[3]) && $line[3] === ' ') {
                break;
            }
        }

        $code = (int)substr($response, 0, 3);
        if ($code !== $expectedCode) {
            throw new Exception("SMTP Unexpected Code {$code} (Expected {$expectedCode}): " . trim($response));
        }

        return $response;
    }

    /**
     * Responsive & Modern HTML Email Template
     */
    private static function buildOtpEmailHtml(string $otpCode): string {
        return <<<HTML
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXA AI Verification Code</title>
</head>
<body style="margin: 0; padding: 0; background-color: #0b0f19; font-family: Tahoma, 'Segoe UI', Arial, sans-serif; color: #f8fafc;">
    <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0b0f19; padding: 40px 10px;">
        <tr>
            <td align="center">
                <table role="presentation" width="100%" max-width="500px" border="0" cellspacing="0" cellpadding="0" style="max-width: 500px; background-color: #161e2e; border-radius: 16px; border: 1px solid #2d3748; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                    <!-- Header -->
                    <tr>
                        <td align="center" style="padding-bottom: 24px;">
                            <h1 style="margin: 0; color: #00e5ff; font-size: 28px; font-weight: 800; letter-spacing: 2px;">NEXA AI</h1>
                            <p style="margin: 4px 0 0 0; color: #94a3b8; font-size: 14px;">دستیار هوشمند تحلیل چارت‌های معاملاتی</p>
                        </td>
                    </tr>
                    
                    <!-- Divider -->
                    <tr>
                        <td style="border-top: 1px solid #2d3748; padding-top: 24px;"></td>
                    </tr>

                    <!-- Title -->
                    <tr>
                        <td align="center" style="padding-bottom: 16px;">
                            <h2 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: 700;">تایید آدرس ایمیل / Email Verification</h2>
                        </td>
                    </tr>

                    <!-- Message -->
                    <tr>
                        <td align="center" style="padding-bottom: 24px; color: #cbd5e1; font-size: 14px; line-height: 1.6;">
                            کد تایید حساب کاربری شما در NEXA AI آماده است:<br>
                            <span style="font-size: 13px; color: #94a3b8; dir: ltr;">Your verification code is:</span>
                        </td>
                    </tr>

                    <!-- OTP Code Box -->
                    <tr>
                        <td align="center" style="padding-bottom: 24px;">
                            <div style="background-color: #090d16; border: 2px dashed #00e5ff; border-radius: 12px; padding: 20px; text-align: center;">
                                <span style="font-size: 36px; font-weight: 800; color: #00e5ff; letter-spacing: 8px; font-family: 'Courier New', monospace;">{$otpCode}</span>
                            </div>
                        </td>
                    </tr>

                    <!-- Expiry & Warning -->
                    <tr>
                        <td align="center" style="padding-bottom: 24px; color: #f59e0b; font-size: 13px;">
                            ⏱️ این کد تا <b>۱۰ دقیقه</b> آینده معتبر است.<br>
                            <span style="font-size: 12px; color: #94a3b8; dir: ltr;">This code expires in 10 minutes.</span>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td align="center" style="border-top: 1px solid #2d3748; padding-top: 20px; color: #64748b; font-size: 12px; line-height: 1.5;">
                            اگر شما این درخواست را ثبت نکرده‌اید، این ایمیل را نادیده بگیرید.<br>
                            © NEXA AI Trading Assistant - All rights reserved.
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
HTML;
    }
}
