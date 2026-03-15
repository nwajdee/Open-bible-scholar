package com.openbiblescholar.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openbiblescholar.data.model.AiConfig
import com.openbiblescholar.data.model.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyStore @Inject constructor(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "obs_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(provider: AiProvider, key: String) {
        prefs.edit().putString("apikey_${provider.name}", key).apply()
    }

    fun getApiKey(provider: AiProvider): String =
        prefs.getString("apikey_${provider.name}", "") ?: ""

    fun saveSelectedProvider(provider: AiProvider) {
        prefs.edit().putString("selected_provider", provider.name).apply()
    }

    fun getSelectedProvider(): AiProvider {
        val name = prefs.getString("selected_provider", AiProvider.GROQ.name) ?: AiProvider.GROQ.name
        return try { AiProvider.valueOf(name) } catch (e: Exception) { AiProvider.GROQ }
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
    }

    fun getSelectedModel(): String =
        prefs.getString("selected_model", "llama3-8b-8192") ?: "llama3-8b-8192"

    fun getAiConfig(): AiConfig {
        val provider = getSelectedProvider()
        return AiConfig(
            provider = provider,
            apiKey = getApiKey(provider),
            model = getSelectedModel()
        )
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
