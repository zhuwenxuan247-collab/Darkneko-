package com.xc.quotewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import java.util.Calendar

/**
 * 零点精确闹钟调度器（API 36 完整修复版）
 *
 * 问题根因：
 *  - API 31+ 引入 SCHEDULE_EXACT_ALARM 权限，默认不授予
 *  - API 33+ 新增 ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED 广播：
 *    用户在设置里授权/撤销后，旧的 setAndAllowWhileIdle 降级闹钟不会自动升级，
 *    必须主动重新注册
 *  - API 36.1 的 battery restriction 策略收紧：即使已授权，
 *    若 App 长期未被交互，系统可能在 restriction 周期内跳过精确触发窗口
 *    → 修复方案：每次触发后自我重调度，并在权限状态变更时立即重注册
 *
 * 完整修复清单：
 *  ① schedule() — 三档权限分支，含 USE_EXACT_ALARM（API 33 新增，无需用户授权）
 *  ② rescheduleOnPermissionGrant() — 权限授予后从降级模式升级到精确模式
 *  ③ openExactAlarmSettings() — 引导用户跳转到精确闹钟设置页
 *  ④ nextMidnightMillis() — 计算下一零点，时区安全
 */
object MidnightAlarmScheduler {

    private const val REQUEST_CODE = 7700

    fun schedule(context: Context) {
        val am       = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi       = buildPendingIntent(context)
        val triggerAt = nextMidnightMillis()

        when {
            // ── 分支 A：API 33+
            // USE_EXACT_ALARM 是 normal permission（AndroidManifest 声明即可，
            // 无需运行时授权），优先使用，完全跳过权限弹窗问题
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // USE_EXACT_ALARM 在 API 33+ 可用且已在 Manifest 声明时直接精确调度
                // 若设备厂商不支持（理论上不应发生），回退到 canScheduleExactAlarms 分支
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    // 极端情况：OEM 锁定了所有精确闹钟 → 非精确降级
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }

            // ── 分支 B：API 31-32
            // SCHEDULE_EXACT_ALARM 需用户手动授权
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    // 降级；权限授予后 AlarmPermissionReceiver 会调用 rescheduleOnPermissionGrant 升级
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }

            // ── 分支 C：API 23-30，setExactAndAllowWhileIdle 无需任何权限
            else -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * 当 AlarmPermissionReceiver 收到 ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
     * 且 canScheduleExactAlarms() == true 时调用。
     * 取消旧的降级闹钟，重新注册精确闹钟。
     */
    fun rescheduleOnPermissionGrant(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            // cancel() 幂等：若已是精确闹钟也无副作用
            cancel(context)
            schedule(context)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context))
    }

    /**
     * 跳转到系统「闹钟和提醒」设置页，仅 API 31+ 需要引导用户手动授权时调用。
     * API 33+ 使用 USE_EXACT_ALARM（normal permission）不需要此方法。
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /** 是否当前已具备精确闹钟权限 */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 计算下一个本地零点的 epoch 毫秒。
     * 使用 Calendar.getInstance() 而非 System.currentTimeMillis() + 常量，
     * 确保夏令时（DST）切换时不产生偏移。
     */
    fun nextMidnightMillis(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
