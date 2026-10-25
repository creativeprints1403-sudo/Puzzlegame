package com.creativeprints.tilemerge

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.creativeprints.tilemerge.billing.BillingManager
import com.creativeprints.tilemerge.game.Direction
import com.creativeprints.tilemerge.game.Game
import com.creativeprints.tilemerge.game.GameCodec
import com.creativeprints.tilemerge.game.GameState

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("tilemerge", Context.MODE_PRIVATE)
    private val game = Game()
    private val history = ArrayDeque<GameState>()

    var size by mutableIntStateOf(prefs.getInt(KEY_SIZE, 4))
        private set
    var state by mutableStateOf(load(size))
        private set
    var best by mutableIntStateOf(prefs.getInt(bestKey(size), 0))
        private set
    var undoCredits by mutableIntStateOf(prefs.getInt(KEY_UNDOS, FREE_UNDOS))
        private set
    var premium by mutableStateOf(prefs.getBoolean(KEY_PREMIUM, false))
        private set

    val billing = BillingManager(app) { owned ->
        premium = owned
        prefs.edit { putBoolean(KEY_PREMIUM, owned) }
    }

    init {
        billing.connect()
    }

    val hasHistory: Boolean get() = history.isNotEmpty()
    val canUndo: Boolean get() = hasHistory && (premium || undoCredits > 0)

    /** Returns true if any tiles merged, so the UI can give haptic feedback. */
    fun move(direction: Direction): Boolean {
        val next = game.move(state, direction) ?: return false
        history.addLast(state)
        if (history.size > MAX_HISTORY) history.removeFirst()
        update(next)
        return next.cells.any { it?.merged == true }
    }

    fun undo() {
        if (!canUndo) return
        if (!premium) {
            undoCredits--
            prefs.edit { putInt(KEY_UNDOS, undoCredits) }
        }
        update(history.removeLast())
    }

    fun addUndoCredits(count: Int) {
        undoCredits += count
        prefs.edit { putInt(KEY_UNDOS, undoCredits) }
    }

    fun newGame() {
        history.clear()
        update(game.newGame(size))
    }

    fun keepPlaying() = update(state.copy(keepPlaying = true))

    fun selectSize(newSize: Int) {
        if (newSize == size) return
        size = newSize
        prefs.edit { putInt(KEY_SIZE, newSize) }
        history.clear()
        best = prefs.getInt(bestKey(newSize), 0)
        state = load(newSize)
    }

    private fun update(next: GameState) {
        state = next
        if (next.score > best) best = next.score
        prefs.edit {
            putString(gameKey(size), GameCodec.encode(next))
            putInt(bestKey(size), best)
        }
    }

    private fun load(size: Int): GameState =
        GameCodec.decode(prefs.getString(gameKey(size), null))?.takeIf { it.size == size }
            ?: game.newGame(size)

    override fun onCleared() {
        billing.end()
    }

    private companion object {
        const val KEY_SIZE = "size"
        const val KEY_UNDOS = "undo_credits"
        const val KEY_PREMIUM = "premium"
        const val FREE_UNDOS = 3
        const val MAX_HISTORY = 20
        fun gameKey(size: Int) = "game_$size"
        fun bestKey(size: Int) = "best_$size"
    }
}
