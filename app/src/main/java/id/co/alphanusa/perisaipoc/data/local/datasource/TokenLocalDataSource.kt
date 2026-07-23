package id.co.alphanusa.perisaipoc.data.local.datasource

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import id.co.alphanusa.perisaipoc.core.util.Constants
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/** Penyimpanan refresh token. Implementasinya disembunyikan dari domain & UI. */
interface TokenLocalDataSource {
    fun saveRefreshToken(token: String)
    fun getRefreshToken(): String?
    fun clearRefreshToken()
    fun hasRefreshToken(): Boolean
    fun clearAll()
}

/**
 * Menyimpan refresh token di [EncryptedSharedPreferences]. Bila berkas terenkripsi
 * rusak (mis. keystore berubah), data lama dihapus lalu dibuat ulang agar aplikasi
 * tidak gagal total — pengguna cukup login ulang.
 */
@Singleton
class EncryptedTokenLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenLocalDataSource {

    private companion object {
        const val TAG = "TokenLocalDataSource"
    }

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences = try {
        buildEncryptedPrefs()
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences gagal dibuka, menghapus data lama...", e)
        clearCorruptedData()
        buildEncryptedPrefs()
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            Constants.Prefs.AUTH_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun clearCorruptedData() {
        context.deleteSharedPreferences(Constants.Prefs.AUTH_NAME)
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghapus entry keystore", e)
        }
    }

    override fun saveRefreshToken(token: String) {
        prefs.edit().putString(Constants.Prefs.KEY_REFRESH_TOKEN, token).apply()
    }

    override fun getRefreshToken(): String? =
        prefs.getString(Constants.Prefs.KEY_REFRESH_TOKEN, null)

    override fun clearRefreshToken() {
        prefs.edit().remove(Constants.Prefs.KEY_REFRESH_TOKEN).apply()
    }

    override fun hasRefreshToken(): Boolean = getRefreshToken() != null

    override fun clearAll() {
        prefs.edit().clear().apply()
    }
}
