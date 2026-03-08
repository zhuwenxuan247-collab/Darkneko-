package com.xc.quotewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 零点闹钟触发器
 *
 * goAsync() 使 onReceive 生命周期延伸到协程完成，
 * 防止系统在 Glance updateAll() 挂起调用结束前回收进程。
 * 自我重调度（链式单次闹钟）在 Glance 更新完成后执行，
 * 确保即使系统在更新期间发生重启，下一次闹钟也已注册。
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        QuoteRepository.advanceToNext(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                QuoteGlanceWidget().updateAll(context)
            } finally {
                // 无论 updateAll 成功与否，都重新注册下一次零点闹钟
                MidnightAlarmScheduler.schedule(context)
                pending.finish()
            }
        }
    }
}
