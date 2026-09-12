# 净空 CleanSweep

一款简洁、克制的 Android 垃圾清理应用原型，使用 Kotlin 与 Jetpack Compose 开发。

## 已实现

- 扫描并清理本应用自身的内部、外部缓存
- 扫描/结果/清理完成状态动画
- 一键进入 Android 系统存储管理页
- 清理建议与隐私说明页
- Material 3 三页式中文界面，适配 Android 8.0 及以上

## 运行

1. 用 Android Studio 打开 `CleanSweep` 文件夹。
2. 等待 Gradle 同步完成。
3. 连接 Android 设备或启动模拟器，运行 `app`。

也可以将项目推送到 GitHub，在 Actions 中运行 “Build Android APK”，构建完成后从 Artifacts 下载 `app-debug.apk`，传到安卓手机即可安装。

> Android 11 之后，普通应用不能在后台任意删除其他应用的缓存。净空只直接删除自身缓存；其他应用、照片、下载等内容由用户在系统存储管理页面确认处理。
