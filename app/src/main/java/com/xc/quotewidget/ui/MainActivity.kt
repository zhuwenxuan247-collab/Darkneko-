package com.xc.quotewidget.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xc.quotewidget.MidnightAlarmScheduler
import com.xc.quotewidget.auth.AuthUiState
import com.xc.quotewidget.auth.AuthViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuoteWidgetTheme {
                MainScreen(
                    onNavigateToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(androidx.compose.ui.platform.LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val state = uiState) {
                is AuthUiState.LoggedIn -> {
                    // 已登录：展示账号信息卡 + 闹钟权限状态
                    AccountInfoCard(
                        user            = state.user,
                        canExactAlarm   = MidnightAlarmScheduler.canScheduleExact(context),
                        onOpenAlarmSettings = {
                            MidnightAlarmScheduler.openExactAlarmSettings(context)
                        },
                        onSwitchAccount = onNavigateToLogin,
                        onSignOut       = { viewModel.signOut() }
                    )
                }
                else -> {
                    // 未登录 / 加载中：引导进入登录页
                    WelcomeCard(onLogin = onNavigateToLogin)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  AccountInfoCard — 已登录主页面
// ──────────────────────────────────────────────────────────────────
@Composable
private fun AccountInfoCard(
    user                : com.xc.quotewidget.auth.UserInfo,
    canExactAlarm       : Boolean,
    onOpenAlarmSettings : () -> Unit,
    onSwitchAccount     : () -> Unit,
    onSignOut           : () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 账号卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(28.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text  = "已登录",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = user.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
                if (user.email.isNotEmpty()) {
                    Text(
                        text  = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // 精确闹钟权限提示卡（仅权限未授予时显示）
        if (!canExactAlarm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text  = "⏰ 零点换言精度降低",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "精确闹钟权限未授予，每日换言可能偏移最多 30 分钟。点击下方按钮在设置中开启。",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAlarmSettings,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor   = MaterialTheme.colorScheme.onTertiary,
                        )
                    ) {
                        Text("前往设置开启", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 操作按钮组
        Button(
            onClick  = onSwitchAccount,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(16.dp),
        ) {
            Text("切换账号")
        }
        Button(
            onClick  = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer,
            )
        ) {
            Text("退出登录")
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  WelcomeCard — 未登录引导
// ──────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeCard(onLogin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "名言组件",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "登录账号后可同步名言偏好，并在多设备上保持一致的桌面体验。",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = onLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "登录 / 注册",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
