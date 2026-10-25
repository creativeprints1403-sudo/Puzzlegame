package com.creativeprints.tilemerge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeprints.tilemerge.BuildConfig
import com.creativeprints.tilemerge.GameViewModel
import com.creativeprints.tilemerge.ads.AdsManager
import com.creativeprints.tilemerge.ads.BannerAd
import com.creativeprints.tilemerge.game.WIN_VALUE

/** How many undos one rewarded ad gives. */
const val UNDOS_PER_AD = 3

@Composable
fun GameScreen(
    vm: GameViewModel,
    ads: AdsManager,
    privacyOptionsRequired: Boolean,
    onBuyPremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    onPrivacyOptions: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val state = vm.state
    val showAds = !vm.premium && ads.ready

    fun newGame() {
        if (!vm.premium) ads.onNewGame()
        vm.newGame()
    }

    fun undo() {
        when {
            vm.canUndo -> vm.undo()
            vm.hasHistory && ads.rewardedReady -> ads.showRewarded {
                vm.addUndoCredits(UNDOS_PER_AD)
                vm.undo()
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tile Merge",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    ScoreBox("SCORE", state.score)
                    Spacer(Modifier.padding(4.dp))
                    ScoreBox("BEST", vm.best)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::newGame) { Text("New") }
                    OutlinedButton(onClick = ::undo, enabled = vm.hasHistory) {
                        Text(
                            when {
                                vm.premium -> "Undo"
                                vm.undoCredits > 0 -> "Undo (${vm.undoCredits})"
                                ads.rewardedReady -> "Undo ▶ ad"
                                else -> "Undo (0)"
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in 4..6) {
                        FilterChip(
                            selected = vm.size == n,
                            onClick = { vm.selectSize(n) },
                            label = { Text("$n×$n") },
                        )
                    }
                }

                Board(
                    state = state,
                    onSwipe = { dir ->
                        if (vm.move(dir)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Swipe to move. Join equal numbers to reach $WIN_VALUE!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                )

                if (!vm.premium) {
                    TextButton(onClick = onBuyPremium) {
                        Text(
                            "Remove ads + unlimited undo" + (vm.billing.price?.let { " · $it" } ?: ""),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (showAds) BannerAd()
        }
    }

    if (state.showWin) {
        AlertDialog(
            onDismissRequest = vm::keepPlaying,
            title = { Text("You made $WIN_VALUE! 🎉") },
            text = { Text("Score: ${state.score}. Keep going for a bigger tile?") },
            confirmButton = { Button(onClick = vm::keepPlaying) { Text("Keep going") } },
            dismissButton = { TextButton(onClick = ::newGame) { Text("New game") } },
        )
    } else if (state.over) {
        val canRescue = vm.canUndo || (!vm.premium && ads.rewardedReady && vm.hasHistory)
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game over") },
            text = { Text("Score: ${state.score}\nBest: ${vm.best}") },
            confirmButton = { Button(onClick = ::newGame) { Text("Try again") } },
            dismissButton = {
                if (canRescue) {
                    TextButton(onClick = ::undo) {
                        Text(if (vm.canUndo) "Undo last move" else "Watch ad to undo")
                    }
                }
            },
        )
    }

    if (showSettings) {
        val uri = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (vm.premium) Text("Premium unlocked. Thank you! ❤")
                    TextButton(onClick = onRestorePurchase) { Text("Restore purchase") }
                    if (privacyOptionsRequired) {
                        TextButton(onClick = onPrivacyOptions) { Text("Ad privacy choices") }
                    }
                    TextButton(onClick = { uri.openUri(BuildConfig.PRIVACY_POLICY_URL) }) { Text("Privacy policy") }
                    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
