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
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.samples.apps.nowinandroid.sync.initializers.Sync
import com.google.samples.apps.nowinandroid.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

/**
 * [Application] class for NiA
 */
@HiltAndroidApp
class NiaApplication :
    MultiDexApplication(),
    ImageLoaderFactory,
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    /**
     * 存储 AppOpenAd 加载成功后的响应数据
     */
    data class AppOpenAdResponseData(
        val appOpenAd: AppOpenAd? = null,
        val responseInfo: ResponseInfo? = null,
        val adUnitId: String? = null,
        val loadTime: Long = 0
    )

    /**
     * 当前加载成功的广告响应数据
     */
    var currentAdResponseData: AppOpenAdResponseData? = null
        private set

    /**
     * 最后一次加载失败的错误信息
     */
    var lastLoadError: LoadAdError? = null
        private set

    override fun onCreate() {
        super<MultiDexApplication>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        setStrictModePolicy()

        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
        profileVerifierLogger()
    }

    /**
    * DefaultLifecycleObserver method that shows the app open ad when the app moves to foreground.
    */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let {
        // Show the ad (if available) when the app moves to foreground.
        appOpenAdManager.showAdIfAvailable(it)
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

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        if (!appOpenAdManager.isShowingAd) {
        currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {} 

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /**
     * Load an app open ad.
     *
     * @param activity the activity that shows the app open ad
     */
    fun loadAd(activity: Activity) {
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.loadAd(activity)
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private inner class AppOpenAdManager {

        private var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager =
          GoogleMobileAdsConsentManager.getInstance(applicationContext)
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false
    
        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0
    
        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
          // Do not load ad if there is an unused ad or one is already loading.
          if (isLoadingAd || isAdAvailable()) {
            return
          }
    
          isLoadingAd = true
          // 构建 AdRequest，可以添加内容定位参数来影响广告匹配
          val request = AdRequest.Builder()
            // 1. 内容URL：指定当前页面或内容的URL，帮助AdMob匹配相关广告
            // .setContentUrl("https://www.nowinandroid.apps.samples.google.com")
            
            // 2. 关键词：添加关键词来帮助AdMob匹配相关广告
            // .addKeyword("android")
            // .addKeyword("development")
            // .addKeyword("news")
            
            // 3. 请求代理：标识请求来源（可选）
            // .setRequestAgent("NowInAndroid-App")
            
            // 4. 邻居内容字符串：提供页面内容的文本描述（可选）
            // .setNeighboringContentUrls(listOf("https://example.com"))
            
            // 注意：App Open Ad 通常不需要这些参数，因为这些参数主要用于
            // Banner、Interstitial 等广告类型。App Open Ad 的广告内容主要由
            // AdMob 后台的广告活动配置决定（如预订广告）。
            .build()
          AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            object : AppOpenAdLoadCallback() {
              /**
               * Called when an app open ad has loaded.
               *
               * @param ad the loaded app open ad.
               */
              override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                isLoadingAd = false
                loadTime = Date().time
                
                // 获取响应信息
                val responseInfo = ad.responseInfo
                val adUnitId = AD_UNIT_ID
                
                // 保存到应用单例
                this@NiaApplication.currentAdResponseData = AppOpenAdResponseData(
                    appOpenAd = ad,
                    responseInfo = responseInfo,
                    adUnitId = adUnitId,
                    loadTime = loadTime
                )
                
                // 打印详细日志
                Log.d(LOG_TAG, "========== AppOpenAd Loaded Successfully ==========")
                Log.d(LOG_TAG, "Package Name: ${context.packageName}")
                Log.d(LOG_TAG, "AdUnitId: $adUnitId (Production Ad Unit)")
                Log.d(LOG_TAG, "LoadTime: ${Date(loadTime)}")
                Log.d(LOG_TAG, "Note: Using Production Ad Unit with Test Device - showing real ad creatives")
                
                responseInfo?.let { info ->
                    // 打印完整的 ResponseInfo JSON（与官方示例一致）
                    Log.d(LOG_TAG, "ResponseInfo: $info")
                    
                    Log.d(LOG_TAG, "ResponseId: ${info.responseId}")
                    Log.d(LOG_TAG, "MediationAdapterClassName: ${info.mediationAdapterClassName}")
                    
                    // 尝试获取并打印 LoadedAdapterResponse（与官方示例一致）
                    try {
                        val loadedAdapterResponseMethod = info.javaClass.getMethod("getLoadedAdapterResponse")
                        val loadedAdapterResponse = loadedAdapterResponseMethod.invoke(info)
                        loadedAdapterResponse?.let { loaded ->
                            try {
                                val adSourceNameMethod = loaded.javaClass.getMethod("getAdSourceName")
                                val adSourceName = adSourceNameMethod.invoke(loaded) as? String
                                Log.d(LOG_TAG, "AdSourceName: ${adSourceName ?: ""}")
                            } catch (e: Exception) {}
                            
                            try {
                                val adSourceIdMethod = loaded.javaClass.getMethod("getAdSourceId")
                                val adSourceId = adSourceIdMethod.invoke(loaded)
                                Log.d(LOG_TAG, "AdSourceId: $adSourceId")
                            } catch (e: Exception) {}
                            
                            try {
                                val latencyMillisMethod = loaded.javaClass.getMethod("getLatencyMillis")
                                val latencyMillis = latencyMillisMethod.invoke(loaded)
                                Log.d(LOG_TAG, "LatencyMillis: $latencyMillis")
                            } catch (e: Exception) {}
                        }
                    } catch (e: Exception) {
                        // getLoadedAdapterResponse 方法不存在，忽略
                    }
                    
                    // 打印所有适配器响应信息（包括详细信息）
                    info.adapterResponses.forEachIndexed { index, adapterResponse ->
                        Log.d(LOG_TAG, "AdapterResponse[$index]: ${adapterResponse.adapterClassName}")
                        Log.d(LOG_TAG, "  - AdSourceName: ${adapterResponse.adSourceName}")
                        
                        // 尝试获取 AdSourceId（如果存在）
                        try {
                            val adSourceIdMethod = adapterResponse.javaClass.getMethod("getAdSourceId")
                            val adSourceId = adSourceIdMethod.invoke(adapterResponse)
                            Log.d(LOG_TAG, "  - AdSourceId: $adSourceId")
                        } catch (e: Exception) {
                            // getAdSourceId 方法不存在，忽略
                        }
                        
                        Log.d(LOG_TAG, "  - LatencyMillis: ${adapterResponse.latencyMillis}ms")
                        adapterResponse.adError?.let { error ->
                            Log.d(LOG_TAG, "  - AdError: code=${error.code}, message=${error.message}")
                        } ?: Log.d(LOG_TAG, "  - AdError: null")
                    }
                } ?: Log.w(LOG_TAG, "ResponseInfo is null")
                
                Log.d(LOG_TAG, "AppOpenAd Object: $ad")
                Log.d(LOG_TAG, "==================================================")
                
                Toast.makeText(context, "onAdLoaded", Toast.LENGTH_SHORT).show()
              }
    
              /**
               * Called when an app open ad has failed to load.
               *
               * @param loadAdError the error.
               */
              override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoadingAd = false
                
                // 保存错误信息到应用单例
                this@NiaApplication.lastLoadError = loadAdError
                
                // 打印详细错误日志
                Log.e(LOG_TAG, "========== AppOpenAd Load Failed ==========")
                Log.e(LOG_TAG, "AdUnitId: $AD_UNIT_ID")
                Log.e(LOG_TAG, "Application ID: ca-app-pub-3554230884415364~6501138828")
                Log.e(LOG_TAG, "Error Code: ${loadAdError.code}")
                Log.e(LOG_TAG, "Error Domain: ${loadAdError.domain}")
                Log.e(LOG_TAG, "Error Message: ${loadAdError.message}")
                Log.e(LOG_TAG, "Error Cause: ${loadAdError.cause}")
                
                // 错误代码3的特殊处理说明
                if (loadAdError.code == 3) {
                    Log.e(LOG_TAG, "")
                    Log.e(LOG_TAG, "⚠️ 错误代码3 - Publisher data not found")
                    Log.e(LOG_TAG, "可能的原因：")
                    Log.e(LOG_TAG, "1. 广告单元ID未在AdMob后台创建")
                    Log.e(LOG_TAG, "2. 广告单元ID未激活")
                    Log.e(LOG_TAG, "3. Application ID和Ad Unit ID不匹配（不属于同一账户）")
                    Log.e(LOG_TAG, "4. 应用包名与AdMob后台注册的不一致")
                    Log.e(LOG_TAG, "")
                    Log.e(LOG_TAG, "解决方案：")
                    Log.e(LOG_TAG, "1. 登录AdMob控制台：https://apps.admob.com")
                    Log.e(LOG_TAG, "2. 确认广告单元ID存在：$AD_UNIT_ID")
                    Log.e(LOG_TAG, "3. 确认广告单元已激活")
                    Log.e(LOG_TAG, "4. 确认Application ID和Ad Unit ID属于同一账户")
                    Log.e(LOG_TAG, "")
                }
                
                // 打印 ResponseInfo（如果存在）
                loadAdError.responseInfo?.let { responseInfo ->
                    Log.e(LOG_TAG, "ResponseInfo:")
                    Log.e(LOG_TAG, "  - ResponseId: ${responseInfo.responseId}")
                    Log.e(LOG_TAG, "  - MediationAdapterClassName: ${responseInfo.mediationAdapterClassName}")
                    
                    // 打印适配器响应信息
                    responseInfo.adapterResponses.forEachIndexed { index, adapterResponse ->
                        Log.e(LOG_TAG, "  - AdapterResponse[$index]:")
                        Log.e(LOG_TAG, "    * AdSourceName: ${adapterResponse.adSourceName}")
                        Log.e(LOG_TAG, "    * LatencyMillis: ${adapterResponse.latencyMillis}ms")
                        adapterResponse.adError?.let { error ->
                            Log.e(LOG_TAG, "    * AdError: code=${error.code}, domain=${error.domain}, message=${error.message}")
                        } ?: Log.e(LOG_TAG, "    * AdError: null")
                    }
                } ?: Log.w(LOG_TAG, "ResponseInfo is null")
                
                Log.e(LOG_TAG, "Formatted Error: ${formatLoadAdError(loadAdError)}")
                Log.e(LOG_TAG, "===========================================")
                
                Toast.makeText(
                    context,
                    "onAdFailedToLoad: ${loadAdError.code}",
                    Toast.LENGTH_SHORT
                  )
                  .show()
              }
            },
          )
        }

        private fun formatLoadAdError(loadAdError: LoadAdError): String {
          val responseInfo = loadAdError.responseInfo
          val mediationInfo =
            responseInfo?.let {
              val adapterResponsesSummary =
                it.adapterResponses.joinToString(prefix = "[", postfix = "]") { response ->
                  val errorMessage = response.adError?.message ?: "none"
                  "{adSource=${response.adSourceName}, latency=${response.latencyMillis}ms, error=$errorMessage}"
                }
              "mediationAdapter=${it.mediationAdapterClassName}, adapterResponses=$adapterResponsesSummary"
            }
          return buildString {
            append("code=${loadAdError.code}, domain=${loadAdError.domain}, message=${loadAdError.message}")
            if (mediationInfo != null) {
              append(", ").append(mediationInfo)
            }
          }
        }
    
        /** Check if ad was loaded more than n hours ago. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
          val dateDifference: Long = Date().time - loadTime
          val numMilliSecondsPerHour: Long = 3600000
          return dateDifference < numMilliSecondsPerHour * numHours
        }
    
        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
          // Ad references in the app open beta will time out after four hours, but this time limit
          // may change in future beta versions. For details, see:
          // https://support.google.com/admob/answer/9341964?hl=en
          return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }
    
        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
          showAdIfAvailable(
            activity,
            object : OnShowAdCompleteListener {
              override fun onShowAdComplete() {
                // Empty because the user will go back to the activity that shows the ad.
              }
            },
          )
        }
    
        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
          // If the app open ad is already showing, do not show the ad again.
          if (isShowingAd) {
            Log.d(LOG_TAG, "The app open ad is already showing.")
            return
          }
    
          // If the app open ad is not available yet, invoke the callback.
          if (!isAdAvailable()) {
            Log.d(LOG_TAG, "The app open ad is not ready yet.")
            onShowAdCompleteListener.onShowAdComplete()
            if (googleMobileAdsConsentManager.canRequestAds) {
              loadAd(activity)
            }
            return
          }
    
          Log.d(LOG_TAG, "Will show ad.")
    
          appOpenAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
              /** Called when full screen content is dismissed. */
              override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")
                Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT).show()
    
                onShowAdCompleteListener.onShowAdComplete()
                if (googleMobileAdsConsentManager.canRequestAds) {
                  loadAd(activity)
                }
              }
    
              /** Called when fullscreen content failed to show. */
              override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                Toast.makeText(activity, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT).show()
    
                onShowAdCompleteListener.onShowAdComplete()
                if (googleMobileAdsConsentManager.canRequestAds) {
                  loadAd(activity)
                }
              }
    
              /** Called when fullscreen content is shown. */
              override fun onAdShowedFullScreenContent() {
                Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
                Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show()
              }
            }
          isShowingAd = true
          appOpenAd?.show(activity)
        }
      }
    
      companion object {
        /**
         * 真实的生产环境广告单元 ID
         * 
         * 使用"测试设备 + 真实广告单元ID"的方式可以：
         * 1. 获取接近生产的广告创意（真实广告内容）
         * 2. 在测试设备上安全测试，不会产生无效点击
         * 3. 验证广告展示效果和用户体验
         * 
         * 注意：
         * - 测试设备ID需要在logcat中获取（见下方说明）
         * - 只有添加到测试设备列表的设备才会显示真实广告
         * - 其他设备会显示测试广告，避免产生无效点击
         * 
         * ⚠️ 重要：确保此ID在AdMob后台存在且已激活
         * 当前使用的广告单元：testOpen1
         * AdMob后台地址：https://apps.admob.com
         * 
         * 如果遇到错误代码3 "Publisher data not found"：
         * 1. 登录AdMob后台确认广告单元ID存在
         * 2. 确认广告单元已激活
         * 3. 确认Application ID和Ad Unit ID属于同一账户
         * 4. 注意：应用审批状态为"需要审核"时可能影响广告加载
         */
        private const val AD_UNIT_ID = "ca-app-pub-3554230884415364/5736111244" // testOpen1 - 开屏广告
        private const val LOG_TAG = "NiaApplication"
    
        /**
         * 测试设备哈希ID
         * 
         * 如何获取你的测试设备ID：
         * 1. 运行应用，查看logcat输出
         * 2. 查找类似这样的日志：
         *    "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("YOUR_DEVICE_ID"))
         *     to get test ads on this device"
         * 3. 将 YOUR_DEVICE_ID 替换下面的占位符
         * 
         * 或者从logcat中查找：
         * "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("YOUR_DEVICE_ID")"
         * 
         * 当前配置的设备ID：39932DCCD8F03408461EA41EB5F1E43C
         * ✅ 已配置真实测试设备ID
         */
        const val TEST_DEVICE_HASHED_ID = "39932DCCD8F03408461EA41EB5F1E43C"
      }

}
