package com.xc.quotewidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 第一个组件实例被添加到桌面时，启动零点闹钟
        MidnightAlarmScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个组件实例被移除时，取消闹钟
        MidnightAlarmScheduler.cancel(context)
    }
}
