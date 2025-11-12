/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.google.samples.apps.nowinandroid.NiaBuildType

plugins {
    alias(libs.plugins.nowinandroid.android.application)
    alias(libs.plugins.nowinandroid.android.application.compose)
    alias(libs.plugins.nowinandroid.android.application.flavors)
    alias(libs.plugins.nowinandroid.android.application.jacoco)
    alias(libs.plugins.nowinandroid.android.application.firebase)
    alias(libs.plugins.nowinandroid.hilt)
    alias(libs.plugins.google.osslicenses)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        applicationId = "com.google.samples.apps.nowinandroid"
        versionCode = 8
        versionName = "0.1.2" // X.Y.Z; X = Major, Y = minor, Z = Patch level

        // Custom test runner to set up Hilt dependency graph
        testInstrumentationRunner = "com.google.samples.apps.nowinandroid.core.testing.NiaTestRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = NiaBuildType.DEBUG.applicationIdSuffix
        }
        release {
            isMinifyEnabled = providers.gradleProperty("minifyWithR8")
                .map(String::toBooleanStrict).getOrElse(true)
            applicationIdSuffix = NiaBuildType.RELEASE.applicationIdSuffix
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro")

            // To publish on the Play store a private signing key is required, but to allow anyone
            // who clones the code to sign and run the release variant, use the debug signing key.
            // TODO: Abstract the signing configuration to a separate file to avoid hardcoding this.
            signingConfig = signingConfigs.named("debug").get()
            // Ensure Baseline Profile is fresh for release builds.
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    namespace = "com.google.samples.apps.nowinandroid"
}

dependencies {
    implementation(projects.feature.interests)
    implementation(projects.feature.foryou)
    implementation(projects.feature.bookmarks)
    implementation(projects.feature.topic)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
    implementation(projects.sync.work)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.window.core)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(projects.uiTestHiltManifest)

    kspTest(libs.hilt.compiler)

    testImplementation(projects.core.dataTest)
    testImplementation(projects.core.datastoreTest)
    testImplementation(libs.hilt.android.testing)
    testImplementation(projects.sync.syncTest)
    testImplementation(libs.kotlin.test)

    testDemoImplementation(libs.androidx.navigation.testing)
    testDemoImplementation(libs.robolectric)
    testDemoImplementation(libs.roborazzi)
    testDemoImplementation(projects.core.screenshotTesting)
    testDemoImplementation(projects.core.testing)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(projects.core.dataTest)
    androidTestImplementation(projects.core.datastoreTest)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlin.test)

    baselineProfile(projects.benchmarks)
}

baselineProfile {
    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false

    // Make use of Dex Layout Optimizations via Startup Profiles
    dexLayoutOptimization = true
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}

// 为向后兼容性创建任务别名
// 当有多个 flavor 维度时，Gradle 无法自动解析模糊的任务名称
// 因此我们需要显式创建别名，让旧的构建命令仍然可以工作
afterEvaluate {
    // 构建任务别名
    tasks.register("assembleDemoDebug") {
        dependsOn("assembleDemoDefaultDebug")
        group = "build"
        description = "Assembles the DemoDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdDebug") {
        dependsOn("assembleProdDefaultDebug")
        group = "build"
        description = "Assembles the ProdDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleDemoRelease") {
        dependsOn("assembleDemoDefaultRelease")
        group = "build"
        description = "Assembles the DemoDefaultRelease variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdRelease") {
        dependsOn("assembleProdDefaultRelease")
        group = "build"
        description = "Assembles the ProdDefaultRelease variant (backward compatibility alias)"
    }
    
    // 单元测试任务别名
    tasks.register("assembleDemoDebugUnitTest") {
        dependsOn("assembleDemoDefaultDebugUnitTest")
        group = "verification"
        description = "Assembles unit tests for DemoDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdDebugUnitTest") {
        dependsOn("assembleProdDefaultDebugUnitTest")
        group = "verification"
        description = "Assembles unit tests for ProdDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleDemoReleaseUnitTest") {
        dependsOn("assembleDemoDefaultReleaseUnitTest")
        group = "verification"
        description = "Assembles unit tests for DemoDefaultRelease variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdReleaseUnitTest") {
        dependsOn("assembleProdDefaultReleaseUnitTest")
        group = "verification"
        description = "Assembles unit tests for ProdDefaultRelease variant (backward compatibility alias)"
    }
    
    // Android 测试任务别名
    tasks.register("assembleDemoDebugAndroidTest") {
        dependsOn("assembleDemoDefaultDebugAndroidTest")
        group = "verification"
        description = "Assembles Android tests for DemoDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdDebugAndroidTest") {
        dependsOn("assembleProdDefaultDebugAndroidTest")
        group = "verification"
        description = "Assembles Android tests for ProdDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("assembleDemoReleaseAndroidTest") {
        dependsOn("assembleDemoDefaultReleaseAndroidTest")
        group = "verification"
        description = "Assembles Android tests for DemoDefaultRelease variant (backward compatibility alias)"
    }
    
    tasks.register("assembleProdReleaseAndroidTest") {
        dependsOn("assembleProdDefaultReleaseAndroidTest")
        group = "verification"
        description = "Assembles Android tests for ProdDefaultRelease variant (backward compatibility alias)"
    }
    
    // 测试执行任务别名
    tasks.register("testDemoDebugUnitTest") {
        dependsOn("testDemoDefaultDebugUnitTest")
        group = "verification"
        description = "Runs unit tests for DemoDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("testProdDebugUnitTest") {
        dependsOn("testProdDefaultDebugUnitTest")
        group = "verification"
        description = "Runs unit tests for ProdDefaultDebug variant (backward compatibility alias)"
    }
    
    tasks.register("testDemoReleaseUnitTest") {
        dependsOn("testDemoDefaultReleaseUnitTest")
        group = "verification"
        description = "Runs unit tests for DemoDefaultRelease variant (backward compatibility alias)"
    }
    
    tasks.register("testProdReleaseUnitTest") {
        dependsOn("testProdDefaultReleaseUnitTest")
        group = "verification"
        description = "Runs unit tests for ProdDefaultRelease variant (backward compatibility alias)"
    }
    
    tasks.register("connectedDemoDebugAndroidTest") {
        dependsOn("connectedDemoDefaultDebugAndroidTest")
        group = "verification"
        description = "Runs Android tests for DemoDefaultDebug variant on connected devices (backward compatibility alias)"
    }
    
    tasks.register("connectedProdDebugAndroidTest") {
        dependsOn("connectedProdDefaultDebugAndroidTest")
        group = "verification"
        description = "Runs Android tests for ProdDefaultDebug variant on connected devices (backward compatibility alias)"
    }
}
