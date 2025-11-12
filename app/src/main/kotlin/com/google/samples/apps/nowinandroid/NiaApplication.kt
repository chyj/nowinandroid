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

package com.google.samples.apps.nowinandroid

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.samples.apps.nowinandroid.sync.initializers.Sync
import com.google.samples.apps.nowinandroid.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [Application] class for NiA
 */
@HiltAndroidApp
class NiaApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    override fun onCreate() {
        super.onCreate()

        setStrictModePolicy()

        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
        profileVerifierLogger()

        // Initialize Mobile Ads SDK (only once per app session)
        initializeMobileAds()
        
        // Verify AdMob configuration
        verifyAdMobConfiguration()
    }
    
    /**
     * 验证 AdMob 配置
     */
    private fun verifyAdMobConfiguration() {
        com.google.samples.apps.nowinandroid.ads.ConfigVerification.verifyConfiguration(this)
    }

    /**
     * 初始化 Google Mobile Ads SDK
     * 
     * 根据 Google 官方指南，每次应用会话只应初始化一次。
     * SDK 版本会在初始化完成后记录到日志中。
     * 
     * 注意：初始化是异步的，但根据官方示例，应该在 Application 中尽早初始化。
     */
    private fun initializeMobileAds() {
        Log.d(TAG, "[LIFECYCLE] Starting MobileAds initialization...")
        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
            // 记录 SDK 版本和适配器状态
            val adapterStatusMap = initializationStatus.adapterStatusMap
            Log.d(TAG, "[LIFECYCLE] ✅ Mobile Ads SDK initialized successfully")
            Log.d(TAG, "[LIFECYCLE] SDK Version: ${MobileAds.getVersion()}")
            
            adapterStatusMap.forEach { (adapter, status) ->
                Log.d(TAG, "[LIFECYCLE] Adapter: $adapter, Status: ${status.initializationState}, " +
                    "Description: ${status.description}")
            }

            // TODO: 如果需要配置测试设备 ID，可以在这里添加
            // MobileAds.setRequestConfiguration(
            //     RequestConfiguration.Builder()
            //         .setTestDeviceIds(listOf("YOUR_TEST_DEVICE_ID"))
            //         .build()
            // )
        }
    }

    override fun newImageLoader(): ImageLoader = imageLoader.get()

    /**
     * Return true if the application is debuggable.
     */
    private fun isDebuggable(): Boolean {
        return 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    /**
     * Set a thread policy that detects all potential problems on the main thread, such as network
     * and disk access.
     *
     * If a problem is found, the offending call will be logged and the application will be killed.
     */
    private fun setStrictModePolicy() {
        if (isDebuggable()) {
            StrictMode.setThreadPolicy(
                Builder().detectAll().penaltyLog().build(),
            )
        }
    }

    companion object {
        private const val TAG = "NiaApplication"
    }
}
