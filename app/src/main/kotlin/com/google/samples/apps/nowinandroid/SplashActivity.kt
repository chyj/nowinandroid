package com.google.samples.apps.nowinandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Splash Activity rendered with Jetpack Compose. */
class SplashActivity : ComponentActivity() {

  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private var adShown = AtomicBoolean(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      SplashLoadingScreen()
    }

    // Log the Mobile Ads SDK version.
    Log.d(LOG_TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())
    Log.d(LOG_TAG, "Package Name: " + packageName)
    Log.d(LOG_TAG, "Application ID: ca-app-pub-3554230884415364~6501138828 (Production)")
    Log.d(LOG_TAG, "Test Device ID: ${NiaApplication.TEST_DEVICE_HASHED_ID}")
    Log.d(LOG_TAG, "Mode: Production Ad Unit + Test Device (Real Ad Creatives)")

    // 直接初始化并加载广告，不需要用户同意
    initializeMobileAdsSdk()
    
    // 启动协程检查广告状态并显示
    checkAndShowAd()
  }
  
  /**
   * 检查广告状态并显示
   * 如果广告已加载完成，立即显示；否则等待加载完成
   */
  private fun checkAndShowAd() {
    CoroutineScope(Dispatchers.Main).launch {
      // 等待广告加载完成（最多等待10秒）
      var waitedTime = 0L
      val maxWaitTime = 10000L // 10秒
      val checkInterval = 100L // 每100ms检查一次
      
      while (waitedTime < maxWaitTime) {
        val niaApp = application as NiaApplication
        val adResponseData = niaApp.currentAdResponseData
        
        // 如果广告加载失败，立即进入主界面
        if (niaApp.lastLoadError != null && adResponseData == null) {
          Log.d(LOG_TAG, "Ad loading failed, proceeding to main activity.")
          if (!adShown.getAndSet(true)) {
            startMainActivity()
          }
          return@launch
        }
        
        // 如果广告已加载完成，显示广告
        if (adResponseData?.appOpenAd != null) {
          Log.d(LOG_TAG, "Ad loaded, showing now.")
          niaApp.showAdIfAvailable(
            this@SplashActivity,
            object : NiaApplication.OnShowAdCompleteListener {
              override fun onShowAdComplete() {
                // 广告显示完成后，直接进入主界面
                if (!adShown.getAndSet(true)) {
                  startMainActivity()
                }
              }
            }
          )
          return@launch
        }
        
        delay(checkInterval)
        waitedTime += checkInterval
      }
      
      // 超时后，如果没有广告，直接进入主界面
      Log.d(LOG_TAG, "Ad loading timeout, proceeding to main activity.")
      if (!adShown.getAndSet(true)) {
        startMainActivity()
      }
    }
  }

  private fun initializeMobileAdsSdk() {
    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      return
    }

    // 配置测试设备：使用真实广告单元ID + 测试设备ID
    // 这样可以获取接近生产的广告创意，同时避免产生无效点击
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTestDeviceIds(listOf(NiaApplication.TEST_DEVICE_HASHED_ID))
        .build()
    )
    Log.d(LOG_TAG, "RequestConfiguration: Test Device = ${NiaApplication.TEST_DEVICE_HASHED_ID}")

    CoroutineScope(Dispatchers.IO).launch {
      // Initialize the Google Mobile Ads SDK on a background thread.
      MobileAds.initialize(this@SplashActivity) {}
      runOnUiThread {
        // Load an ad on the main thread.
        (application as NiaApplication).loadAd(this@SplashActivity)
      }
    }

    // Load an ad.
  }

  /** Start the MainActivity. */
  fun startMainActivity() {
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
  }

  companion object {
    private const val LOG_TAG = "SplashActivity"
  }
}

@Composable
private fun SplashLoadingScreen() {
  Box(
    modifier =
      Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
