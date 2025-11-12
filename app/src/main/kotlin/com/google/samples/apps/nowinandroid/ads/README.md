# AdMob 插页式广告集成说明

本文档说明如何配置和验证 Google AdMob 插页式广告集成。

## 需要替换的配置

### 1. AdMob App ID

**位置**: `app/src/main/AndroidManifest.xml`

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" />
```

**说明**: 
- 当前使用的是 Google 测试 App ID
- 替换为您的实际 AdMob App ID（格式：`ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`）
- 在 [AdMob 控制台](https://apps.admob.com/) 的"应用"部分可以找到您的 App ID

### 2. 插页式广告位 ID

**位置**: `app/src/main/kotlin/com/google/samples/apps/nowinandroid/ads/InterstitialAdManager.kt`

```kotlin
private val adUnitId = "ca-app-pub-3940256099942544/1033173712"
```

**说明**:
- 当前使用的是 Google 测试广告位 ID
- 替换为您的实际插页式广告位 ID（格式：`ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX`）
- 在 AdMob 控制台的"广告单元"部分创建插页式广告单元后可以获得

### 3. 测试设备 ID（可选）

**位置**: `app/src/main/kotlin/com/google/samples/apps/nowinandroid/MainActivity.kt`

```kotlin
val testDeviceId: String? = if (isDebug) {
    // 在调试模式下，可以设置测试设备 ID
    // 示例: "33BE2250B28B63B8F82BEC..."
    null // 设置为 null 以使用默认测试设备
} else {
    null
}
```

**说明**:
- 用于在测试设备上显示测试广告
- 获取方法：运行应用后查看 logcat，搜索 "Use RequestConfiguration.Builder().setTestDeviceIds"
- 将找到的设备 ID 替换示例中的值

## 假设条件

1. **UMP 同意流程配置**: 已在 AdMob 控制台配置了用户消息平台（UMP）同意流程
2. **网络权限**: 应用已声明 `INTERNET` 权限（已在 AndroidManifest.xml 中配置）
3. **AdMob 账户**: 已创建 AdMob 账户并完成应用注册
4. **广告单元**: 已创建插页式广告单元

## QA 验证步骤

### 1. 测试设备配置

1. 在测试设备上安装应用
2. 运行应用并查看 logcat
3. 搜索包含 "Use RequestConfiguration.Builder().setTestDeviceIds" 的日志
4. 复制设备 ID 并配置到 `MainActivity.kt` 中

### 2. 同意流程验证

1. **首次启动应用**:
   - 应该显示 UMP 同意表单
   - 选择"同意"或"拒绝"后，表单应正常关闭

2. **同意后**:
   - 如果用户同意，应该开始加载插页式广告
   - 查看 logcat 确认广告加载成功

3. **隐私选项**:
   - 如果 UMP 配置要求显示隐私选项，应用应提供入口
   - 可以在设置页面添加隐私选项入口（可选）

### 3. 广告显示验证

1. **导航切换测试**:
   - 点击底部导航栏切换页面（For You、Bookmarks、Interests）
   - 如果广告已加载完成，应该显示插页式广告
   - 广告关闭后，应该正常导航到目标页面

2. **广告未准备好**:
   - 如果广告未加载完成，应该正常继续导航流程
   - 不应阻塞用户操作

3. **广告加载失败**:
   - 查看 logcat 中的错误信息
   - 确认错误代码和描述
   - 验证应用不会崩溃

### 4. Ad Inspector 验证

1. **启用 Ad Inspector**:
   - 在测试设备上，打开"设置" > "Google" > "广告"
   - 启用"广告调试"或"Ad Inspector"

2. **验证测试广告**:
   - 运行应用并触发广告
   - 确认显示的是测试广告（标签为"测试广告"）
   - 验证广告可以正常关闭

### 5. 日志验证

查看 logcat 中的以下标签：

- `NiaApplication`: Mobile Ads SDK 初始化日志
- `ConsentManager`: UMP 同意流程日志
- `InterstitialAdManager`: 插页式广告加载和显示日志

**关键日志**:
- `Mobile Ads SDK initialized` - SDK 初始化成功
- `SDK Version: X.X.X` - SDK 版本信息
- `Consent form dismissed. Can request ads: true/false` - 同意状态
- `Interstitial ad loaded successfully` - 广告加载成功
- `Ad showed full screen content` - 广告显示成功
- `Ad dismissed full screen content` - 广告关闭成功

### 6. 生产环境验证

1. **替换真实 ID**:
   - 将测试 App ID 和广告位 ID 替换为真实 ID
   - 确保测试设备 ID 配置正确（仅调试构建）

2. **发布前检查**:
   - 确认同意流程正常工作
   - 确认广告可以正常加载和显示
   - 确认广告关闭后应用功能正常
   - 确认没有内存泄漏（广告引用已正确清空）

3. **性能测试**:
   - 验证广告加载不影响应用性能
   - 验证导航切换流畅度
   - 验证内存使用正常

## 常见问题

### Q: 广告不显示？
A: 
1. 检查是否已获得用户同意（查看 `ConsentManager.canRequestAds`）
2. 检查广告位 ID 是否正确
3. 检查网络连接
4. 查看 logcat 中的错误信息

### Q: 同意表单不显示？
A:
1. 检查 UMP 配置是否正确
2. 检查测试设备 ID 配置（调试模式下）
3. 检查 AdMob 控制台中的 UMP 设置

### Q: 广告重复加载？
A:
- `InterstitialAdManager` 已实现防重复加载逻辑
- 如果仍出现重复加载，检查是否有多个实例

### Q: 如何重置同意状态进行测试？
A:
- 在调试模式下，可以调用 `consentManager.reset()` 重置同意状态
- 注意：这仅在测试时使用

## 参考文档

- [Google AdMob 快速入门](https://developers.google.com/admob/android/quick-start)
- [插页式广告指南](https://developers.google.com/admob/android/interstitial)
- [UMP SDK 文档](https://developers.google.com/admob/android/privacy/overview)

