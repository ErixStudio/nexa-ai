package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LiveTicker(
    val rawSymbol: String,
    val displaySymbol: String,
    val price: Double,
    val changePct: Double,
    val isUp: Boolean
) {
    val formattedPrice: String
        get() = when {
            price >= 1000 -> String.format("$%,.2f", price)
            price >= 1 -> String.format("$%.2f", price)
            else -> String.format("$%.4f", price)
        }

    val formattedChange: String
        get() = String.format("%+.2f%%", changePct)
}

object MarketPriceService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Symbols mapping: Binance Symbol -> Display Symbol
    private val targetSymbols = mapOf(
        "BTCUSDT" to "BTC/USDT",
        "ETHUSDT" to "ETH/USDT",
        "SOLUSDT" to "SOL/USDT",
        "BNBUSDT" to "BNB/USDT",
        "XRPUSDT" to "XRP/USDT",
        "ADAUSDT" to "ADA/USDT",
        "DOGEUSDT" to "DOGE/USDT"
    )

    suspend fun fetchLiveTickers(): List<LiveTicker> = withContext(Dispatchers.IO) {
        // 1. Try Binance Public API
        try {
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbols=[\"BTCUSDT\",\"ETHUSDT\",\"SOLUSDT\",\"BNBUSDT\",\"XRPUSDT\",\"ADAUSDT\",\"DOGEUSDT\"]"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseBody)
                val list = mutableListOf<LiveTicker>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawSymbol = obj.getString("symbol")
                    val lastPrice = obj.optDouble("lastPrice", 0.0)
                    val changePct = obj.optDouble("priceChangePercent", 0.0)

                    val displaySymbol = targetSymbols[rawSymbol] ?: rawSymbol

                    list.add(
                        LiveTicker(
                            rawSymbol = rawSymbol,
                            displaySymbol = displaySymbol,
                            price = lastPrice,
                            changePct = changePct,
                            isUp = changePct >= 0
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to CoinGecko Public API if Binance is restricted
        try {
            val cgUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,binancecoin,ripple,cardano,dogecoin&vs_currencies=usd&include_24hr_change=true"
            val cgRequest = Request.Builder()
                .url(cgUrl)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            val cgResponse = okHttpClient.newCall(cgRequest).execute()
            if (cgResponse.isSuccessful) {
                val cgBody = cgResponse.body?.string() ?: ""
                val cgJson = JSONObject(cgBody)
                val list = mutableListOf<LiveTicker>()

                val mapping = mapOf(
                    "bitcoin" to ("BTCUSDT" to "BTC/USDT"),
                    "ethereum" to ("ETHUSDT" to "ETH/USDT"),
                    "solana" to ("SOLUSDT" to "SOL/USDT"),
                    "binancecoin" to ("BNBUSDT" to "BNB/USDT"),
                    "ripple" to ("XRPUSDT" to "XRP/USDT"),
                    "cardano" to ("ADAUSDT" to "ADA/USDT"),
                    "dogecoin" to ("DOGEUSDT" to "DOGE/USDT")
                )

                for ((id, pair) in mapping) {
                    if (cgJson.has(id)) {
                        val coinObj = cgJson.getJSONObject(id)
                        val price = coinObj.optDouble("usd", 0.0)
                        val changePct = coinObj.optDouble("usd_24h_change", 0.0)
                        list.add(
                            LiveTicker(
                                rawSymbol = pair.first,
                                displaySymbol = pair.second,
                                price = price,
                                changePct = changePct,
                                isUp = changePct >= 0
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) {
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback static tickers if both network calls fail
        listOf(
            LiveTicker("BTCUSDT", "BTC/USDT", 68420.50, 3.42, true),
            LiveTicker("ETHUSDT", "ETH/USDT", 3485.20, 2.15, true),
            LiveTicker("SOLUSDT", "SOL/USDT", 184.10, 5.80, true),
            LiveTicker("BNBUSDT", "BNB/USDT", 582.40, -0.75, false),
            LiveTicker("XRPUSDT", "XRP/USDT", 0.6240, 1.20, true)
        )
    }
}
