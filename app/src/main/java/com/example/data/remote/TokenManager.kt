package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedSharedPreferences(context.applicationContext) 
        ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences? {
        return try {
            val masterKeyClass = Class.forName("androidx.security.crypto.MasterKey\$Builder")
            val keySchemeClass = Class.forName("androidx.security.crypto.MasterKey\$KeyScheme")
            val aes256Gcm = keySchemeClass.getField("AES256_GCM").get(null)

            val builder = masterKeyClass.getConstructor(Context::class.java).newInstance(context)
            masterKeyClass.getMethod("setKeyScheme", keySchemeClass).invoke(builder, aes256Gcm)
            val masterKey = masterKeyClass.getMethod("build").invoke(builder)

            val espClass = Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
            val keySchemeEnum = Class.forName("androidx.security.crypto.EncryptedSharedPreferences\$PrefKeyEncryptionScheme")
            val valSchemeEnum = Class.forName("androidx.security.crypto.EncryptedSharedPreferences\$PrefValueEncryptionScheme")

            val keyEnc = keySchemeEnum.enumConstants?.firstOrNull { it.toString() == "AES256_SKEY_KEYGEN" }
                ?: keySchemeEnum.enumConstants?.firstOrNull()
            val valEnc = valSchemeEnum.enumConstants?.firstOrNull { it.toString() == "AES256_GCM" }
                ?: valSchemeEnum.enumConstants?.firstOrNull()

            val createMethod = espClass.getMethod(
                "create",
                Context::class.java,
                String::class.java,
                Class.forName("androidx.security.crypto.MasterKey"),
                keySchemeEnum,
                valSchemeEnum
            )
            createMethod.invoke(null, context, PREFS_NAME, masterKey, keyEnc, valEnc) as SharedPreferences
        } catch (e: Exception) {
            null
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "nexa_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_USER_EMAIL = "jwt_user_email"
        private const val KEY_USER_ID = "jwt_user_id"
    }
}
