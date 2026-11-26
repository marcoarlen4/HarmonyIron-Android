# HarmonyIron-Android（鸿铁）

面向健身房力量训练人群的纯原生 Android 应用（Kotlin + Jetpack Compose + Room + Glance）。

## 核心特性（V1.0 MVP）
- **极简练中记录**：历史重量次数自动带入、单手 `+/-` 快调、RPE 标记、PR 里程碑实时提示
- **锁屏组间倒计时**：`Foreground Service` 常驻实时倒计时通知与到点震动提醒
- **桌面微件**：`Jetpack Glance` 展示本周总容量与“一键开练”直达
- **科学分析与隐私**：`StrengthMath` 1RM 估算与 Compose `Canvas` 自绘图表，纯本地存储并支持双端通用 JSON 备份
