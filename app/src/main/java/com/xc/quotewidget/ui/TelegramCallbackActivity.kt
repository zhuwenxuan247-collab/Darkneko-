package com.xc.quotewidget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xc.quotewidget.auth.AuthUiState
import com.xc.quotewidget.auth.AuthViewModel
import kotlinx.coroutines.delay

/**
 * Telegram OAuth 深链接回调处理器
 *
 * 完整回调链路：
 *  1. AuthRepository.launchTelegramOAuth() 用 CCT 打开
 *     https://oauth.telegram.org/auth?callback_url=quotewidget://telegram-callback
 *  2. 用户在 Telegram 授权后，系统重定向到
 *     quotewidget://telegram-callback?id=xxx&first_name=xxx&photo_url=xxx&hash=xxx
 *  3. Android 的 intent-filter 匹配 scheme="quotewidget"，
 *     host="telegram-callback" → 启动本 Activity
 *  4. 本 Activity 从 intent.data 提取 URI，交给 AuthViewModel 处理
 *  5. 登录成功后用 FLAG_ACTIVITY_CLEAR_TOP 回到 LoginActivity，
 *     LoginActivity 的 onLoginSuccess 负责最终跳转
 *
 * 安全说明：
 *  Telegram 在回调 URL 中携带 hash 参数（HMAC-SHA256 签名）。
 *  生产环境应在后端用 bot token 验证 hash，防止参数伪造。
 *  当前实现仅做前端解析，适合个人/内测场景。
 *  验证算法参考：https://core.telegram.org/widgets/login#checking-authorization
 */
class TelegramCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 提取深链接 URI（intent.data 由 Android 路由注入）
        val callbackUri: Uri? = intent?.data

        setContent {
            QuoteWidgetTheme {
                TelegramCallbackScreen(
                    callbackUri   = callbackUri,
                    onSuccess     = { navigateBackToLogin() },
                    onError       = { navigateBackToLogin() },
                )
            }
        }
    }

    /**
     * 回到 LoginActivity。
     * FLAG_ACTIVITY_CLEAR_TOP：若 LoginActivity 已在栈中，清除其上的所有 Activity，
     * 并调用 onNewIntent 而非重建，避免重复走 onCreate 登录流程。
     */
    private fun navigateBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}

// ══════════════════════════════════════════════════════════════════
//  TelegramCallbackScreen
//  MD3 Expressive 设计：
//  ① Telegram 蓝色大圆 icon（Expressive hero 元素）
//  ② spring 弹跳缩放动画（Expressive MotionScheme）
//  ③ 成功态用 secondaryContainer 卡片 + 对勾弹入
//  ④ 错误态用 errorContainer 卡片 + 说明文字
// ══════════════════════════════════════════════════════════════════
@Composable
private fun TelegramCallbackScreen(
    callbackUri: Uri?,
    onSuccess  : () -> Unit,
    onError    : () -> Unit,
    viewModel  : AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(androidx.compose.ui.platform.LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // 首次进入时触发回调解析
    LaunchedEffect(callbackUri) {
        if (callbackUri != null) {
            viewModel.handleTelegramCallback(callbackUri)
        } else {
            // URI 为 null：Telegram 取消授权或系统路由异常
            delay(1200)
            onError()
        }
    }

    // 监听状态变化，成功/失败后短暂停留再跳转（让用户看到反馈）
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.LoggedIn -> {
                delay(900)   // 停留让 spring 动画完成
                onSuccess()
            }
            is AuthUiState.Failure -> {
                delay(1800)  // 停留让用户读到错误信息
                onError()
            }
            else -> Unit
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading, AuthUiState.Idle -> ProcessingCard()
            is AuthUiState.LoggedIn -> SuccessCard(name = state.user.displayName)
            is AuthUiState.Failure  -> ErrorCard(message = state.message)
            else                    -> ProcessingCard()
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  ProcessingCard — 解析中
// ──────────────────────────────────────────────────────────────────
@Composable
private fun ProcessingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Telegram 蓝色大圆（Expressive hero icon）
            Surface(
                modifier = Modifier.size(72.dp),
                shape    = CircleShape,
                color    = Color(0xFF229ED9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "✈",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color    = Color.White,
                            fontSize = 32.sp,
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            CircularProgressIndicator(
                modifier    = Modifier.size(28.dp),
                color       = Color(0xFF229ED9),
                strokeWidth = 3.dp,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text  = "正在验证 Telegram 授权…",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  SuccessCard — 登录成功
//  Expressive spring 弹跳缩放（stiffnessMediumLow，过冲感强）
// ──────────────────────────────────────────────────────────────────
@Composable
private fun SuccessCard(name: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "SuccessScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape  = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 对勾圆形
            Surface(
                modifier = Modifier.size(72.dp),
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "✓",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color      = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 36.sp,
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text  = "登录成功",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "欢迎，$name",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            )

            Spacer(Modifier.height(10.dp))

            // Expressive 小胶囊来源标签
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF229ED9).copy(alpha = 0.15f)
            ) {
                Text(
                    text     = "通过 Telegram 认证",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF229ED9),
                        fontWeight = FontWeight.SemiBold,
                    )
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  ErrorCard — 登录失败
// ──────────────────────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.error
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "✕",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color      = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 32.sp,
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text  = "授权失败",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onErrorContainer,
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color     = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text  = "即将返回登录页…",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
