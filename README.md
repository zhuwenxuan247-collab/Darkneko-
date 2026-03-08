# QuoteWidget v4 — MD3 Expressive + 账号同步 + API 36

## 新特性（v3 → v4）

| 模块 | 变化 |
|------|------|
| 设计语言 | MD3 → **MD3 Expressive**（tertiary 三角色盘 / 大圆角 / spring 动画）|
| 账号同步 | 新增 Google / Telegram **OAuth 登录注册** |
| 数据持久化 | DataStore Preferences 存储会话快照 |
| 精确闹钟 | USE_EXACT_ALARM (API 33+) + 权限变更实时监听 + 降级自动升级 |
| 构建工具链 | Kotlin **2.0.21** + AGP **8.7.3** + Gradle **8.11.1** + API **36** |

---

## 使用前必做：填写两处配置

### 1. Google Client ID
文件：`app/src/main/java/com/xc/quotewidget/auth/AuthRepository.kt`
```kotlin
private val googleClientId: String = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com"
```
获取方式：Google Cloud Console → API 和服务 → 凭据 → 创建 OAuth 2.0 客户端 ID（Android 类型）

### 2. Telegram Bot 用户名
文件：同上
```kotlin
private val telegramBotUsername: String = "YOUR_BOT_USERNAME"
```
获取方式：Telegram → @BotFather → /newbot 创建 Bot → /setdomain 绑定回调域 `quotewidget://telegram-callback`

---

## 手机端打包（GitHub Actions）

1. 完成上方两处配置
2. 上传代码到 GitHub 仓库（public 或 private 均可）
3. 推送触发 Actions → 约 5 分钟 → 下载 `QuoteWidget-v4-debug.zip` → 解压安装 APK

---

## 安装后操作（Pixel 8 Pro / API 36）

API 36 设备声明了 `USE_EXACT_ALARM`（normal permission），**无需手动授权**，零点换言自动精确触发。

若在 API 31-32 设备上测试：
- 设置 → 应用 → 名言组件 → 闹钟和提醒 → 开启

---

## 项目结构

```
com.xc.quotewidget/
├── auth/
│   ├── UserSession.kt          # DataStore 会话持久化
│   ├── AuthRepository.kt       # Google Credential Manager + Telegram OAuth
│   └── AuthViewModel.kt        # UI 状态机
├── ui/
│   ├── Theme.kt                # MD3 Expressive MaterialTheme
│   ├── LoginScreen.kt          # 登录页 Compose UI
│   ├── LoginActivity.kt        # 登录页宿主
│   ├── MainActivity.kt         # 主页（账号信息 + 权限引导）
│   └── TelegramCallbackActivity.kt  # 深链接回调处理
├── QuoteGlanceWidget.kt        # Glance Widget（3 档响应式 + Expressive）
├── QuoteWidgetReceiver.kt      # GlanceAppWidgetReceiver
├── RefreshAction.kt            # 点击换言回调
├── MidnightAlarmScheduler.kt   # 精确闹钟调度（三档权限分支）
├── AlarmReceiver.kt            # 零点触发器
├── AlarmPermissionReceiver.kt  # 权限状态变更监听
├── BootReceiver.kt             # 开机重注册
└── QuoteRepository.kt          # 名言数据 + SharedPreferences
```
