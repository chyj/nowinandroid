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

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Compose 组件，用于显示原生广告。
 * 使用 Material 3 设计系统，样式与 NewsResourceCardExpanded 保持一致。
 * 
 * Logcat 关键字：NativeAdCard
 */
@Composable
fun NativeAdCard(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier,
    onAdDestroyed: () -> Unit = {},
) {
    /**
     * Logcat 过滤关键字：NativeAdCard
     * 使用方式：在 Android Studio Logcat 中输入 "NativeAdCard" 查看所有广告卡片相关日志
     */
    val TAG = "NativeAdCard"
    
    if (nativeAd == null) {
        android.util.Log.d(TAG, "⚠️ [广告UI] 广告为空，不显示广告卡片")
        return
    }
    
    android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")
    android.util.Log.d(TAG, "🎨 [广告UI] 开始渲染广告卡片")
    android.util.Log.d(TAG, "   标题: ${nativeAd.headline}")
    android.util.Log.d(TAG, "   广告主: ${nativeAd.advertiser}")

    val context = LocalContext.current
    
    // 创建隐藏的 NativeAdView 用于 AdMob 跟踪
    val nativeAdView = remember {
        NativeAdView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    // 创建隐藏的 View 用于 AdMob 点击跟踪
    val headlineView = remember { TextView(context) }
    val bodyView = remember { TextView(context) }
    val callToActionView = remember { Button(context) }
    val iconView = remember { ImageView(context) }
    val advertiserView = remember { TextView(context) }
    val starRatingView = remember { RatingBar(context) }
    val priceView = remember { TextView(context) }
    val storeView = remember { TextView(context) }
    val mediaView = remember { MediaView(context) }

    Card(
        onClick = { 
            android.util.Log.d(TAG, "👆 [广告UI] 用户点击了广告卡片")
            // 触发 AdMob 的点击跟踪
            callToActionView.performClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Column {
            // 媒体视图（图片/视频）- 使用 AndroidView 包装 MediaView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                AndroidView(
                    factory = { mediaView },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 标题和应用图标行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // 应用图标（可选）
                        nativeAd.icon?.let { icon ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                            ) {
                                AsyncImage(
                                    model = icon.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }

                        // 标题
                        Text(
                            text = nativeAd.headline ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 广告主名称（可选）
                    nativeAd.advertiser?.let { advertiser ->
                        Text(
                            text = advertiser,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 正文描述
                    nativeAd.body?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 评分、价格和商店信息行（可选）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 评分（可选）
                        nativeAd.starRating?.let { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "★",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", rating),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }

                        // 价格（可选）
                        nativeAd.price?.let { price ->
                            Text(
                                text = price,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        // 商店名称（可选）
                        nativeAd.store?.let { store ->
                            Text(
                                text = store,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 行动号召按钮
                    nativeAd.callToAction?.let { cta ->
                        androidx.compose.material3.Button(
                            onClick = { callToActionView.performClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = cta,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 将 NativeAd 绑定到 NativeAdView 并注册视图用于点击跟踪
    DisposableEffect(nativeAd) {
        android.util.Log.d(TAG, "🔗 [广告UI] 绑定广告到 NativeAdView")
        
        // 设置隐藏视图的内容（用于 AdMob 点击跟踪）
        headlineView.text = nativeAd.headline ?: ""
        bodyView.text = nativeAd.body ?: ""
        callToActionView.text = nativeAd.callToAction ?: ""
        advertiserView.text = nativeAd.advertiser ?: ""
        priceView.text = nativeAd.price ?: ""
        storeView.text = nativeAd.store ?: ""
        
        // 设置视图为不可见（仅用于 AdMob 跟踪）
        headlineView.visibility = View.GONE
        bodyView.visibility = View.GONE
        callToActionView.visibility = View.GONE
        advertiserView.visibility = View.GONE
        priceView.visibility = View.GONE
        storeView.visibility = View.GONE
        iconView.visibility = View.GONE
        starRatingView.visibility = View.GONE
        
        nativeAd.icon?.let { icon ->
            iconView.setImageDrawable(icon.drawable)
            android.util.Log.d(TAG, "   🖼️ [广告UI] 设置应用图标")
        }
        
        nativeAd.starRating?.let { rating ->
            starRatingView.rating = rating.toFloat()
            android.util.Log.d(TAG, "   ⭐ [广告UI] 设置评分: $rating")
        }

        // 将隐藏的视图添加到 NativeAdView（必需，用于 AdMob 点击跟踪）
        nativeAdView.addView(headlineView)
        nativeAdView.addView(bodyView)
        nativeAdView.addView(callToActionView)
        nativeAdView.addView(iconView)
        nativeAdView.addView(advertiserView)
        nativeAdView.addView(starRatingView)
        nativeAdView.addView(priceView)
        nativeAdView.addView(storeView)

        // 将视图注册到 NativeAdView（必需，用于 AdMob 点击跟踪）
        nativeAdView.setHeadlineView(headlineView)
        nativeAdView.setBodyView(bodyView)
        nativeAdView.setCallToActionView(callToActionView)
        nativeAdView.setIconView(iconView)
        nativeAdView.setAdvertiserView(advertiserView)
        nativeAdView.setStarRatingView(starRatingView)
        nativeAdView.setPriceView(priceView)
        nativeAdView.setStoreView(storeView)
        nativeAdView.mediaView = mediaView

        // 设置原生广告对象（必需）
        nativeAdView.setNativeAd(nativeAd)
        android.util.Log.d(TAG, "✅ [广告UI] 广告卡片渲染完成，已绑定到 NativeAdView")
        android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")

        onDispose {
            android.util.Log.d(TAG, "🗑️ [广告UI] 广告卡片离开组合，清理视图")
            // 清理视图
            nativeAdView.removeView(headlineView)
            nativeAdView.removeView(bodyView)
            nativeAdView.removeView(callToActionView)
            nativeAdView.removeView(iconView)
            nativeAdView.removeView(advertiserView)
            nativeAdView.removeView(starRatingView)
            nativeAdView.removeView(priceView)
            nativeAdView.removeView(storeView)
            android.util.Log.d(TAG, "✅ [广告UI] 视图清理完成")
            // 注意：不要在这里销毁广告，因为广告可能被重用
            // 销毁逻辑应该在 NativeAdManager 中处理
        }
    }
    
    // 隐藏的 NativeAdView 容器，用于 AdMob 点击跟踪（不显示在 UI 中）
    Box(modifier = Modifier.size(0.dp)) {
        AndroidView(
            factory = { nativeAdView },
            modifier = Modifier.size(0.dp),
        )
    }
}

