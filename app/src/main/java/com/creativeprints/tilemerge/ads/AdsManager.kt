package com.creativeprints.tilemerge.ads

import android.app.Activity
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.creativeprints.tilemerge.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Loads and shows the interstitial and rewarded ads. Banners live in BannerAd.kt. */
class AdsManager(private val activity: Activity) {
    private var started = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var newGamesSinceInterstitial = 0
    private var lastInterstitialAt = 0L

    /** True once the Mobile Ads SDK is ready and the user's consent allows ads. */
    var ready by mutableStateOf(false)
        private set

    var rewardedReady by mutableStateOf(false)
        private set

    fun initialize() {
        if (started) return
        started = true
        MobileAds.initialize(activity) {
            activity.runOnUiThread {
                ready = true
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    private fun request() = AdRequest.Builder().build()

    private fun loadInterstitial() {
        InterstitialAd.load(activity, BuildConfig.INTERSTITIAL_ID, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
            }
        })
    }

    private fun loadRewarded() {
        RewardedAd.load(activity, BuildConfig.REWARDED_ID, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                rewardedReady = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                rewardedReady = false
            }
        })
    }

    /**
     * Called when the player starts a new game. Shows an interstitial at most every
     * [GAMES_BETWEEN_INTERSTITIALS] games and [MIN_INTERVAL_MS] apart, so ads stay
     * tolerable and within AdMob's placement policies.
     */
    fun onNewGame() {
        newGamesSinceInterstitial++
        val ad = interstitial ?: return
        val now = SystemClock.elapsedRealtime()
        if (newGamesSinceInterstitial < GAMES_BETWEEN_INTERSTITIALS) return
        if (lastInterstitialAt != 0L && now - lastInterstitialAt < MIN_INTERVAL_MS) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadInterstitial()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = loadInterstitial()
        }
        interstitial = null
        newGamesSinceInterstitial = 0
        lastInterstitialAt = now
        ad.show(activity)
    }

    /** Shows a rewarded ad and calls [onReward] only if the player watched it. */
    fun showRewarded(onReward: () -> Unit) {
        val ad = rewarded ?: return
        rewarded = null
        rewardedReady = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadRewarded()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = loadRewarded()
        }
        ad.show(activity) { onReward() }
    }

    private companion object {
        const val GAMES_BETWEEN_INTERSTITIALS = 2
        const val MIN_INTERVAL_MS = 3 * 60 * 1000L
    }
}
