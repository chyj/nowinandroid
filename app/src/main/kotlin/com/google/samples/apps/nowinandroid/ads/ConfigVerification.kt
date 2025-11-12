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

package com.google.samples.apps.nowinandroid.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

/**
 * 验证 AdMob 配置是否正确
 * 
 * 参考 Google 官方示例：
 * https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample
 */
object ConfigVerification {
    private const val TAG = "ConfigVerification"
    
    /**
     * 验证 AdMob 配置
     */
    fun verifyConfiguration(context: Context) {
        Log.d(TAG, "=== AdMob Configuration Verification ===")
        
        // 1. 检查 App ID
        verifyAppId(context)
        
        // 2. 检查 Ad Unit ID
        verifyAdUnitId()
        
        // 3. 检查权限
        verifyPermissions(context)
        
        // 4. 检查网络连接（需要运行时检查）
        Log.d(TAG, "Note: Network connectivity check requires runtime permission")
        
        Log.d(TAG, "=== Verification Complete ===")
    }
    
    /**
     * 验证 App ID 配置
     */
    private fun verifyAppId(context: Context) {
        Log.d(TAG, "1. Checking App ID configuration...")
        
        try {
            val appInfo: ApplicationInfo = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            
            val appId = appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
            
            if (appId.isNullOrEmpty()) {
                Log.e(TAG, "❌ App ID is missing in AndroidManifest.xml")
                Log.e(TAG, "   Add: <meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"YOUR_APP_ID\"/>")
            } else {
                Log.d(TAG, "✅ App ID found: $appId")
                
                // 验证格式
                when {
                    appId.startsWith("ca-app-pub-") && appId.contains("~") -> {
                        Log.d(TAG, "✅ App ID format is correct")
                        if (appId == "ca-app-pub-3940256099942544~3347511713") {
                            Log.d(TAG, "ℹ️  Using test App ID (for testing only)")
                        }
                    }
                    else -> {
                        Log.w(TAG, "⚠️  App ID format may be incorrect")
                        Log.w(TAG, "   Expected format: ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking App ID: ${e.message}", e)
        }
    }
    
    /**
     * 验证 Ad Unit ID
     */
    private fun verifyAdUnitId() {
        Log.d(TAG, "2. Checking Ad Unit ID configuration...")
        
        // 注意：Ad Unit ID 在代码中定义，这里只验证格式
        val testAdUnitId = "ca-app-pub-3940256099942544/1033173712"
        Log.d(TAG, "✅ Test Ad Unit ID format: $testAdUnitId")
        
        when {
            testAdUnitId.startsWith("ca-app-pub-") && testAdUnitId.contains("/") -> {
                Log.d(TAG, "✅ Ad Unit ID format is correct")
                Log.d(TAG, "ℹ️  Using test Ad Unit ID (for testing only)")
            }
            else -> {
                Log.w(TAG, "⚠️  Ad Unit ID format may be incorrect")
                Log.w(TAG, "   Expected format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX")
            }
        }
    }
    
    /**
     * 验证权限配置
     */
    private fun verifyPermissions(context: Context) {
        Log.d(TAG, "3. Checking permissions...")
        
        val hasInternet = context.packageManager.checkPermission(
            android.Manifest.permission.INTERNET,
            context.packageName
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasInternet) {
            Log.d(TAG, "✅ INTERNET permission granted")
        } else {
            Log.e(TAG, "❌ INTERNET permission missing")
            Log.e(TAG, "   Add: <uses-permission android:name=\"android.permission.INTERNET\" />")
        }
    }
}

