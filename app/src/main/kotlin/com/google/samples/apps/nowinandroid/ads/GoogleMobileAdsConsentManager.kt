/*
 * Copyright 2024 The Android Open Source Project
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

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * 用户消息平台 (UMP) 同意管理器的单例实现。
 * 处理 GDPR 等隐私合规要求。
 */
object GoogleMobileAdsConsentManager {
    /**
     * Logcat 过滤关键字：ConsentManager
     * 使用方式：在 Android Studio Logcat 中输入 "ConsentManager" 查看所有用户同意相关日志
     */
    private const val TAG = "ConsentManager"

    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null

    /**
     * 将 ConsentStatus 转换为可读的字符串
     * 注意：consentStatus 属性返回的是 Int? 类型
     */
    private fun getConsentStatusString(status: Int?): String {
        if (status == null) {
            return "NULL (未初始化)"
        }
        return when (status) {
            ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN (未知)"
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED (需要用户同意)"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED (不需要同意)"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED (已获得同意)"
            else -> "UNKNOWN_STATUS ($status)"
        }
    }

    /**
     * 初始化同意管理器
     * 
     * 生命周期：应用启动时调用
     * Logcat 关键字：ConsentManager
     */
    fun initialize(activity: Activity) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "🔐 [用户同意] 开始初始化用户同意管理器")
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // 开发阶段：设置测试设备（可选）
        // 在生产环境中应移除或注释掉这部分代码
        val isDebug = (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Log.d(TAG, "   调试模式: $isDebug")
        if (isDebug) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("TEST_DEVICE_ID_HASH")
                .build()

            val params = ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()

            consentInformation?.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // 同意信息更新成功
                    Log.d(TAG, "✅ [用户同意] 同意信息更新成功")
                    val status = consentInformation?.consentStatus
                    Log.d(TAG, "   当前同意状态: ${getConsentStatusString(status)}")
                },
                { formError: FormError ->
                    // 同意信息更新失败
                    Log.e(TAG, "❌ [用户同意] 同意信息更新失败: ${formError.message} (code: ${formError.errorCode})")
                },
            )
        } else {
            // 生产环境：使用默认参数
            val params = ConsentRequestParameters.Builder().build()
            consentInformation?.requestConsentInfoUpdate(
                activity,
                params,
                {
                    Log.d(TAG, "✅ [用户同意] 同意信息更新成功")
                    val status = consentInformation?.consentStatus
                    Log.d(TAG, "   当前同意状态: ${getConsentStatusString(status)}")
                },
                { formError: FormError ->
                    Log.e(TAG, "❌ [用户同意] 同意信息更新失败: ${formError.message} (code: ${formError.errorCode})")
                },
            )
        }
    }

    /**
     * 检查是否需要显示同意表单
     */
    fun isConsentFormRequired(): Boolean {
        val required = consentInformation?.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
        Log.d(TAG, "🔍 [用户同意] 检查是否需要显示同意表单: $required")
        return required
    }

    /**
     * 检查是否可以加载广告
     */
    fun canRequestAds(): Boolean {
        val status = consentInformation?.consentStatus
        val canRequest = status == ConsentInformation.ConsentStatus.OBTAINED ||
            status == ConsentInformation.ConsentStatus.NOT_REQUIRED
        Log.d(TAG, "🔍 [用户同意] 检查是否可以加载广告: $canRequest (状态: ${getConsentStatusString(status)})")
        return canRequest
    }

    /**
     * 加载并显示同意表单
     * 
     * 生命周期：在需要获取用户同意时调用
     * Logcat 关键字：ConsentManager
     */
    fun loadAndShowConsentFormIfRequired(activity: Activity) {
        Log.d(TAG, "📋 [用户同意] 检查是否需要加载同意表单")
        if (isConsentFormRequired()) {
            Log.d(TAG, "⏳ [用户同意] 开始加载同意表单...")
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form: ConsentForm ->
                    Log.d(TAG, "✅ [用户同意] 同意表单加载成功")
                    consentForm = form
                    if (consentInformation?.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        Log.d(TAG, "📱 [用户同意] 显示同意表单")
                        form.show(activity) {
                            // 表单关闭后的回调
                            val newStatus = consentInformation?.consentStatus
                            Log.d(TAG, "🔒 [用户同意] 同意表单已关闭，新状态: ${getConsentStatusString(newStatus)}")
                        }
                    }
                },
                { formError: FormError ->
                    Log.e(TAG, "❌ [用户同意] 同意表单加载失败: ${formError.message} (code: ${formError.errorCode})")
                },
            )
        } else {
            Log.d(TAG, "ℹ️ [用户同意] 不需要显示同意表单")
        }
    }

    /**
     * 重置同意状态（用于测试）
     */
    fun resetConsent() {
        Log.d(TAG, "🔄 [用户同意] 重置同意状态（测试用）")
        consentInformation?.reset()
        Log.d(TAG, "✅ [用户同意] 同意状态已重置")
    }
}

