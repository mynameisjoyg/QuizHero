package com.joyg.quizhero

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false


    /**
     * 預先載入廣告
     */
    fun loadAd(onLoaded: (() -> Unit)? = null, onFailed: (() -> Unit)? = null) {
        if (rewardedAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            context.getString(R.string.rewardedAdUnitId),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    onFailed?.invoke()
                }
            }
        )
    }

    /**
     * 播放廣告
     * @param activity 傳入目前的 Activity
     * @param onUserEarnedReward 當使用者看完廣告獲得獎勵時的回調
     * @param onAdClosed 廣告關閉後的回調（不論是否有獲得獎勵）
     */
    fun showAd(activity: Activity, onUserEarnedReward: (amount: Int, type: String) -> Unit, onAdClosed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                // 使用者完整觀看廣告，給予獎勵
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
            // 廣告播放完後釋放，並預載下一支廣告
            rewardedAd = null
            loadAd()
            onAdClosed()
        } else {
            // 廣告尚未準備好時的處置
            onAdClosed()
            loadAd()
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
