package com.google.samples.apps.nowinandroid

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Splash Activity rendered with Jetpack Compose. */
class SplashActivity : ComponentActivity() {

  private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private val gatherConsentFinished = AtomicBoolean(false)
  private var secondsRemaining: Long = 0L
  private var countdownText by mutableStateOf("")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      SplashCountdownScreen(text = countdownText)
    }

    // Log the Mobile Ads SDK version.
    Log.d(LOG_TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())
    Log.d(LOG_TAG, "Package Name: " + packageName)
    Log.d(LOG_TAG, "Application ID: ca-app-pub-3554230884415364~6501138828 (Production)")
    Log.d(LOG_TAG, "Test Device ID: ${NiaApplication.TEST_DEVICE_HASHED_ID}")
    Log.d(LOG_TAG, "Mode: Production Ad Unit + Test Device (Real Ad Creatives)")

    // Create a timer so the SplashActivity will be displayed for a fixed amount of time.
    createTimer()

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
    googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
      if (consentError != null) {
        // Consent not obtained in current session.
        Log.w(LOG_TAG, String.format("%s: %s", consentError.errorCode, consentError.message))
      }

      gatherConsentFinished.set(true)

      if (googleMobileAdsConsentManager.canRequestAds) {
        initializeMobileAdsSdk()
      }

      if (secondsRemaining <= 0) {
        startMainActivity()
      }
    }

    // This sample attempts to load ads using consent obtained in the previous session.
    if (googleMobileAdsConsentManager.canRequestAds) {
      initializeMobileAdsSdk()
    }
  }

  /**
   * Create the countdown timer, which counts down to zero and show the app open ad.
   *
   * @param time the number of milliseconds that the timer counts down from
   */
  private fun createTimer() {
    val countDownTimer: CountDownTimer =
      object : CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
        override fun onTick(millisUntilFinished: Long) {
          secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
          countdownText = "App is done loading in: $secondsRemaining"
        }

        override fun onFinish() {
          secondsRemaining = 0
          countdownText = "Done."

          (application as NiaApplication).showAdIfAvailable(
            this@SplashActivity,
            object : NiaApplication.OnShowAdCompleteListener {
              override fun onShowAdComplete() {
                // Check if the consent form is currently on screen before moving to the main
                // activity.
                if (gatherConsentFinished.get()) {
                  startMainActivity()
                }
              }
            }
          )
        }
      }
    countDownTimer.start()
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
    // Number of milliseconds to count down before showing the app open ad. This simulates the time
    // needed to load the app.
    private const val COUNTER_TIME_MILLISECONDS = 10000L

    private const val LOG_TAG = "SplashActivity"
  }
}

@Composable
private fun SplashCountdownScreen(text: String) {
  Box(
    modifier =
      Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = text.ifEmpty { " " }, style = MaterialTheme.typography.bodyLarge)
  }
}
