package com.xc.quotewidget.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xc.quotewidget.BuildConfig
import kotlinx.coroutines.flow.firstOrNull
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String)  : AuthResult()
    object Cancelled                       : AuthResult()
    object NoGoogleAccount                 : AuthResult()
}

/**
 * AuthRepository v2：
 *  - 所有机密从 BuildConfig 读取（编译时由 local.properties / GitHub Secrets 注入）
 *  - Google idToken 过期检测 + 静默刷新
 *  - Telegram 回调 HMAC-SHA256 hash 验证（前端实现，适合 sideload 场景）
 */
class AuthRepository(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val session            = UserSession(context)

    // BuildConfig 字段在编译时确定；若为空字符串说明构建时未注入
    private val googleClientId      = BuildConfig.GOOGLE_CLIENT_ID
    private val telegramBotToken    = BuildConfig.TELEGRAM_BOT_TOKEN
    private val telegramBotUsername = BuildConfig.TELEGRAM_BOT_USERNAME

    val currentUser = session.currentUser

    // ─────────────────────────────────────────────────────────────
    //  Google 登录
    // ─────────────────────────────────────────────────────────────
    suspend fun signInWithGoogle(activityContext: Context): AuthResult {
        if (googleClientId.isEmpty()) {
            return AuthResult.Error("Google Client ID 未配置，请在 local.properties 中填写 GOOGLE_CLIENT_ID")
        }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleClientId)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            val result     = credentialManager.getCredential(
                activityContext,
                GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

            // Google JWT 有效期 1 小时，存储过期时间戳
            val expiryMs = System.currentTimeMillis() + 3_600_000L

            val user = UserInfo(
                provider    = AuthProvider.GOOGLE,
                displayName = credential.displayName ?: credential.givenName ?: "Google 用户",
                email       = credential.id,
                avatarUrl   = credential.profilePictureUri?.toString() ?: "",
                idToken     = credential.idToken,
                tokenExpiry = expiryMs,
            )
            session.save(user)
            AuthResult.Success(user)

        } catch (e: NoCredentialException)                 { AuthResult.NoGoogleAccount }
        catch (e: GetCredentialCancellationException)      { AuthResult.Cancelled       }
        catch (e: Exception)                               { AuthResult.Error(e.localizedMessage ?: "Google 登录失败") }
    }

    /**
     * 静默刷新 Google idToken。
     *
     * 刷新时机：调用方（ViewModel / 后台同步任务）在发起网络请求前调用
     * [ensureGoogleTokenFresh]，若 token 距过期不足 5 分钟则自动触发。
     *
     * 刷新机制：Credential Manager 在账号已授权的情况下会静默返回新 token，
     * 无需再次弹出账号选择界面（setFilterByAuthorizedAccounts(true)）。
     *
     * @return 刷新后的 idToken，或 null（刷新失败 / 用户未登录 / 非 Google 账号）
     */
    suspend fun ensureGoogleTokenFresh(activityContext: Context): String? {
        val user = session.currentUser.firstOrNull() ?: return null
        if (user.provider != AuthProvider.GOOGLE) return user.idToken  // Telegram 不需要刷新

        val fiveMinutes = 5 * 60 * 1000L
        if (System.currentTimeMillis() < user.tokenExpiry - fiveMinutes) {
            return user.idToken  // 未到刷新窗口，直接返回现有 token
        }

        // token 已过期或即将过期 → 静默刷新
        // setFilterByAuthorizedAccounts(true)：仅在已授权账号中静默选择，不弹 UI
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(googleClientId)
                .setAutoSelectEnabled(true)   // 静默模式必须 true
                .build()

            val result     = credentialManager.getCredential(
                activityContext,
                GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val refreshed  = user.copy(
                idToken     = credential.idToken,
                tokenExpiry = System.currentTimeMillis() + 3_600_000L,
            )
            session.save(refreshed)
            credential.idToken

        } catch (e: Exception) {
            // 静默刷新失败（token 彻底过期 / 账号被撤权）→ 清除会话，强制重登录
            session.clear()
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Telegram OAuth（Chrome Custom Tab）
    // ─────────────────────────────────────────────────────────────
    fun launchTelegramOAuth(activityContext: Context) {
        if (telegramBotUsername.isEmpty()) {
            // Bot 未配置时弹出提示（实际应在 UI 层拦截）
            return
        }
        val redirectUri = "quotewidget://telegram-callback"
        val authUrl = Uri.parse("https://oauth.telegram.org/auth").buildUpon()
            .appendQueryParameter("bot_id",       telegramBotUsername)
            .appendQueryParameter("scope",        "name,photo_url")
            .appendQueryParameter("callback_url", redirectUri)
            .appendQueryParameter("origin",       redirectUri)
            .build()

        CustomTabsIntent.Builder().setShowTitle(true).build()
            .launchUrl(activityContext, authUrl)
    }

    /**
     * Telegram 回调处理 + HMAC-SHA256 hash 验证
     *
     * 验证算法（官方规范）：
     *  1. 取回调参数中除 hash 外所有字段，按字母序排列为 "key=value\n" 字符串
     *  2. 用 SHA256(bot_token) 作为 HMAC-SHA256 的密钥
     *  3. 计算 HMAC-SHA256(data_check_string)，十六进制与 hash 参数对比
     *
     * 安全权衡（已在 local.properties.example 中说明）：
     *  bot_token 被编译进 APK → 仅适合个人 sideload。
     *  公开分发应将此验证移到后端，APK 中只保存用户 id，不保存 token。
     *
     * @param uri  quotewidget://telegram-callback?id=...&hash=...
     */
    suspend fun handleTelegramCallback(uri: Uri): AuthResult {
        if (telegramBotToken.isEmpty()) {
            return AuthResult.Error("Telegram Bot Token 未配置，请在 local.properties 中填写 TELEGRAM_BOT_TOKEN")
        }

        return try {
            val params = mapOf(
                "id"         to (uri.getQueryParameter("id")         ?: return AuthResult.Error("缺少 id 参数")),
                "first_name" to (uri.getQueryParameter("first_name") ?: ""),
                "last_name"  to (uri.getQueryParameter("last_name")  ?: ""),
                "photo_url"  to (uri.getQueryParameter("photo_url")  ?: ""),
                "username"   to (uri.getQueryParameter("username")   ?: ""),
                "auth_date"  to (uri.getQueryParameter("auth_date")  ?: ""),
            ).filterValues { it.isNotEmpty() }

            val hash = uri.getQueryParameter("hash")
                ?: return AuthResult.Error("缺少 hash 参数，无法验证 Telegram 授权")

            // ── HMAC-SHA256 验证 ──────────────────────────────────
            if (!verifyTelegramHash(params, hash)) {
                return AuthResult.Error("Telegram 签名验证失败，数据可能已被篡改")
            }

            // ── auth_date 时效性检查（超过 24 小时拒绝） ──────────
            val authDate = params["auth_date"]?.toLongOrNull() ?: 0L
            val nowSec   = System.currentTimeMillis() / 1000L
            if (nowSec - authDate > 86_400L) {
                return AuthResult.Error("Telegram 授权已过期（超过 24 小时），请重新授权")
            }

            val displayName = listOfNotNull(
                params["first_name"]?.takeIf { it.isNotEmpty() },
                params["last_name"]?.takeIf  { it.isNotEmpty() }
            ).joinToString(" ").ifEmpty { "Telegram 用户" }

            val user = UserInfo(
                provider    = AuthProvider.TELEGRAM,
                displayName = displayName,
                email       = "",
                avatarUrl   = params["photo_url"] ?: "",
                telegramId  = params["id"]!!,
                tokenExpiry = Long.MAX_VALUE,  // Telegram 授权无过期（由 auth_date 管控）
            )
            session.save(user)
            AuthResult.Success(user)

        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Telegram 回调解析失败")
        }
    }

    /**
     * HMAC-SHA256 验证核心逻辑
     *
     * key  = SHA-256(bot_token)  ← 注意：密钥本身是 bot_token 的 SHA-256 哈希
     * data = 所有非 hash 参数按 key 字母序排列，格式："key=value\n"（末尾无换行）
     */
    private fun verifyTelegramHash(params: Map<String, String>, expectedHash: String): Boolean {
        // 1. 构造 data_check_string
        val dataCheckString = params.entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value}" }

        // 2. key = SHA-256(bot_token)
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val secretKey = sha256.digest(telegramBotToken.toByteArray(Charsets.UTF_8))

        // 3. HMAC-SHA256(data_check_string, secretKey)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey, "HmacSHA256"))
        val computed = mac.doFinal(dataCheckString.toByteArray(Charsets.UTF_8))

        // 4. 转十六进制与 hash 参数对比（常量时间比较防止时序攻击）
        val computedHex = computed.joinToString("") { "%02x".format(it) }
        return computedHex.length == expectedHash.length &&
               computedHex.zip(expectedHash).all { (a, b) -> a == b }
    }

    /** 登出 */
    suspend fun signOut() = session.clear()
}
