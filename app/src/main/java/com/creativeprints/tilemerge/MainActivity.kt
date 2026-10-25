package com.creativeprints.tilemerge

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.creativeprints.tilemerge.ads.AdsManager
import com.creativeprints.tilemerge.ads.ConsentManager
import com.creativeprints.tilemerge.ui.GameScreen
import com.creativeprints.tilemerge.ui.TileMergeTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    private lateinit var ads: AdsManager
    private lateinit var consent: ConsentManager
    private var privacyOptionsRequired by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ads = AdsManager(this)
        consent = ConsentManager(this)

        // Consent from a previous session lets ads start right away.
        if (consent.canRequestAds) ads.initialize()
        consent.gather {
            if (consent.canRequestAds) ads.initialize()
            privacyOptionsRequired = consent.privacyOptionsRequired
        }

        setContent {
            TileMergeTheme {
                GameScreen(
                    vm = vm,
                    ads = ads,
                    privacyOptionsRequired = privacyOptionsRequired,
                    onBuyPremium = {
                        if (!vm.billing.launchPurchase(this)) {
                            Toast.makeText(this, "Store not ready, please try again in a moment.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRestorePurchase = {
                        vm.billing.connect()
                        Toast.makeText(this, "Checking your purchases…", Toast.LENGTH_SHORT).show()
                    },
                    onPrivacyOptions = { consent.showPrivacyOptions { } },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.billing.refreshPurchases()
    }
}
