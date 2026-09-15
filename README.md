# NEXA AI

> AI-powered technical analysis assistant for cryptocurrency charts.

NEXA AI is an Android application that uses artificial intelligence to analyze cryptocurrency trading charts and provide structured market insights.

Upload a chart screenshot and NEXA AI analyzes the visible market structure, indicators, and price action to generate an AI-assisted trading analysis.

## ✨ Features

- 📊 AI-powered chart analysis
- 🟢 Buy / Sell / Hold signal
- 🎯 Suggested entry price
- 🛑 Stop-loss level
- 📈 Technical analysis and market structure insights
- 🧠 AI-generated reasoning behind each analysis
- 📱 Native Android application
- 🔐 Secure backend architecture for API communication
- 🌐 Persian and English support

## 🏗️ Architecture

NEXA AI uses a client-server architecture:

**Android App**
- Kotlin
- Jetpack Compose
- Material Design
- OkHttp

**Backend**
- PHP / REST API
- MySQL
- PDO

**AI**
- Google Gemini API

The Android application communicates with the backend rather than exposing sensitive server-side credentials directly inside the application.

## 🚀 Getting Started

### Requirements

- Android Studio
- JDK 21
- Android SDK
- A configured NEXA AI backend

### Android

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the Gradle project.
4. Configure the required environment variables.
5. Build and run the application on an emulator or Android device.

### Environment Variables

Create a local `.env` file for development and configure the required API credentials.

Example:

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY

Never commit real API keys, passwords, tokens, or other credentials to Git.

📁 Project Structure
NEXA AI
├── app/                 # Android application
├── backend/             # Backend services
├── backend-php/         # PHP backend
├── gradle/              # Gradle configuration
└── README.md
🔒 Security

Sensitive credentials are intentionally excluded from the repository.

For local development, configure your own environment variables and never store production secrets directly in source code.

⚠️ Disclaimer

NEXA AI provides AI-assisted technical analysis for educational and informational purposes.

It does not provide financial advice, and generated signals should not be considered a guarantee of market performance.

Always perform your own research and risk assessment before making financial decisions.

📌 Project Status

NEXA AI is currently under active development.

Features, architecture, and AI analysis capabilities may change as the project evolves.

👨‍💻 Developer

Developed by ErixStudio.

Building AI-powered applications for mobile and web platforms.

⭐ If you find NEXA AI interesting, consider giving the repository a star.