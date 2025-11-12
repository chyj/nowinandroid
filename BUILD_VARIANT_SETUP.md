# 构建变体配置说明

## 📋 变更摘要

已成功添加新的构建变体支持，可以生成 `com.google.samples.apps.nowinandroid.native` 等不同的 APK，同时保持源码包名不变。

## 🔧 修改的文件

### 1. `build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/NiaFlavor.kt`

**变更内容：**

- 添加了新的 flavor 维度 `adType`，用于区分广告类型
- 添加了新的 flavor：
  - `native` - 原生广告变体（applicationIdSuffix: `.native`）

**Diff：**

```kotlin
enum class FlavorDimension {
    contentType,
+   adType  // 新增广告类型维度
}

enum class NiaFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null, val isDefault: Boolean = false) {
    demo(FlavorDimension.contentType, applicationIdSuffix = ".demo", isDefault = true),
    prod(FlavorDimension.contentType),
+   default(FlavorDimension.adType, isDefault = true), // 默认广告类型，不添加后缀
+   native(FlavorDimension.adType, applicationIdSuffix = ".native"),
}
```

### 2. `app/build.gradle.kts`

**变更内容：**

- 添加了任务别名以保持向后兼容性，包括：
  - **构建任务：** `assembleDemoDebug`, `assembleProdDebug`, `assembleDemoRelease`, `assembleProdRelease`
  - **单元测试任务：** `assembleDemoDebugUnitTest`, `assembleProdDebugUnitTest`, `testDemoDebugUnitTest`, `testProdDebugUnitTest` 等
  - **Android 测试任务：** `assembleDemoDebugAndroidTest`, `connectedDemoDebugAndroidTest` 等

**说明：** 当有多个 flavor 维度时，Gradle 无法自动解析模糊的任务名称。通过添加这些别名，旧的构建和测试命令仍然可以正常工作。所有别名都指向对应的 `default` flavor 变体。

### 3. `app-nia-catalog/build.gradle.kts`

**变更内容：**

- 添加了 `adType` 维度的 `missingDimensionStrategy`，指定使用 `default` flavor
- 这解决了 `app-nia-catalog` 依赖库模块时的变体选择模糊问题

**说明：** 由于库模块（如 `core:designsystem`、`core:ui`）现在也有 `adType` flavor 维度，`app-nia-catalog` 需要明确指定使用哪个 `adType` flavor 来解析依赖。

### 4. `app/google-services.json`

**变更内容：**

- 添加了以下新包名的配置（需要在 Firebase Console 中配置）：
  - `com.google.samples.apps.nowinandroid.native`
  - `com.google.samples.apps.nowinandroid.demo.native`
  - `com.google.samples.apps.nowinandroid.native.debug`
  - `com.google.samples.apps.nowinandroid.demo.native.debug`

**说明：** 这些配置支持所有可能的 flavor 组合（demo/prod × native × debug/release）

## 📦 生成的 ApplicationId

使用 flavor 维度组合后，会生成以下 ApplicationId：

| ContentType | AdType  | BuildType | ApplicationId                                            |
| ----------- | ------- | --------- | -------------------------------------------------------- |
| demo        | default | debug     | `com.google.samples.apps.nowinandroid.demo.debug`        |
| demo        | default | release   | `com.google.samples.apps.nowinandroid.demo`              |
| demo        | native  | debug     | `com.google.samples.apps.nowinandroid.demo.native.debug` |
| demo        | native  | release   | `com.google.samples.apps.nowinandroid.demo.native`       |
| prod        | default | debug     | `com.google.samples.apps.nowinandroid.debug`             |
| prod        | default | release   | `com.google.samples.apps.nowinandroid`                   |
| prod        | native  | debug     | `com.google.samples.apps.nowinandroid.native.debug`      |
| prod        | native  | release   | `com.google.samples.apps.nowinandroid.native`            |

## 🚀 构建命令

### 构建默认变体（不指定 adType 时使用 default）

```bash
# 构建 demo + default + debug
# 方式1：使用完整名称
./gradlew assembleDemoDefaultDebug

# 方式2：使用向后兼容的别名（推荐，与之前的行为一致）
./gradlew assembleDemoDebug

# 构建 prod + default + debug
./gradlew assembleProdDefaultDebug
# 或者使用别名
./gradlew assembleProdDebug
```

**注意：** 由于添加了新的 flavor 维度，`assembleDemoDebug` 等任务名称变得模糊。为了保持向后兼容，我们在 `app/build.gradle.kts` 中添加了任务别名，让这些命令仍然可以正常工作。它们会自动指向 `assembleDemoDefaultDebug` 等默认变体。

### 测试命令

```bash
# 运行单元测试（使用别名）
./gradlew testDemoDebugUnitTest      # → testDemoDefaultDebugUnitTest
./gradlew testProdDebugUnitTest      # → testProdDefaultDebugUnitTest

# 运行 Android 测试（使用别名）
./gradlew connectedDemoDebugAndroidTest  # → connectedDemoDefaultDebugAndroidTest
./gradlew connectedProdDebugAndroidTest  # → connectedProdDefaultDebugAndroidTest

# 或者使用完整名称
./gradlew testDemoDefaultDebugUnitTest
./gradlew testDemoNativeDebugUnitTest
```

### 构建 native 变体

```bash
# 构建 demo + native + debug
./gradlew assembleDemoNativeDebug

# 构建 prod + native + debug
./gradlew assembleProdNativeDebug

# 构建 prod + native + release
./gradlew assembleProdNativeRelease
```

### 查看所有可用变体

```bash
./gradlew tasks --all | grep assemble
```

## ⚠️ 重要提醒

### 1. Firebase/AdMob 后台配置

**必须在 Firebase Console 和 AdMob Console 中添加以下包名：**

- `com.google.samples.apps.nowinandroid.native`
- `com.google.samples.apps.nowinandroid.demo.native`
- `com.google.samples.apps.nowinandroid.native.debug`
- `com.google.samples.apps.nowinandroid.demo.native.debug`

### 5. 更新 google-services.json

当前 `google-services.json` 需要：

1. 从 Firebase Console 下载最新的 `google-services.json`
2. 确保包含所有需要的包名配置
3. 替换当前项目中的 `app/google-services.json`

### 6. AdMob App ID 配置

在 `AndroidManifest.xml` 中，当前使用的是测试 App ID。对于不同的变体，如果需要使用不同的 App ID，可以通过 flavor-specific 的 manifest 文件来配置。

## ✅ 验证步骤

1. **同步 Gradle：**

   ```bash
   ./gradlew --refresh-dependencies
   ```

2. **检查可用变体：**

   ```bash
   ./gradlew tasks --all | grep -E "(assemble|native)"
   ```

3. **构建测试：**

   ```bash
   ./gradlew assembleDemoNativeDebug
   ```

4. **验证 ApplicationId：**
   构建完成后，检查生成的 APK 的 ApplicationId：
   ```bash
   # 使用 aapt2 或 apkanalyzer 检查
   aapt dump badging app/build/outputs/apk/demoNative/debug/app-demo-native-debug.apk | grep package
   ```

## 📝 注意事项

1. **源码包名保持不变：** 所有源码文件中的 `package` 声明保持为 `com.google.samples.apps.nowinandroid`，只有最终生成的 APK 的 ApplicationId 会不同。

2. **Flavor 维度组合：** 由于使用了 flavor 维度，必须同时选择 `contentType` 和 `adType` 两个维度的 flavor。默认情况下，如果不指定，Gradle 会选择第一个 flavor（demo 和 default）。

3. **依赖解析：** 如果某个模块（如 `app-nia-catalog`）不直接使用 flavor，但依赖了有 flavor 维度的库模块，需要使用 `missingDimensionStrategy` 来指定默认的 flavor 选择。

4. **资源文件：** 如果需要为不同变体提供不同的资源文件，可以在 `app/src/native/` 目录下创建对应的资源文件。

5. **代码差异：** 如果 native 变体需要不同的代码逻辑，可以使用 `BuildConfig` 或资源文件来区分。

## 🔍 故障排查

如果构建失败，检查：

1. Gradle 版本是否支持 flavor 维度
2. `google-services.json` 是否包含所有需要的包名
3. Firebase/AdMob 后台是否已配置对应的包名
