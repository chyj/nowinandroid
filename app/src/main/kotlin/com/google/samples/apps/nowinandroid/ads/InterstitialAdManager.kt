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
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理插页式广告的加载和显示
 * 
 * 参考 Google 官方示例：
 * https://github.com/googleads/googleads-mobile-android-examples/tree/main/kotlin/admob/InterstitialExample
 * 
 * 根据 Google 官方指南：
 * - 使用 InterstitialAd.load 加载广告
 * - 避免重复加载
 * - 妥善处理所有回调
 * - 在 FullScreenContentCallback 的所有路径中清空广告引用
 * - 如果广告未准备好则恢复正常流程
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    private val consentManager: GoogleMobileAdsConsentManager,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var pendingShowRequest: Activity? = null // 记录待显示的请求

    /**
     * 插页式广告位 ID
     * 
     * TODO: 替换为您的实际广告位 ID
     * 测试广告位 ID: ca-app-pub-3940256099942544/1033173712
     */
    private val adUnitId = "ca-app-pub-3940256099942544/1033173712"
    
    init {
        Log.d(TAG, "[LIFECYCLE] InterstitialAdManager initialized")
        Log.d(TAG, "[LIFECYCLE] - Ad Unit ID: $adUnitId")
    }

    /**
     * 加载插页式广告
     * 
     * 只有在用户同意后才会加载广告。如果正在加载或已加载，则不会重复加载。
     * 
     * @param activity 当前 Activity（用于检查同意状态）
     */
    fun loadAd(activity: Activity) {
        Log.d(TAG, "[LIFECYCLE] loadAd() called")
        
        // 检查是否已获得用户同意
        val canRequestAds = consentManager.canRequestAds
        Log.d(TAG, "[LIFECYCLE] - Can request ads: $canRequestAds")
        if (!canRequestAds) {
            Log.w(TAG, "[LIFECYCLE] Cannot load ad: user consent not obtained")
            return
        }

        // 检查 MobileAds SDK 是否已初始化
        // 注意：MobileAds.initialize() 是异步的，但通常很快完成
        // 根据官方文档，getVersion() 总是返回非空字符串，所以这里主要用于日志记录
        try {
            val sdkVersion = MobileAds.getVersion()
            Log.d(TAG, "[LIFECYCLE] MobileAds SDK version: $sdkVersion")
        } catch (e: Exception) {
            Log.w(TAG, "[LIFECYCLE] ⚠️ MobileAds SDK may not be fully initialized yet: ${e.message}")
            Log.w(TAG, "[LIFECYCLE] Waiting a moment and retrying...")
            // 延迟重试（简单处理，实际可以使用回调机制）
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                loadAd(activity)
            }, 500)
            return
        }

        // 避免重复加载
        Log.d(TAG, "[LIFECYCLE] - Current state: isLoading=$isLoadingAd, adLoaded=${interstitialAd != null}")
        if (isLoadingAd || interstitialAd != null) {
            Log.d(TAG, "[LIFECYCLE] Ad already loaded or loading, skipping load request")
            return
        }

        Log.d(TAG, "[LIFECYCLE] Starting ad load request...")
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "[LIFECYCLE] AdRequest created, calling InterstitialAd.load()")

        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "[LIFECYCLE] ✅ onAdLoaded() - Ad loaded successfully")
                    interstitialAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "[LIFECYCLE] - Ad state updated: isLoading=false, adLoaded=true")

                    // 设置全屏内容回调，确保在所有路径中清空广告引用
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "[LIFECYCLE] 📱 onAdDismissedFullScreenContent() - User dismissed ad")
                            interstitialAd = null
                            isLoadingAd = false
                            pendingShowRequest = null
                            Log.d(TAG, "[LIFECYCLE] - Ad state cleared, preparing to load next ad")
                            // 广告关闭后，预加载下一个广告
                            loadAd(activity)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "[LIFECYCLE] ❌ onAdFailedToShowFullScreenContent()")
                            Log.e(TAG, "[LIFECYCLE] - Error message: ${adError.message}")
                            Log.e(TAG, "[LIFECYCLE] - Error code: ${adError.code}")
                            Log.e(TAG, "[LIFECYCLE] - Error domain: ${adError.domain}")
                            interstitialAd = null
                            isLoadingAd = false
                            Log.d(TAG, "[LIFECYCLE] - Ad state cleared after show failure")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "[LIFECYCLE] 👁️ onAdShowedFullScreenContent() - Ad is now visible to user")
                            Log.d(TAG, "[LIFECYCLE] Note: Test ads may appear as black screen - this is normal")
                            Log.d(TAG, "[LIFECYCLE] Note: Real ads will show actual content")
                            // 注意：不要在这里清空引用，应该在 onAdDismissedFullScreenContent 中清空
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "[LIFECYCLE] 📊 onAdImpression() - Ad impression recorded")
                            Log.d(TAG, "[LIFECYCLE] Ad has been successfully displayed and recorded")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "[LIFECYCLE] 👆 onAdClicked() - User clicked on ad")
                        }
                    }
                    Log.d(TAG, "[LIFECYCLE] FullScreenContentCallback configured")
                    
                    // 如果之前有待显示的请求，现在广告已加载，尝试显示
                    pendingShowRequest?.let { pendingActivity ->
                        Log.d(TAG, "[LIFECYCLE] Found pending show request, attempting to show ad now...")
                        pendingShowRequest = null
                        showAd(pendingActivity)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "[LIFECYCLE] ❌ onAdFailedToLoad() - Ad load failed")
                    Log.e(TAG, "[LIFECYCLE] - Error message: ${loadAdError.message}")
                    Log.e(TAG, "[LIFECYCLE] - Error code: ${loadAdError.code}")
                    Log.e(TAG, "[LIFECYCLE] - Error domain: ${loadAdError.domain}")
                    Log.e(TAG, "[LIFECYCLE] - Response info: ${loadAdError.responseInfo}")
                    
                    // 常见错误代码说明
                    when (loadAdError.code) {
                        0 -> Log.e(TAG, "[LIFECYCLE] Error type: ERROR_CODE_INTERNAL_ERROR - Check AdMob configuration")
                        1 -> Log.e(TAG, "[LIFECYCLE] Error type: ERROR_CODE_INVALID_REQUEST - Check ad unit ID")
                        2 -> Log.e(TAG, "[LIFECYCLE] Error type: ERROR_CODE_NETWORK_ERROR - Check internet connection")
                        3 -> Log.e(TAG, "[LIFECYCLE] Error type: ERROR_CODE_NO_FILL - No ad available")
                        8 -> Log.e(TAG, "[LIFECYCLE] Error type: ERROR_CODE_INVALID_AD_SIZE - Invalid ad size")
                        else -> Log.e(TAG, "[LIFECYCLE] Error type: UNKNOWN (code: ${loadAdError.code})")
                    }
                    
                    // 如果是网络错误或内部错误，可能是配置问题
                    if (loadAdError.code == 0 || loadAdError.code == 2) {
                        Log.w(TAG, "[LIFECYCLE] ⚠️ Possible issues:")
                        Log.w(TAG, "[LIFECYCLE] 1. Check AdMob App ID in AndroidManifest.xml")
                        Log.w(TAG, "[LIFECYCLE] 2. Verify ad unit ID is correct")
                        Log.w(TAG, "[LIFECYCLE] 3. Check internet connection")
                        Log.w(TAG, "[LIFECYCLE] 4. Verify AdMob account is set up correctly")
                        Log.w(TAG, "[LIFECYCLE] 5. For test ads, ensure using test ad unit IDs")
                    }
                    
                    interstitialAd = null
                    isLoadingAd = false
                    pendingShowRequest = null // 清除待显示请求
                    Log.d(TAG, "[LIFECYCLE] - Ad state cleared after load failure")
                }
            },
        )
    }

    /**
     * 显示插页式广告
     * 
     * @param activity 当前 Activity
     * @return true 如果广告已显示，false 如果广告未准备好
     */
    fun showAd(activity: Activity): Boolean {
        Log.d(TAG, "[LIFECYCLE] showAd() called")
        val ad = interstitialAd
        val canRequestAds = consentManager.canRequestAds
        
        Log.d(TAG, "[LIFECYCLE] - Ad state: adLoaded=${ad != null}, isLoading=$isLoadingAd, canRequestAds=$canRequestAds")
        
        return if (ad != null) {
            Log.d(TAG, "[LIFECYCLE] ✅ Ad is ready, showing ad...")
            try {
                ad.show(activity)
                Log.d(TAG, "[LIFECYCLE] ✅ ad.show() called successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "[LIFECYCLE] ❌ Exception while showing ad: ${e.message}", e)
                false
            }
        } else {
            Log.d(TAG, "[LIFECYCLE] ⚠️ Ad not ready")
            // 如果广告未准备好且未在加载中，尝试加载新广告
            if (!isLoadingAd && canRequestAds) {
                Log.d(TAG, "[LIFECYCLE] Attempting to load new ad...")
                loadAd(activity)
            } else if (isLoadingAd) {
                Log.d(TAG, "[LIFECYCLE] Ad is currently loading, saving show request for when ad loads...")
                // 保存待显示的请求，等广告加载完成后自动显示
                pendingShowRequest = activity
            } else if (!canRequestAds) {
                Log.w(TAG, "[LIFECYCLE] Cannot load ad: user consent not obtained")
            }
            false
        }
    }

    /**
     * 检查广告是否已准备好显示
     */
    fun isAdReady(): Boolean {
        val ready = interstitialAd != null
        Log.d(TAG, "[LIFECYCLE] isAdReady() check: $ready")
        return ready
    }

    /**
     * 清空当前广告引用（用于测试或重置）
     */
    fun clearAd() {
        Log.d(TAG, "[LIFECYCLE] clearAd() called - clearing ad state")
        interstitialAd = null
        isLoadingAd = false
        pendingShowRequest = null
        Log.d(TAG, "[LIFECYCLE] Ad state cleared")
    }

    companion object {
        private const val TAG = "InterstitialAdManager"
    }
}

