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
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

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
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // 自适应横幅广告高度会根据内容调整
    ) {
        if (canRequestAds) {
            // 使用 remember 保存 AdView 引用，确保只创建一次
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
                        }
                        
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.e(TAG, "========== 广告加载失败 ==========")
                            Log.e(TAG, "AdUnitId: ${this@apply.adUnitId}")
                            Log.e(TAG, "Error Code: ${loadAdError.code}")
                            Log.e(TAG, "Error Domain: ${loadAdError.domain}")
                            Log.e(TAG, "Error Message: ${loadAdError.message}")
                            Log.e(TAG, "Response Info: ${loadAdError.responseInfo}")
                            val cause = loadAdError.cause
                            if (cause != null) {
                                Log.e(TAG, "Cause: $cause")
                                if (cause is Throwable) {
                                    Log.e(TAG, "StackTrace: ${Log.getStackTraceString(cause)}")
                                }
                            }
                        }
                        
                        override fun onAdOpened() {
                            super.onAdOpened()
                            Log.d(TAG, "========== 广告打开 ==========")
                            Log.d(TAG, "AdUnitId: ${this@apply.adUnitId}")
                        }
                        
                        override fun onAdClosed() {
                            super.onAdClosed()
                            Log.d(TAG, "========== 广告关闭 ==========")
                            Log.d(TAG, "AdUnitId: ${this@apply.adUnitId}")
                        }
                        
                        override fun onAdClicked() {
                            super.onAdClicked()
                            Log.d(TAG, "========== 广告被点击 ==========")
                            Log.d(TAG, "AdUnitId: ${this@apply.adUnitId}")
                        }
                        
                        override fun onAdImpression() {
                            super.onAdImpression()
                            Log.d(TAG, "========== 广告展示 ==========")
                            Log.d(TAG, "AdUnitId: ${this@apply.adUnitId}")
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
            LaunchedEffect(adView) {
                // 延迟一小段时间确保 AdView 完全初始化
                delay(100)
                Log.d(TAG, "========== 构建 AdRequest ==========")
                // 构建测试广告请求
                // 注意：使用测试广告单元 ID 时，AdMob 会自动返回测试广告
                val adRequest = AdRequest.Builder()
                    .build()
                Log.d(TAG, "AdRequest 构建完成")
                Log.d(TAG, "开始加载广告...")
                Log.d(TAG, "调用 loadAd()...")
                adView.loadAd(adRequest)
                Log.d(TAG, "loadAd() 调用完成（异步加载中）")
            }
            
            // 在组件销毁时清理 AdView 资源
            DisposableEffect(adView) {
                Log.d(TAG, "========== DisposableEffect 安装 ==========")
                Log.d(TAG, "AdView 已添加到 Compose 树中")
                onDispose {
                    Log.d(TAG, "========== DisposableEffect 清理 ==========")
                    Log.d(TAG, "开始销毁 AdView...")
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

