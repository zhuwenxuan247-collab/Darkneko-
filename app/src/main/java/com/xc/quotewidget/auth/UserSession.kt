package com.xc.quotewidget.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_session")

enum class AuthProvider { NONE, GOOGLE, TELEGRAM }

data class UserInfo(
    val provider    : AuthProvider,
    val displayName : String,
    val email       : String,
    val avatarUrl   : String,
    val idToken     : String = "",
    val telegramId  : String = "",
    /**
     * Google idToken 过期时间（epoch 毫秒）。
     * Google JWT 默认有效期 1 小时（3600 秒），
     * 此字段存储 saveTime + 3600_000 ms。
     * Telegram 登录无 token 过期，此字段设为 Long.MAX_VALUE。
     */
    val tokenExpiry : Long   = Long.MAX_VALUE,
)

private object Keys {
    val PROVIDER     = stringPreferencesKey("provider")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val EMAIL        = stringPreferencesKey("email")
    val AVATAR_URL   = stringPreferencesKey("avatar_url")
    val ID_TOKEN     = stringPreferencesKey("id_token")
    val TELEGRAM_ID  = stringPreferencesKey("telegram_id")
    val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
}

class UserSession(private val context: Context) {

    val currentUser: Flow<UserInfo?> = context.dataStore.data.map { prefs ->
        val provider = prefs[Keys.PROVIDER]
            ?.let { runCatching { AuthProvider.valueOf(it) }.getOrNull() }
            ?: AuthProvider.NONE
        if (provider == AuthProvider.NONE) null
        else UserInfo(
            provider    = provider,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            email       = prefs[Keys.EMAIL]        ?: "",
            avatarUrl   = prefs[Keys.AVATAR_URL]   ?: "",
            idToken     = prefs[Keys.ID_TOKEN]     ?: "",
            telegramId  = prefs[Keys.TELEGRAM_ID]  ?: "",
            tokenExpiry = prefs[Keys.TOKEN_EXPIRY] ?: Long.MAX_VALUE,
        )
    }

    suspend fun save(user: UserInfo) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROVIDER]     = user.provider.name
            prefs[Keys.DISPLAY_NAME] = user.displayName
            prefs[Keys.EMAIL]        = user.email
            prefs[Keys.AVATAR_URL]   = user.avatarUrl
            prefs[Keys.ID_TOKEN]     = user.idToken
            prefs[Keys.TELEGRAM_ID]  = user.telegramId
            prefs[Keys.TOKEN_EXPIRY] = user.tokenExpiry
        }
    }

    suspend fun clear() = context.dataStore.edit { it.clear() }
}
