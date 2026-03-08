package com.xc.quotewidget.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xc.quotewidget.R
import com.xc.quotewidget.auth.AuthUiState
import com.xc.quotewidget.auth.AuthViewModel

// ══════════════════════════════════════════════════════════════════
//  LoginScreen
//  MD3 Expressive 设计要点：
//  ① 顶部大面积 primaryContainer 渐变背景块（Expressive hero zone）
//  ② 品牌 icon 使用超大尺寸（72dp），突破标准 MD3 的 56dp 惯例
//  ③ 登录按钮使用 ButtonGroup 视觉分组（FilledTonal + Outlined 对比）
//  ④ 所有过渡动画使用 spring（stiffness = Medium）替代 tween，
//     符合 Expressive MotionScheme spring preset
//  ⑤ 已登录态用 Card + 头像环形裁切 + tertiary 徽章呈现
// ══════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // 错误消息 → Snackbar
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Failure -> {
                snackbar.showSnackbar(s.message)
                viewModel.clearError()
            }
            is AuthUiState.NoGoogleAccount -> {
                snackbar.showSnackbar("设备上未绑定 Google 账号，请先在系统设置中添加")
                viewModel.clearError()
            }
            is AuthUiState.LoggedIn -> onLoginSuccess()
            else -> Unit
        }
    }

    QuoteWidgetTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ── Expressive Hero Zone：顶部大色块背景 ────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                // ── 主内容列 ─────────────────────────────────────
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(64.dp))

                    // Expressive：超大品牌 icon（72dp）
                    Surface(
                        modifier  = Modifier.size(80.dp),
                        shape     = RoundedCornerShape(24.dp),
                        color     = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text  = "\u201c",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 96.sp,
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // App 名称
                    Text(
                        text  = "名言组件",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text  = "登录后可在多设备间同步你的名言偏好",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    )

                    Spacer(Modifier.height(40.dp))

                    // ── 状态动画切换 ───────────────────────────────
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            // Expressive spring 过渡
                            (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                             slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it / 4 })
                                .togetherWith(
                                    fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                    slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { -it / 4 }
                                )
                        },
                        label = "AuthStateTransition"
                    ) { state ->
                        when (state) {
                            is AuthUiState.LoggedIn -> LoggedInCard(
                                user       = state.user,
                                onSignOut  = { viewModel.signOut() }
                            )
                            is AuthUiState.Loading  -> LoadingCard()
                            else -> LoginButtonGroup(
                                onGoogleClick   = {
                                    viewModel.signInWithGoogle(context)
                                },
                                onTelegramClick = {
                                    viewModel.launchTelegramOAuth(context)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  LoginButtonGroup
//  MD3 Expressive ButtonGroup：FilledTonal（主）+ Outlined（次）
//  两按钮垂直排列，视觉重量形成层次感
// ──────────────────────────────────────────────────────────────────
@Composable
private fun LoginButtonGroup(
    onGoogleClick  : () -> Unit,
    onTelegramClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),   // Expressive 更大圆角
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "选择登录方式",
                style = MaterialTheme.typography.titleMedium.copy(
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            )

            Spacer(Modifier.height(20.dp))

            // Google 登录（FilledTonal = Expressive 主操作色调）
            FilledTonalButton(
                onClick  = onGoogleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            ) {
                // Google 色彩环形徽章
                GoogleColorDot()
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = "使用 Google 账号继续",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Telegram 登录（Outlined = 次要操作）
            OutlinedButton(
                onClick  = onTelegramClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                // Telegram 蓝色圆形纸飞机徽章
                TelegramDot()
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = "使用 Telegram 账号继续",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(12.dp))

            Text(
                text  = "登录即表示你同意本应用的服务条款与隐私政策",
                style = MaterialTheme.typography.bodySmall.copy(
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  LoggedInCard — 已登录态
//  MD3 Expressive：tertiaryContainer 徽章 + 头像大圆裁切 + spring 出现动画
// ──────────────────────────────────────────────────────────────────
@Composable
private fun LoggedInCard(
    user     : com.xc.quotewidget.auth.UserInfo,
    onSignOut: () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter   = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit    = scaleOut() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(28.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 头像 + provider 徽章
                Box(contentAlignment = Alignment.BottomEnd) {
                    // 头像
                    if (user.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model             = user.avatarUrl,
                            contentDescription = "头像",
                            modifier          = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // 无头像时显示首字母
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape    = CircleShape,
                            color    = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text  = user.displayName.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                )
                            }
                        }
                    }
                    // Provider 徽章（tertiaryContainer 小圆点）
                    Surface(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (user.provider) {
                                com.xc.quotewidget.auth.AuthProvider.GOOGLE   -> GoogleColorDot(size = 12.dp)
                                com.xc.quotewidget.auth.AuthProvider.TELEGRAM -> TelegramDot(size = 12.dp)
                                else -> Unit
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text  = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
                if (user.email.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 来源标签（Expressive 小胶囊）
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text     = "已通过 ${if (user.provider == com.xc.quotewidget.auth.AuthProvider.GOOGLE) "Google" else "Telegram"} 登录",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                TextButton(
                    onClick = onSignOut,
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("退出登录", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  LoadingCard — 加载态
// ──────────────────────────────────────────────────────────────────
@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Box(
            modifier        = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  辅助装饰组件
// ──────────────────────────────────────────────────────────────────

/** Google 四色小圆点（简化版 logo） */
@Composable
private fun GoogleColorDot(size: androidx.compose.ui.unit.Dp = 18.dp) {
    Row(
        modifier            = Modifier.size(size),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment   = Alignment.CenterVertically
    ) {
        // 用四象限色块模拟 Google logo
        val s = size / 2
        Column {
            Box(Modifier.size(s).background(Color(0xFF4285F4), RoundedCornerShape(topStart = s / 2)))
            Box(Modifier.size(s).background(Color(0xFF34A853), RoundedCornerShape(bottomStart = s / 2)))
        }
        Column {
            Box(Modifier.size(s).background(Color(0xFFEA4335), RoundedCornerShape(topEnd = s / 2)))
            Box(Modifier.size(s).background(Color(0xFFFBBC05), RoundedCornerShape(bottomEnd = s / 2)))
        }
    }
}

/** Telegram 蓝色圆点 */
@Composable
private fun TelegramDot(size: androidx.compose.ui.unit.Dp = 18.dp) {
    Box(
        modifier         = Modifier
            .size(size)
            .background(Color(0xFF229ED9), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 纸飞机用文字符号近似替代，避免需要外部图片资源
        Text(
            text  = "✈",
            style = androidx.compose.ui.text.TextStyle(
                color    = Color.White,
                fontSize = (size.value * 0.55f).sp,
            )
        )
    }
}
