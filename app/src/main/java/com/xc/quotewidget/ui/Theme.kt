package com.xc.quotewidget.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ══════════════════════════════════════════════════════════════════
//  MD3 Expressive 静态色盘（API < 31 回退）
//  与 Widget 侧色盘保持一致，确保视觉连续性
// ══════════════════════════════════════════════════════════════════
private val ExpressiveLightColors = lightColorScheme(
    primary                = Color(0xFF6750A4),
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFEADDFF),
    onPrimaryContainer     = Color(0xFF21005D),
    secondary              = Color(0xFF625B71),
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFE8DEF8),
    onSecondaryContainer   = Color(0xFF1D192B),
    tertiary               = Color(0xFF7D5260),
    onTertiary             = Color(0xFFFFFFFF),
    tertiaryContainer      = Color(0xFFFFD8E4),
    onTertiaryContainer    = Color(0xFF31111D),
    surface                = Color(0xFFFEF7FF),
    onSurface              = Color(0xFF1C1B1F),
    onSurfaceVariant       = Color(0xFF49454F),
    surfaceContainerHighest= Color(0xFFECE6F0),
    surfaceContainerLow    = Color(0xFFF7F2FA),
    outline                = Color(0xFF79747E),
    outlineVariant         = Color(0xFFCAC4D0),
)

private val ExpressiveDarkColors = darkColorScheme(
    primary                = Color(0xFFD0BCFF),
    onPrimary              = Color(0xFF381E72),
    primaryContainer       = Color(0xFF4F378B),
    onPrimaryContainer     = Color(0xFFEADDFF),
    secondary              = Color(0xFFCCC2DC),
    onSecondary            = Color(0xFF332D41),
    secondaryContainer     = Color(0xFF4A4458),
    onSecondaryContainer   = Color(0xFFE8DEF8),
    tertiary               = Color(0xFFEFB8C8),
    onTertiary             = Color(0xFF492532),
    tertiaryContainer      = Color(0xFF633B48),
    onTertiaryContainer    = Color(0xFFFFD8E4),
    surface                = Color(0xFF141218),
    onSurface              = Color(0xFFE6E1E5),
    onSurfaceVariant       = Color(0xFFCAC4D0),
    surfaceContainerHighest= Color(0xFF36343B),
    surfaceContainerLow    = Color(0xFF1D1B20),
    outline                = Color(0xFF938F99),
    outlineVariant         = Color(0xFF49454F),
)

/**
 * App 级 MD3 Expressive 主题包装器。
 * 所有 Activity 内的 Compose 内容都应包裹在此函数中。
 *
 * @param darkTheme 默认跟随系统深色模式
 */
@Composable
fun QuoteWidgetTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> ExpressiveDarkColors
        else      -> ExpressiveLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // MD3 Expressive 1.4.x：MaterialTheme 自动应用 MotionScheme spring 动画曲线
        // 无需手动传入 motionScheme，默认即为 Expressive spring preset
        content     = content
    )
}
