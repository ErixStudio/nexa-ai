package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Purchase

data class MyketPurchaseResult(
    val isSuccess: Boolean,
    val sku: String,
    val purchaseToken: String,
    val orderId: String,
    val packageName: String,
    val errorMessage: String? = null
)

class MyketBillingManager(
    private val context: Context
) {

    private val tag = "MyketBillingManager"

    private val myketHelper: IabHelper

    init {
        val publicKey = BuildConfig.MYKET_PUBLIC_KEY

        if (publicKey.isBlank() ||
            publicKey == "YOUR_MYKET_PUBLIC_KEY" ||
            publicKey == "کلید_عمومی_مایکت_تو"
        ) {
            Log.e(tag, "MYKET_PUBLIC_KEY is missing or invalid.")
        }

        myketHelper = IabHelper(
            context.applicationContext,
            publicKey
        )

        // فقط برای تست نسخه‌ای که قرار است به مایکت بدهیم.
        // بعد از تأیید کامل می‌توانی false کنی.
        myketHelper.enableDebugLogging(true)
    }

    fun isMyketInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(MYKET_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSkuForPlan(planKey: String): String {
        return when (planKey.uppercase()) {
            "MONTHLY" -> "nexa_monthly"
            "QUARTERLY" -> "nexa_quarterly"
            "SEMI_ANNUAL" -> "nexa_semiannual"
            "ANNUAL" -> "nexa_annual"
            else -> "nexa_monthly"
        }
    }

    fun startPurchase(
        activity: Activity,
        planKey: String,
        onResult: (MyketPurchaseResult) -> Unit
    ) {

        val sku = getSkuForPlan(planKey)
        val packageName = context.packageName

        if (!isMyketInstalled()) {
            onResult(
                MyketPurchaseResult(
                    isSuccess = false,
                    sku = sku,
                    purchaseToken = "",
                    orderId = "",
                    packageName = packageName,
                    errorMessage =
                        "برنامه مایکت روی دستگاه شما نصب نیست. لطفاً ابتدا برنامه مایکت را نصب نمایید."
                )
            )
            return
        }

        val payload =
            "nexa_${planKey.uppercase()}_${System.currentTimeMillis()}"

        Log.d(tag, "Starting Myket purchase")
        Log.d(tag, "SKU: $sku")
        Log.d(tag, "Plan: $planKey")
        Log.d(tag, "Package: $packageName")

        try {

            /*
             * اینجا عمداً از ITEM_TYPE_SUBS استفاده می‌کنیم،
             * چون پلن‌های NEXA AI اشتراکی هستند.
             *
             * اگر در پنل مایکت این SKUها به‌عنوان Subscription ساخته نشده‌اند
             * و واقعاً In-App Product هستند، این قسمت باید به INAPP تغییر کند.
             */
            myketHelper.launchPurchaseFlow(
                activity,
                sku,
                IabHelper.ITEM_TYPE_SUBS,
                object : IabHelper.OnIabPurchaseFinishedListener {

                    override fun onIabPurchaseFinished(
                        result: IabResult?,
                        purchase: Purchase?
                    ) {

                        Log.d(
                            tag,
                            "Purchase finished. result=$result purchase=$purchase"
                        )

                        if (result == null) {
                            onResult(
                                MyketPurchaseResult(
                                    isSuccess = false,
                                    sku = sku,
                                    purchaseToken = "",
                                    orderId = "",
                                    packageName = packageName,
                                    errorMessage =
                                        "پاسخ نامعتبر از سرویس پرداخت مایکت دریافت شد."
                                )
                            )
                            return
                        }

                        if (result.isFailure) {

                            Log.e(
                                tag,
                                "Myket purchase failed: $result"
                            )

                            onResult(
                                MyketPurchaseResult(
                                    isSuccess = false,
                                    sku = sku,
                                    purchaseToken = "",
                                    orderId = "",
                                    packageName = packageName,
                                    errorMessage = mapMyketError(
                                        result.response,
                                        result.message
                                    )
                                )
                            )

                            return
                        }

                        if (purchase == null) {
                            onResult(
                                MyketPurchaseResult(
                                    isSuccess = false,
                                    sku = sku,
                                    purchaseToken = "",
                                    orderId = "",
                                    packageName = packageName,
                                    errorMessage =
                                        "خرید انجام شد اما اطلاعات خرید از مایکت دریافت نشد."
                                )
                            )
                            return
                        }

                        val purchaseSku = purchase.sku
                        val purchaseToken = purchase.token
                        val orderId = purchase.orderId
                        val purchasePackage =
                            purchase.packageName.ifBlank { packageName }

                        if (purchaseSku != sku) {
                            Log.e(
                                tag,
                                "SKU mismatch. expected=$sku actual=$purchaseSku"
                            )

                            onResult(
                                MyketPurchaseResult(
                                    isSuccess = false,
                                    sku = purchaseSku,
                                    purchaseToken = "",
                                    orderId = orderId,
                                    packageName = purchasePackage,
                                    errorMessage =
                                        "شناسه محصول خریداری‌شده با پلن انتخابی مطابقت ندارد."
                                )
                            )
                            return
                        }

                        if (purchaseToken.isBlank()) {
                            onResult(
                                MyketPurchaseResult(
                                    isSuccess = false,
                                    sku = purchaseSku,
                                    purchaseToken = "",
                                    orderId = orderId,
                                    packageName = purchasePackage,
                                    errorMessage =
                                        "توکن خرید از مایکت دریافت نشد."
                                )
                            )
                            return
                        }

                        /*
                         * Purchase signature توسط IabHelper رسمی مایکت
                         * در فرآیند Billing بررسی می‌شود.
                         *
                         * سپس همین purchaseToken به Backend ارسال می‌شود
                         * تا سمت developer.myket.ir نیز مجدداً verify شود.
                         */
                        onResult(
                            MyketPurchaseResult(
                                isSuccess = true,
                                sku = purchaseSku,
                                purchaseToken = purchaseToken,
                                orderId = orderId,
                                packageName = purchasePackage
                            )
                        )
                    }
                },
                payload
            )

        } catch (e: Exception) {

            Log.e(tag, "Myket purchase exception", e)

            onResult(
                MyketPurchaseResult(
                    isSuccess = false,
                    sku = sku,
                    purchaseToken = "",
                    orderId = "",
                    packageName = packageName,
                    errorMessage =
                        "خطا در راه‌اندازی سرویس پرداخت مایکت: ${
                            e.localizedMessage ?: "خطای ناشناخته"
                        }"
                )
            )
        }
    }

    private fun mapMyketError(
        responseCode: Int,
        message: String?
    ): String {

        return when (responseCode) {
            IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED ->
                "فرآیند خرید توسط کاربر لغو شد."

            IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE ->
                "سرویس پرداخت مایکت روی این دستگاه در دسترس نیست."

            IabHelper.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE ->
                "محصول انتخاب‌شده در مایکت در دسترس نیست."

            IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR ->
                "تنظیمات محصول یا اطلاعات برنامه در مایکت صحیح نیست."

            IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED ->
                "این اشتراک قبلاً خریداری شده است."

            IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED ->
                "این محصول در حساب مایکت شما خریداری نشده است."

            else ->
                "خطا در سرویس پرداخت مایکت (کد: $responseCode)${
                    if (!message.isNullOrBlank()) {
                        "\n$message"
                    } else {
                        ""
                    }
                }"
        }
    }

    fun release() {
        try {
            myketHelper.dispose()
        } catch (e: Exception) {
            Log.w(tag, "Error disposing Myket billing helper", e)
        }
    }

    companion object {
        const val MYKET_PACKAGE = "ir.mservices.market"
    }
}