package com.xc.quotewidget.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 登录界面的 UI 状态机 */
sealed class AuthUiState {
    object Idle        : AuthUiState()
    object Loading     : AuthUiState()
    data class LoggedIn(val user: UserInfo) : AuthUiState()
    data class Failure(val message: String) : AuthUiState()
    object NoGoogleAccount : AuthUiState()
}

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // 启动时恢复已有会话
        viewModelScope.launch {
            repo.currentUser.collect { user ->
                if (user != null && _uiState.value is AuthUiState.Idle) {
                    _uiState.value = AuthUiState.LoggedIn(user)
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val r = repo.signInWithGoogle(activityContext)) {
                is AuthResult.Success         -> AuthUiState.LoggedIn(r.user)
                is AuthResult.Error           -> AuthUiState.Failure(r.message)
                is AuthResult.Cancelled       -> AuthUiState.Idle
                is AuthResult.NoGoogleAccount -> AuthUiState.NoGoogleAccount
            }
        }
    }

    /** 仅启动 CCT，结果由 TelegramCallbackActivity 通过 handleTelegramCallback 传入 */
    fun launchTelegramOAuth(activityContext: Context) {
        repo.launchTelegramOAuth(activityContext)
    }

    /** 由 TelegramCallbackActivity 回调触发 */
    fun handleTelegramCallback(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val r = repo.handleTelegramCallback(uri)) {
                is AuthResult.Success   -> AuthUiState.LoggedIn(r.user)
                is AuthResult.Error     -> AuthUiState.Failure(r.message)
                else                    -> AuthUiState.Idle
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Failure ||
            _uiState.value is AuthUiState.NoGoogleAccount
        ) _uiState.value = AuthUiState.Idle
    }


    /**
     * 在发起需要认证的网络请求前调用。
     * 若 Google token 即将过期，静默刷新并返回最新 token。
     * 返回 null = 会话失效，UI 层应跳转到登录页。
     */
    suspend fun ensureTokenFresh(activityContext: android.content.Context): String? =
        repo.ensureGoogleTokenFresh(activityContext)

    // ── Factory：将 AuthRepository 注入 ViewModel ──────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(AuthRepository(context.applicationContext)) as T
    }
}