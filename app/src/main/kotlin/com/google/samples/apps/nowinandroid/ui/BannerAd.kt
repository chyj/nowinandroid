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

package com.google.samples.apps.nowinandroid.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds

private const val TAG = "BannerAd"

/**
 * 横幅广告组件
 * 
 * @param adUnitId AdMob 广告单元 ID，使用测试 ID 或实际 ID
 * @param modifier Modifier 修饰符
 * @param canRequestAds 是否可以请求广告（需要用户同意）
 */
@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    canRequestAds: Boolean = true,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    // 使用测试广告单元 ID（生产环境应使用实际 ID）
    // 根据官方文档：https://developers.google.com/admob/android/banner?hl=zh-cn
    val testAdUnitId = "ca-app-pub-3940256099942544/9214589741" // Google 官方测试横幅广告 ID
    val finalAdUnitId = if (adUnitId.isNotEmpty()) adUnitId else testAdUnitId
    
    // 计算屏幕宽度（dp），用于自适应横幅广告
    val screenWidthDp = configuration.screenWidthDp
    
    // 记录组件创建
    LaunchedEffect(Unit) {
        Log.d(TAG, "========== BannerAd 组件创建 ==========")
        Log.d(TAG, "canRequestAds: $canRequestAds")
        Log.d(TAG, "finalAdUnitId: $finalAdUnitId")
        Log.d(TAG, "screenWidthDp: $screenWidthDp")
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val maxRetries = 8 // 增加重试次数以提高成功率
    val baseRetryDelayMs = 2000L // 基础延迟2秒，使用指数退避
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp) // 自适应横幅广告高度会根据内容调整
    ) {
        if (canRequestAds) {
            // 使用 remember 保存 AdView 和重试计数
            var retryCount by remember { mutableStateOf(0) }
            var isAdLoaded by remember { mutableStateOf(false) }
            
            val adView = remember(finalAdUnitId, screenWidthDp) {
                Log.d(TAG, "========== 创建 AdView ==========")
                Log.d(TAG, "AdUnitId: $finalAdUnitId")
                Log.d(TAG, "screenWidthDp: $screenWidthDp")
                
                AdView(context).apply {
                    // 使用锚定自适应横幅广告（推荐方式）
                    // 根据官方文档：https://developers.google.com/admob/android/banner?hl=zh-cn
                    val adaptiveAdSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        screenWidthDp
                    )
                    setAdSize(adaptiveAdSize)
                    this.adUnitId = finalAdUnitId
                    
                    // 设置广告监听器，记录所有生命周期事件
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            Log.d(TAG, "========== 广告加载成功 ==========")
                            Log.d(TAG, "AdUnitId: ${this@apply.adUnitId}")
                            Log.d(TAG, "AdSize: ${adSize}")
                            // 重置重试计数并标记广告已加载
                            retryCount = 0
                            isAdLoaded = true
                        }
                        
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.e(TAG, "========== 广告加载失败 ==========")
                            Log.e(TAG, "AdUnitId: ${this@apply.adUnitId}")
                            Log.e(TAG, "Error Code: ${loadAdError.code}")
                            Log.e(TAG, "Error Domain: ${loadAdError.domain}")
                            Log.e(TAG, "Error Message: ${loadAdError.message}")
                            
                            // AdMob 错误代码：
                            // 0 = ERROR_CODE_INTERNAL_ERROR - 内部错误，通常不应该重试
                            // 1 = ERROR_CODE_INVALID_REQUEST - 无效请求，通常不应该重试
                            // 2 = ERROR_CODE_NETWORK_ERROR - 网络错误，应该重试
                            // 3 = ERROR_CODE_NO_FILL - 广告库存不足，应该重试
                            // 8 = ERROR_CODE_INVALID_AD_SIZE - 无效广告尺寸，通常不应该重试
                            val currentRetryCount = retryCount
                            // 对于网络错误和库存不足错误，应该重试
                            // 其他错误也可以尝试重试，因为可能是临时问题
                            val isRetryableError = loadAdError.code == 2 || // ERROR_CODE_NETWORK_ERROR
                                    loadAdError.code == 3 || // ERROR_CODE_NO_FILL
                                    (loadAdError.code != 0 && loadAdError.code != 1 && loadAdError.code != 8) // 其他未知错误也尝试重试
                            
                            val shouldRetry = isRetryableError && currentRetryCount < maxRetries
                            
                            if (shouldRetry) {
                                retryCount = currentRetryCount + 1
                                // 使用指数退避策略：延迟时间 = baseDelay * 2^(retryCount-1)
                                // 第1次重试：2秒，第2次：4秒，第3次：8秒，第4次：16秒...
                                val retryDelay = baseRetryDelayMs * (1L shl (currentRetryCount - 1))
                                // 限制最大延迟为30秒
                                val finalDelay = minOf(retryDelay, 30000L)
                                
                                Log.w(TAG, "准备重试加载广告（第 $retryCount 次，最多 $maxRetries 次，延迟 ${finalDelay}ms）")
                                coroutineScope.launch {
                                    delay(finalDelay)
                                    Log.d(TAG, "开始重试加载广告...")
                                    val adRequest = AdRequest.Builder().build()
                                    this@apply.loadAd(adRequest)
                                }
                            } else {
                                Log.e(TAG, "广告加载失败，不再重试（错误代码: ${loadAdError.code}, 重试次数: $currentRetryCount）")
                            }
                        }
                        
                        override fun onAdOpened() {
                            super.onAdOpened()
                            Log.d(TAG, "========== 广告打开 ==========")
                        }
                        
                        override fun onAdClosed() {
                            super.onAdClosed()
                            Log.d(TAG, "========== 广告关闭 ==========")
                        }
                        
                        override fun onAdClicked() {
                            super.onAdClicked()
                            Log.d(TAG, "========== 广告被点击 ==========")
                        }
                        
                        override fun onAdImpression() {
                            super.onAdImpression()
                            Log.d(TAG, "========== 广告展示 ==========")
                        }
                    }
                    
                    // 不在创建时立即加载广告，而是在 AdView 添加到视图后再加载
                    Log.d(TAG, "AdView 配置完成，等待添加到视图后加载广告")
                }
            }
            
            AndroidView(
                factory = { 
                    Log.d(TAG, "========== AndroidView factory 调用 ==========")
                    adView 
                },
                modifier = Modifier.fillMaxWidth(),
            )
            
            // 使用 LaunchedEffect 确保在 AdView 添加到视图后再加载广告
            LaunchedEffect(adView, canRequestAds) {
                if (!canRequestAds) return@LaunchedEffect
                
                // 重置加载状态和重试计数
                isAdLoaded = false
                retryCount = 0
                
                // 确保 AdMob SDK 已初始化
                MobileAds.initialize(context) {}
                
                // 增加延迟时间，确保 AdView 和 SDK 完全初始化
                // 这对于提高首次加载成功率很重要
                delay(500)
                Log.d(TAG, "========== 构建 AdRequest ==========")
                val adRequest = AdRequest.Builder().build()
                Log.d(TAG, "AdRequest 构建完成")
                Log.d(TAG, "使用测试广告单元 ID: $finalAdUnitId")
                Log.d(TAG, "开始加载广告...")
                adView.loadAd(adRequest)
                Log.d(TAG, "loadAd() 调用完成（异步加载中）")
            }
            
            // 根据官方示例，需要正确处理 AdView 的生命周期
            // 在 Activity/Fragment 的 onPause/onResume 中调用 AdView 的 pause() 和 resume()
            DisposableEffect(adView, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            Log.d(TAG, "========== Lifecycle ON_PAUSE ==========")
                            adView.pause()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            Log.d(TAG, "========== Lifecycle ON_RESUME ==========")
                            adView.resume()
                            // 当应用恢复时，如果广告未加载成功且重试次数未达上限，尝试重新加载
                            if (!isAdLoaded && retryCount < maxRetries) {
                                coroutineScope.launch {
                                    delay(500) // 短暂延迟确保视图完全恢复
                                    Log.d(TAG, "应用恢复，尝试重新加载广告...")
                                    val adRequest = AdRequest.Builder().build()
                                    adView.loadAd(adRequest)
                                }
                            }
                        }
                        else -> {}
                    }
                }
                
                lifecycleOwner.lifecycle.addObserver(observer)
                
                onDispose {
                    Log.d(TAG, "========== DisposableEffect 清理 ==========")
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    Log.d(TAG, "开始销毁 AdView...")
                    adView.pause()
                    adView.destroy()
                    Log.d(TAG, "adView.destroy() 完成")
                }
            }
        } else {
            Log.w(TAG, "========== 广告未显示（用户未同意） ==========")
            Log.w(TAG, "canRequestAds: false")
        }
    }
}

