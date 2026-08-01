# nap511

115 网盘第三方 Android 客户端，基于 [zerorooot/nap511](https://github.com/zerorooot/nap511) 二次开发，采用 [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)（HyperOS 设计语言）UI 框架。

## 与原版的主要区别

- **UI 框架**：从 Material 3 迁移至 Miuix（HyperOS），支持智能深色模式与 Monet 动态取色
- **毛玻璃效果**：通过 miuix-blur 实现系统级模糊背景
- **圆角设计**：采用 Miuix Squircle 超椭圆圆角，贴近 HyperOS 视觉风格
- **编译环境**：compileSdk 37 / minSdk 33 / targetSdk 36
- **CI 构建**：使用 GitHub Actions 自动编译 Debug APK

## 功能

| 模块 | 已实现 | 计划中 |
|------|--------|--------|
| 登录 | 账号密码登录、Cookie 登录、登出 | - |
| 网盘文件 | 剪切、删除、重命名、新建文件夹、多选、回收站、获取下载链接、搜索、在线解压 | - |
| 离线文件 | 离线列表、跳转网盘文件夹、查看视频、删除、清空 | - |
| 文件预览 | 文本、音频、照片、视频 | - |
| 离线下载 | 磁力链接、115sha1 导出、种子离线 | - |
| 自定义 | 单次请求数量、默认离线位置 | - |
| 其他 | 磁力跳转 | - |

**不支持**：文件上传/下载、两步验证、安全密钥

## 技术栈

| 技术 | 说明 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Miuix 0.9.3 |
| 主题 | MiuixTheme（ColorSchemeMode.System，支持 Monet 动态取色） |
| 架构 | MVVM |
| 网络 | Retrofit + OkHttp |
| 图片加载 | Coil |
| 视频播放 | GSYVideoPlayer |
| 构建工具 | Gradle (Kotlin DSL) + Version Catalog |

## 构建

### 本地构建

需要 Android Studio 及以下 SDK 组件：

- compileSdk 37（预览版，需通过 canary 通道安装）
- build-tools 37.0.0
- JDK 21

```bash
# 安装 SDK 37 预览版
sdkmanager --channel=3 --install "platforms;android-37.1" "build-tools;37.0.0"

# 构建 Debug APK
./gradlew assembleDebug
```

### CI 构建

推送代码后 GitHub Actions 自动触发构建，产物为 `app-debug.apk`。也可在 Actions 页面手动触发（`workflow_dispatch`）。

## 下载

- **稳定版**：https://github.com/zerorooot/nap511/releases
- **CI 构建产物**：需登录 GitHub，在 Actions 页面的构建记录中下载 Artifact

## 开源协议

本项目基于原 [nap511](https://github.com/zerorooot/nap511) 项目，遵循其原有开源协议。

Miuix 组件库使用 Apache License 2.0。
