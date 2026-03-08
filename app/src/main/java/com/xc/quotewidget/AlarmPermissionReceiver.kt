package com.xc.quotewidget

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 监听精确闹钟权限状态变化（API 33+ 专属广播）
 *
 * 触发场景：
 *  - 用户在「设置 → 应用 → 名言组件 → 闹钟和提醒」将开关从关 → 开
 *  - 用户将开关从开 → 关（此时 canScheduleExactAlarms() 返回 false）
 *
 * 修复逻辑：
 *  - 授权 ON：调用 rescheduleOnPermissionGrant()，将现有的降级
 *    setAndAllowWhileIdle 闹钟替换为精确的 setExactAndAllowWhileIdle
 *  - 授权 OFF：精确闹钟已被系统自动取消，重新以降级模式注册，
 *    确保零点换言功能在失去精确权限后仍然工作（误差 ≤ 30 分钟）
 *
 * 注意：API 33 以下此广播不存在，Manifest 的 minSdk = 26，
 *       因此用 SDK 版本守卫，低版本设备不会注册此 Receiver。
 */
class AlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 双重守卫：广播 action + SDK 版本
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (am.canScheduleExactAlarms()) {
            // 权限刚被授予 → 升级到精确模式
            MidnightAlarmScheduler.rescheduleOnPermissionGrant(context)
        } else {
            // 权限刚被撤销 → 系统已自动取消精确闹钟，重新降级注册
            // cancel() 先清理可能残留的 PendingIntent，再 schedule() 以降级模式重建
            MidnightAlarmScheduler.cancel(context)
            MidnightAlarmScheduler.schedule(context)
        }
    }
}
