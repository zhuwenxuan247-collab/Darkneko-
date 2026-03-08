package com.xc.quotewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机重注册闹钟
 *
 * AlarmManager 闹钟在设备重启后全部清除，必须在开机后重新注册。
 * schedule() 内部已处理 USE_EXACT_ALARM / SCHEDULE_EXACT_ALARM / 降级三个路径，
 * BootReceiver 无需重复判断权限。
 *
 * QUICKBOOT_POWERON：小米 / 一加 / vivo 等国产 ROM 的快速启动广播，
 * 标准 BOOT_COMPLETED 在这些设备上可能延迟或不触发。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                MidnightAlarmScheduler.schedule(context)
            }
        }
    }
}
