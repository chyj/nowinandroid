# 构建变体配置说明

## 概述

已添加新的 product flavors 以支持不同的 AdMob 广告类型变体，同时保持源码包名 `com.google.samples.apps.nowinandroid` 不变。

## 新增的构建变体

### Banner 变体
- **Flavor**: `banner`
- **Application ID**: `com.google.samples.apps.nowinandroid.banner` (release)
- **Application ID**: `com.google.samples.apps.nowinandroid.banner.debug` (debug)

### Interstitial 变体
- **Flavor**: `interstitial`
- **Application ID**: `com.google.samples.apps.nowinandroid.interstitial` (release)
- **Application ID**: `com.google.samples.apps.nowinandroid.interstitial.debug` (debug)

## 构建命令

### 构建 Banner Debug APK
```bash
./gradlew assembleBannerDebug
```

### 构建 Banner Release APK
```bash
./gradlew assembleBannerRelease
```

### 构建 Interstitial Debug APK
```bash
./gradlew assembleInterstitialDebug
```

### 构建 Interstitial Release APK
```bash
./gradlew assembleInterstitialRelease
```

### 构建 Demo Debug APK（原有变体）
```bash
./gradlew assembleDemoDebug
```

## 配置更改摘要

### 1. NiaFlavor.kt
- 添加了 `banner` flavor，applicationIdSuffix = `.banner`
- 添加了 `interstitial` flavor，applicationIdSuffix = `.interstitial`

### 2. google-services.json
- 添加了以下包名配置：
  - `com.google.samples.apps.nowinandroid.banner.debug`
  - `com.google.samples.apps.nowinandroid.banner`
  - `com.google.samples.apps.nowinandroid.interstitial.debug`
  - `com.google.samples.apps.nowinandroid.interstitial`

## 重要提醒

### Firebase/AdMob 后台配置

**⚠️ 必须在 Firebase Console 和 AdMob Console 中添加以下包名：**

1. `com.google.samples.apps.nowinandroid.banner.debug`
2. `com.google.samples.apps.nowinandroid.banner`
3. `com.google.samples.apps.nowinandroid.interstitial.debug`
4. `com.google.samples.apps.nowinandroid.interstitial`

### Firebase Console 配置步骤：
1. 登录 [Firebase Console](https://console.firebase.google.com/)
2. 选择项目
3. 进入项目设置 > 您的应用
4. 为每个包名添加 Android 应用
5. 下载对应的 `google-services.json` 并合并到项目中的 `app/google-services.json`

### AdMob Console 配置步骤：
1. 登录 [AdMob Console](https://apps.admob.com/)
2. 进入应用设置
3. 为每个包名添加应用
4. 为每个应用创建对应的广告单元（Banner 或 Interstitial）

## 验证构建

### 前置条件
- Java 17 或更高版本
- Android SDK 已正确配置

### 验证步骤

1. **列出所有构建变体**：
   ```bash
   ./gradlew tasks --all | grep assemble
   ```

2. **构建特定变体**：
   ```bash
   ./gradlew assembleBannerDebug
   ```

3. **检查生成的 APK**：
   ```bash
   ls -lh app/build/outputs/apk/banner/debug/
   ```

4. **验证 Application ID**：
   使用 `aapt` 工具检查 APK 的包名：
   ```bash
   aapt dump badging app/build/outputs/apk/banner/debug/app-banner-debug.apk | grep package
   ```
   应该显示：`package: name='com.google.samples.apps.nowinandroid.banner.debug'`

## 源码包名保持不变

✅ **重要**：所有 Kotlin 源码文件中的 `package` 声明保持为：
```kotlin
package com.google.samples.apps.nowinandroid
```

只有通过 Gradle 构建配置中的 `applicationIdSuffix` 来区分不同的 APK 变体。

## 文件更改清单

1. `build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/NiaFlavor.kt`
   - 添加 `banner` 和 `interstitial` flavor 定义

2. `app/google-services.json`
   - 添加 4 个新的包名配置项

3. `BUILD_VARIANTS.md` (本文件)
   - 构建变体配置说明文档

