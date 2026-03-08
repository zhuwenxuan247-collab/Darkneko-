package com.xc.quotewidget

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

// ══════════════════════════════════════════════════════════════════
//  MD3 Expressive 色盘（API < 31 回退方案）
//
//  Expressive 相较于标准 MD3 的色盘差异：
//  ① tertiary / tertiaryContainer 被大量用于强调装饰元素
//  ② surfaceContainerHighest 用于卡片底层，提供更丰富的层次感
//  ③ primaryContainer 作为次级交互面，替代单一 primary
//
//  动态取色（API 31+）已内建 Expressive 色阶，无需手动覆盖
// ══════════════════════════════════════════════════════════════════
private val ExpressiveFallbackLight = lightColorScheme(
    primary                = Color(0xFF6750A4), // Expressive violet
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFEADDFF),
    onPrimaryContainer     = Color(0xFF21005D),
    secondary              = Color(0xFF625B71),
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFE8DEF8),
    onSecondaryContainer   = Color(0xFF1D192B),
    tertiary               = Color(0xFF7D5260), // Expressive rose accent
    onTertiary             = Color(0xFFFFFFFF),
    tertiaryContainer      = Color(0xFFFFD8E4),
    onTertiaryContainer    = Color(0xFF31111D),
    surface                = Color(0xFFFEF7FF),
    onSurface              = Color(0xFF1C1B1F),
    onSurfaceVariant       = Color(0xFF49454F),
    surfaceContainerHighest= Color(0xFFECE6F0),
    outline                = Color(0xFF79747E),
)
private val ExpressiveFallbackDark = darkColorScheme(
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
    outline                = Color(0xFF938F99),
)

// ── 响应式断点（与 v3 保持一致） ──────────────────────────────────
private val SIZE_COMPACT  = DpSize(150.dp, 100.dp)
private val SIZE_STANDARD = DpSize(250.dp, 110.dp)
private val SIZE_LARGE    = DpSize(250.dp, 180.dp)

class QuoteGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_STANDARD, SIZE_LARGE)
    )

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val quote = QuoteRepository.getCurrentQuote(context)

        provideContent {
            val ctx = LocalContext.current

            // API 31+ 动态取色已包含 Expressive 色阶（Android 12+ Material You）
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorProviders(
                    day   = dynamicLightColorScheme(ctx),
                    night = dynamicDarkColorScheme(ctx)
                )
            } else {
                ColorProviders(
                    day   = ExpressiveFallbackLight,
                    night = ExpressiveFallbackDark
                )
            }

            GlanceTheme(colors = colors) {
                WidgetContent(quote)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  响应式分发：按实际渲染尺寸路由到对应布局
// ──────────────────────────────────────────────────────────────────
@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
private fun WidgetContent(quote: Quote) {
    val size = LocalSize.current

    // MD3 Expressive：卡片底色使用 surfaceContainerHighest 提升层次感
    val rootMod = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.surfaceVariant)
        .cornerRadius(24.dp)   // Expressive 使用更大圆角（24dp vs 标准 12dp）
        .clickable(actionRunCallback<RefreshAction>())

    when {
        size.height >= SIZE_LARGE.height    -> LargeLayout(quote, rootMod)
        size.width  >= SIZE_STANDARD.width  -> StandardLayout(quote, rootMod)
        else                                -> CompactLayout(quote, rootMod)
    }
}

// ──────────────────────────────────────────────────────────────────
//  COMPACT（2×2 以内）
//  Expressive 改动：引号改用 tertiaryContainer 色块作装饰背景片段
// ──────────────────────────────────────────────────────────────────
@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
private fun CompactLayout(quote: Quote, mod: GlanceModifier) {
    Box(
        modifier         = mod.padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            // Expressive 装饰点：tertiary 小圆角色块衬底引号符号
            Box(
                modifier         = GlanceModifier
                    .background(GlanceTheme.colors.tertiaryContainer)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "\u201c",
                    style = TextStyle(
                        color      = GlanceTheme.colors.onTertiaryContainer,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                )
            }
            Spacer(GlanceModifier.height(5.dp))
            Text(
                text     = quote.text,
                maxLines = 3,
                style    = TextStyle(
                    color     = GlanceTheme.colors.onSurface,
                    fontSize  = 11.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text  = "— ${quote.author}",
                style = TextStyle(
                    color    = GlanceTheme.colors.secondary,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  STANDARD（3×2，默认场景）
//
//  Expressive 改动：
//  ① 左侧竖向 primaryContainer 色条作节奏分隔（Expressive "expressive container" 模式）
//  ② 引号移入色条顶端
//  ③ 底部「轻触换一条」改用 secondaryContainer 胶囊标签
// ──────────────────────────────────────────────────────────────────
@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
private fun StandardLayout(quote: Quote, mod: GlanceModifier) {
    Row(
        modifier            = mod.padding(14.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        // 左侧 Expressive 竖向色条 + 引号
        Column(
            modifier            = GlanceModifier
                .fillMaxHeight()
                .width(28.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment   = Alignment.Top
        ) {
            Text(
                text  = "\u201c",
                style = TextStyle(
                    color      = GlanceTheme.colors.onPrimaryContainer,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
        }

        Spacer(GlanceModifier.width(10.dp))

        // 右侧内容区
        Column(
            modifier            = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text     = quote.text,
                maxLines = 4,
                style    = TextStyle(
                    color     = GlanceTheme.colors.onSurface,
                    fontSize  = 12.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Start,
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text  = "— ${quote.author}",
                style = TextStyle(
                    color      = GlanceTheme.colors.tertiary,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            // Expressive 胶囊标签（secondaryContainer）
            Box(
                modifier = GlanceModifier
                    .wrapContentWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(20.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "轻触换一条",
                    style = TextStyle(
                        color    = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 9.sp,
                    )
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
//  LARGE（4×4+）
//
//  Expressive 改动：
//  ① 顶部横向 tertiaryContainer 色带作标题区（"expressive header zone"）
//  ② 作者 + 大引号组合进入标题区，正文独占下方空间
//  ③ 底部操作标签使用 primaryContainer 胶囊
// ──────────────────────────────────────────────────────────────────
@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
private fun LargeLayout(quote: Quote, mod: GlanceModifier) {
    Column(
        modifier            = mod,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment   = Alignment.Top
    ) {
        // ── Expressive 顶部标题色带 ────────────────────────────
        Row(
            modifier            = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.tertiaryContainer)
                // 仅顶部两角保持圆角，与卡片融合
                .cornerRadius(24.dp)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text  = "\u201c",
                style = TextStyle(
                    color      = GlanceTheme.colors.onTertiaryContainer,
                    fontSize   = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(
                modifier          = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = quote.author,
                    style = TextStyle(
                        color      = GlanceTheme.colors.onTertiaryContainer,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                )
                Text(
                    text  = "名人名言",
                    style = TextStyle(
                        color    = GlanceTheme.colors.tertiary,
                        fontSize = 10.sp,
                    )
                )
            }
        }

        // ── 正文区 ────────────────────────────────────────────
        Column(
            modifier            = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text  = quote.text,
                style = TextStyle(
                    color     = GlanceTheme.colors.onSurface,
                    fontSize  = 15.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Start,
                )
            )
        }

        // ── 底部操作区 ────────────────────────────────────────
        Row(
            modifier            = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(20.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "轻触换一条",
                    style = TextStyle(
                        color      = GlanceTheme.colors.onPrimaryContainer,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                )
            }
        }
    }
}
