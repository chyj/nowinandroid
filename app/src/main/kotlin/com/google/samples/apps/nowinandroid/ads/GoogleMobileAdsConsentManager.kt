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

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理 Google Mobile Ads 同意流程（UMP - User Messaging Platform）
 * 
 * 参考 Google 官方示例：
 * https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample
 * 
 * 根据 Google 官方指南，在加载任何广告之前必须先完成 UMP 同意流程。
 * 只有在用户同意后，才能加载和显示广告。
 */
@Singleton
class GoogleMobileAdsConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /**
     * 是否已经获得用户同意（可以加载广告）
     */
    val canRequestAds: Boolean
        get() {
            val canRequest = consentInformation.canRequestAds()
            Log.d(TAG, "[LIFECYCLE] canRequestAds check: $canRequest")
            return canRequest
        }

    /**
     * 隐私选项是否可用（用户可以选择修改隐私设置）
     */
    val isPrivacyOptionsRequired: Boolean
        get() {
            val isRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            Log.d(TAG, "[LIFECYCLE] isPrivacyOptionsRequired check: $isRequired")
            return isRequired
        }

    /**
     * 收集同意信息
     * 
     * 参考 Google 官方示例实现
     * 
     * @param activity 当前 Activity，用于显示同意表单
     * @param isDebug 是否为调试模式（用于测试）
     * @param testDeviceId 测试设备 ID（仅在调试模式下使用）
     * @param onConsentFormDismissedListener 同意表单关闭时的回调
     */
    fun collectConsent(
        activity: Activity,
        isDebug: Boolean = false,
        testDeviceId: String? = null,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener? = null,
    ) {
        Log.d(TAG, "[LIFECYCLE] collectConsent() called - isDebug: $isDebug, testDeviceId: ${testDeviceId?.take(10)}...")
        
        // 检查当前同意状态
        val currentConsentStatus = consentInformation.consentStatus
        Log.d(TAG, "[LIFECYCLE] Current consent status: $currentConsentStatus")
        Log.d(TAG, "[LIFECYCLE] Can request ads before update: ${consentInformation.canRequestAds()}")
        
        val request = ConsentRequestParameters.Builder()
            .apply {
                if (isDebug) {
                    // 调试模式：设置调试地理区域以强制显示同意表单
                    // 参考官方示例：https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample
                    val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    
                    // 如果提供了测试设备 ID，则添加
                    if (testDeviceId != null) {
                        Log.d(TAG, "[LIFECYCLE] Setting debug settings with test device ID")
                        debugSettingsBuilder.addTestDeviceHashedId(testDeviceId)
                    } else {
                        Log.d(TAG, "[LIFECYCLE] Setting debug settings without test device ID (will use default test device)")
                    }
                    
                    val debugSettings = debugSettingsBuilder.build()
                    setConsentDebugSettings(debugSettings)
                    Log.d(TAG, "[LIFECYCLE] Debug geography set to EEA (will force consent form)")
                }
            }
            .build()

        Log.d(TAG, "[LIFECYCLE] Requesting consent info update...")
        consentInformation.requestConsentInfoUpdate(
            activity,
            request,
            {
                // 同意信息更新成功
                val canRequestAds = consentInformation.canRequestAds()
                val formAvailable = consentInformation.isConsentFormAvailable
                val newConsentStatus = consentInformation.consentStatus
                
                Log.d(TAG, "[LIFECYCLE] Consent info updated successfully")
                Log.d(TAG, "[LIFECYCLE] - Can request ads: $canRequestAds")
                Log.d(TAG, "[LIFECYCLE] - Form available: $formAvailable")
                Log.d(TAG, "[LIFECYCLE] - Consent status: $newConsentStatus")
                
                if (formAvailable) {
                    Log.d(TAG, "[LIFECYCLE] Loading and showing consent form...")
                    loadAndShowConsentForm(
                        activity,
                        onConsentFormDismissedListener,
                    )
                } else {
                    Log.d(TAG, "[LIFECYCLE] Consent form not available")
                    // 如果表单不可用但可以请求广告，直接触发回调加载广告
                    if (canRequestAds) {
                        Log.d(TAG, "[LIFECYCLE] Can request ads without form, triggering callback")
                        onConsentFormDismissedListener?.onConsentFormDismissed(null)
                    } else {
                        Log.w(TAG, "[LIFECYCLE] Cannot request ads and form not available")
                    }
                }
            },
            { formError ->
                // 同意信息更新失败
                Log.e(TAG, "[LIFECYCLE] ❌ Error requesting consent info update")
                Log.e(TAG, "[LIFECYCLE] - Error message: ${formError.message}")
                Log.e(TAG, "[LIFECYCLE] - Error code: ${formError.errorCode}")
                
                // 错误代码说明：
                // 1 = INTERNAL_ERROR
                // 2 = NETWORK_ERROR  
                // 3 = INVALID_OPERATION
                when (formError.errorCode) {
                    1 -> Log.e(TAG, "[LIFECYCLE] Error type: INTERNAL_ERROR - Check UMP configuration in AdMob console")
                    2 -> Log.e(TAG, "[LIFECYCLE] Error type: NETWORK_ERROR - Check internet connection")
                    3 -> Log.e(TAG, "[LIFECYCLE] Error type: INVALID_OPERATION - Check UMP setup")
                    else -> Log.e(TAG, "[LIFECYCLE] Error type: UNKNOWN")
                }
                
                // 即使同意流程失败，如果之前已经获得同意，仍然可以尝试加载广告
                val canRequestAds = consentInformation.canRequestAds()
                Log.d(TAG, "[LIFECYCLE] Can request ads despite error: $canRequestAds")
                if (canRequestAds && onConsentFormDismissedListener != null) {
                    Log.d(TAG, "[LIFECYCLE] Triggering callback despite error (user may have previously consented)")
                    onConsentFormDismissedListener.onConsentFormDismissed(null)
                }
            },
        )
    }

    /**
     * 加载并显示同意表单
     */
    private fun loadAndShowConsentForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener?,
    ) {
        Log.d(TAG, "[LIFECYCLE] loadAndShowConsentForm() called")
        UserMessagingPlatform.loadConsentForm(
            activity,
            { consentForm ->
                Log.d(TAG, "[LIFECYCLE] Consent form loaded successfully, showing form...")
                consentForm.show(
                    activity,
                    onConsentFormDismissedListener ?: ConsentForm.OnConsentFormDismissedListener { formError ->
                        // 表单关闭时的默认处理
                        if (formError != null) {
                            Log.e(TAG, "[LIFECYCLE] Error showing consent form")
                            Log.e(TAG, "[LIFECYCLE] - Error message: ${formError.message}")
                            Log.e(TAG, "[LIFECYCLE] - Error code: ${formError.errorCode}")
                        } else {
                            val finalCanRequestAds = canRequestAds
                            val finalConsentStatus = consentInformation.consentStatus
                            Log.d(TAG, "[LIFECYCLE] Consent form dismissed successfully")
                            Log.d(TAG, "[LIFECYCLE] - Can request ads: $finalCanRequestAds")
                            Log.d(TAG, "[LIFECYCLE] - Final consent status: $finalConsentStatus")
                        }
                    },
                )
            },
            { formError ->
                Log.e(TAG, "[LIFECYCLE] Error loading consent form")
                Log.e(TAG, "[LIFECYCLE] - Error message: ${formError.message}")
                Log.e(TAG, "[LIFECYCLE] - Error code: ${formError.errorCode}")
            },
        )
    }

    /**
     * 显示隐私选项表单（用于用户修改隐私设置）
     * 
     * @param activity 当前 Activity
     * @param onConsentFormDismissedListener 表单关闭时的回调
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener? = null,
    ) {
        Log.d(TAG, "[LIFECYCLE] showPrivacyOptionsForm() called")
        Log.d(TAG, "[LIFECYCLE] - Privacy options required: $isPrivacyOptionsRequired")
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            onConsentFormDismissedListener ?: ConsentForm.OnConsentFormDismissedListener { formError ->
                if (formError != null) {
                    Log.e(TAG, "[LIFECYCLE] Error showing privacy options form")
                    Log.e(TAG, "[LIFECYCLE] - Error message: ${formError.message}")
                    Log.e(TAG, "[LIFECYCLE] - Error code: ${formError.errorCode}")
                } else {
                    val finalCanRequestAds = canRequestAds
                    Log.d(TAG, "[LIFECYCLE] Privacy options form dismissed")
                    Log.d(TAG, "[LIFECYCLE] - Can request ads: $finalCanRequestAds")
                }
            },
        )
    }

    /**
     * 重置同意状态（主要用于测试）
     */
    fun reset() {
        Log.d(TAG, "[LIFECYCLE] reset() called - resetting consent information")
        consentInformation.reset()
        Log.d(TAG, "[LIFECYCLE] Consent information reset completed")
    }

    companion object {
        private const val TAG = "ConsentManager"
    }
}

