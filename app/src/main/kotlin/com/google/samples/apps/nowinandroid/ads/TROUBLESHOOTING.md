# AdMob 插页式广告问题排查指南

## 问题现象

- ✅ 官方示例可以正常运行并获得广告
- ❌ 我们的实现出现错误：
  - UMP 同意流程错误（错误代码 2 = NETWORK_ERROR）
  - 广告加载失败（错误代码 0 = INTERNAL_ERROR）

## 已修复的问题

### 1. ✅ 调试模式配置问题（已修复）

**问题**：在调试模式下，如果没有提供测试设备 ID，不会设置调试地理区域。

**修复**：现在在调试模式下，即使没有测试设备 ID，也会设置 `DEBUG_GEOGRAPHY_EEA`，强制显示同意表单。

```kotlin
if (isDebug) {
    val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    // ...
}
```

### 2. ✅ MobileAds SDK 初始化检查（已添加）

**问题**：可能在 MobileAds SDK 完全初始化之前就尝试加载广告。

**修复**：添加了 SDK 版本检查，确保 SDK 已初始化后再加载广告。

## 其他可能的原因

### 1. UMP 配置问题

**检查项**：
- [ ] 在 AdMob 控制台是否正确配置了 UMP 同意流程
- [ ] UMP 消息是否已发布（不是草稿状态）
- [ ] 是否选择了正确的隐私消息模板

**解决方案**：
1. 登录 [AdMob 控制台](https://apps.admob.com/)
2. 进入"隐私和消息" > "用户消息"
3. 确认消息已发布并激活
4. 检查消息配置是否正确

### 2. 应用包名问题

**检查项**：
- [ ] AdMob 控制台中的应用包名是否与 `build.gradle.kts` 中的 `applicationId` 完全一致
- [ ] Debug 和 Release 版本的包名是否都正确配置

**当前包名**：
- Debug: `com.google.samples.apps.nowinandroid.demo.debug`
- Release: `com.google.samples.apps.nowinandroid`

### 3. 广告单元配置问题

**检查项**：
- [ ] 插页式广告单元是否已创建
- [ ] 广告单元是否已激活
- [ ] 广告单元 ID 是否正确

**当前使用的测试 ID**：
- App ID: `ca-app-pub-3940256099942544~3347511713`
- Ad Unit ID: `ca-app-pub-3940256099942544/1033173712`

### 4. 依赖版本问题

**检查项**：
- [ ] `play-services-ads` 版本是否与官方示例一致
- [ ] `ump` 版本是否与官方示例一致

**当前版本**：
- `play-services-ads`: 23.5.0
- `ump`: 2.2.0

### 5. ProGuard/R8 混淆问题

**检查项**：
- [ ] Release 构建是否启用了代码混淆
- [ ] 是否正确配置了 ProGuard 规则

**解决方案**：
如果使用 Release 构建，需要添加 ProGuard 规则（当前使用 Debug 构建，应该不受影响）。

### 6. Activity 生命周期问题

**检查项**：
- [ ] Activity 是否在正确的生命周期阶段调用广告相关方法
- [ ] Activity 是否可能被销毁或重建

**当前实现**：
- 在 `MainActivity.onCreate()` 中初始化同意流程 ✅
- 使用 `@AndroidEntryPoint` 确保 Activity 正确注入 ✅

### 7. Context 使用问题

**检查项**：
- [ ] 是否使用了正确的 Context（Application vs Activity）
- [ ] Context 是否可能为 null 或已销毁

**当前实现**：
- `ConsentInformation` 使用 `@ApplicationContext` ✅
- `InterstitialAd.load()` 使用 Activity ✅

## 调试步骤

### 1. 对比官方示例

1. 下载官方示例：[InterstitialExample](https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample)
2. 运行官方示例，确认可以正常工作
3. 对比以下关键点：
   - AndroidManifest.xml 配置
   - MobileAds 初始化时机
   - UMP 同意流程实现
   - 广告加载时机

### 2. 检查日志输出

运行应用后，检查以下日志：

```
ConfigVerification: ✅ App ID found
ConfigVerification: ✅ Ad Unit ID format is correct
NiaApplication: [LIFECYCLE] ✅ Mobile Ads SDK initialized successfully
ConsentManager: [LIFECYCLE] Consent info updated successfully
InterstitialAdManager: [LIFECYCLE] ✅ onAdLoaded() - Ad loaded successfully
```

### 3. 验证配置

使用配置验证工具（已自动运行）：
- 查看 `ConfigVerification` 标签的日志
- 确认所有配置项都显示 ✅

### 4. 测试步骤

1. **清除应用数据**：
   ```bash
   adb shell pm clear com.google.samples.apps.nowinandroid.demo.debug
   ```

2. **重新安装应用**：
   ```bash
   ./gradlew installDemoDebug
   ```

3. **运行应用并查看日志**：
   - 过滤标签：`ConfigVerification`, `ConsentManager`, `InterstitialAdManager`, `NiaApplication`
   - 查看完整的生命周期日志

4. **测试广告显示**：
   - 等待同意流程完成
   - 等待广告加载完成（查看日志确认）
   - 点击导航按钮（Saved/Interests）

## 如果问题仍然存在

如果按照以上步骤检查后问题仍然存在，请提供：

1. **完整的 logcat 输出**（从应用启动到点击导航按钮）
2. **配置验证日志**（`ConfigVerification` 标签）
3. **官方示例的运行情况**（确认官方示例确实可以工作）
4. **AdMob 控制台截图**（UMP 配置页面）

## 参考资源

- [Google AdMob 官方示例](https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample)
- [AdMob 快速入门](https://developers.google.com/admob/android/quick-start)
- [插页式广告指南](https://developers.google.com/admob/android/interstitial)
- [UMP SDK 文档](https://developers.google.com/admob/android/privacy/overview)

