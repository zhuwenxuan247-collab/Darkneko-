package com.xc.quotewidget.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xc.quotewidget.MidnightAlarmScheduler

/**
 * 登录页宿主 Activity。
 *
 * 启动场景：
 *  - 用户首次打开 App（MainActivity 检测到 UserSession 为 null 时跳转）
 *  - 用户从设置页手动点击「切换账号」
 *
 * 登录成功后：
 *  - 回调 onLoginSuccess → finish()，让 MainActivity 响应 onResume 重新取 session
 *
 * TelegramCallbackActivity 也会在同一 Task 栈中启动，
 * 回调完成后通过 FLAG_ACTIVITY_CLEAR_TOP 回到此 Activity，
 * 由 AuthViewModel 的 StateFlow 推送 LoggedIn 状态触发跳转。
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge：全面屏沉浸式，配合 Expressive hero zone 顶部大色块
        enableEdgeToEdge()

        setContent {
            LoginScreen(
                onLoginSuccess = {
                    // 登录完成后若精确闹钟还未注册（首次安装），立即注册
                    MidnightAlarmScheduler.schedule(applicationContext)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            )
        }
    }
}
