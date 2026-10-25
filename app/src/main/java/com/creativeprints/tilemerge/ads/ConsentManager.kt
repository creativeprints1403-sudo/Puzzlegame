package com.creativeprints.tilemerge.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Shows Google's consent form where the law requires it (EU/UK, some US states)
 * before any ads are requested. The form itself is configured in AdMob under
 * Privacy & messaging.
 */
class ConsentManager(private val activity: Activity) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    val canRequestAds: Boolean get() = info.canRequestAds()

    val privacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gather(onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> onDone() } },
            { _ -> onDone() },
        )
    }

    fun showPrivacyOptions(onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDone() }
    }
}
