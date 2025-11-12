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

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 原生广告管理器，负责加载和管理原生广告。
 * 使用协程进行异步加载，支持缓存和资源管理。
 */
@Singleton
class NativeAdManager @Inject constructor() {
    /**
     * Logcat 过滤关键字：NativeAdManager
     * 使用方式：在 Android Studio Logcat 中输入 "NativeAdManager" 查看所有广告管理器相关日志
     */
    private val TAG = "NativeAdManager"

    // 测试广告单元 ID（开发阶段使用）
    // 发布前需要替换为实际的广告单元 ID
    private val testAdUnitId = "ca-app-pub-3940256099942544/2247696110"

    private var cachedNativeAd: NativeAd? = null
    private var isInitialized = false

    /**
     * 初始化 AdMob SDK
     * 
     * 生命周期：应用启动时调用一次
     * Logcat 关键字：NativeAdManager
     */
    suspend fun initialize(context: Context) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "🚀 [广告生命周期] 开始初始化 AdMob SDK")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        
        if (isInitialized) {
            Log.d(TAG, "⚠️ [广告生命周期] AdMob SDK 已经初始化，跳过")
            return
        }

        return suspendCancellableCoroutine { continuation ->
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "✅ [广告生命周期] AdMob SDK 初始化完成")
                val statusMap = initializationStatus.adapterStatusMap
                Log.d(TAG, "📊 [广告生命周期] 适配器状态:")
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    val state = status?.initializationState?.name ?: "UNKNOWN"
                    Log.d(TAG, "   - $adapterClass: $state")
                }
                isInitialized = true
                Log.d(TAG, "═══════════════════════════════════════════════════════════")
                continuation.resume(Unit)
            }
        }
    }

    /**
     * 加载原生广告
     * 
     * 生命周期：在需要显示广告时调用
     * Logcat 关键字：NativeAdManager
     */
    suspend fun loadNativeAd(context: Context): Result<NativeAd> {
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "📥 [广告生命周期] 开始加载原生广告")
        Log.d(TAG, "   广告单元 ID: $testAdUnitId")
        
        if (!isInitialized) {
            Log.d(TAG, "⚠️ [广告生命周期] SDK 未初始化，先初始化 SDK")
            initialize(context)
        }

        // 如果已有缓存的广告，先销毁
        if (cachedNativeAd != null) {
            Log.d(TAG, "🗑️ [广告生命周期] 销毁旧广告缓存")
            cachedNativeAd?.destroy()
            cachedNativeAd = null
        }

        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "⏳ [广告生命周期] 创建 AdLoader，开始请求广告...")
            val adLoader = AdLoader.Builder(context, testAdUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "✅ [广告生命周期] 原生广告加载成功！")
                    Log.d(TAG, "   📝 标题: ${nativeAd.headline}")
                    Log.d(TAG, "   📄 正文: ${nativeAd.body?.take(50)}...")
                    Log.d(TAG, "   🎯 CTA: ${nativeAd.callToAction}")
                    Log.d(TAG, "   🏢 广告主: ${nativeAd.advertiser}")
                    cachedNativeAd = nativeAd
                    Log.d(TAG, "💾 [广告生命周期] 广告已缓存")
                    Log.d(TAG, "═══════════════════════════════════════════════════════════")
                    continuation.resume(Result.success(nativeAd))
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ [广告生命周期] 原生广告加载失败！")
                        Log.e(TAG, "   错误代码: ${error.code}")
                        Log.e(TAG, "   错误信息: ${error.message}")
                        Log.e(TAG, "   错误域: ${error.domain}")
                        Log.e(TAG, "   错误原因: ${error.cause}")
                        Log.d(TAG, "═══════════════════════════════════════════════════════════")
                        continuation.resume(Result.failure(Exception(error.message)))
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "👆 [广告生命周期] 用户点击了广告")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ [广告生命周期] 广告展示已记录（Impression）")
                    }
                    
                    override fun onAdOpened() {
                        Log.d(TAG, "🔓 [广告生命周期] 广告已打开")
                    }
                    
                    override fun onAdClosed() {
                        Log.d(TAG, "🔒 [广告生命周期] 广告已关闭")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setVideoOptions(
                            VideoOptions.Builder()
                                .setStartMuted(true)
                                .build(),
                        )
                        .setRequestMultipleImages(false)
                        .build(),
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * 获取缓存的广告（如果存在）
     */
    fun getCachedAd(): NativeAd? {
        val hasCache = cachedNativeAd != null
        Log.d(TAG, "📦 [广告生命周期] 获取缓存广告: ${if (hasCache) "存在" else "不存在"}")
        return cachedNativeAd
    }

    /**
     * 销毁缓存的广告
     * 
     * 生命周期：在不再需要广告时调用（如 Activity/Fragment 销毁）
     * Logcat 关键字：NativeAdManager
     */
    fun destroyCachedAd() {
        if (cachedNativeAd != null) {
            Log.d(TAG, "🗑️ [广告生命周期] 销毁缓存的广告")
            cachedNativeAd?.destroy()
            cachedNativeAd = null
            Log.d(TAG, "✅ [广告生命周期] 广告已销毁")
        } else {
            Log.d(TAG, "ℹ️ [广告生命周期] 没有需要销毁的广告")
        }
    }

    /**
     * 检查是否有缓存的广告
     */
    fun hasCachedAd(): Boolean {
        val hasCache = cachedNativeAd != null
        Log.d(TAG, "🔍 [广告生命周期] 检查缓存: ${if (hasCache) "有缓存" else "无缓存"}")
        return hasCache
    }
}

