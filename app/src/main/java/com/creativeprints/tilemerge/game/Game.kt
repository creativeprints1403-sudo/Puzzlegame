package com.creativeprints.tilemerge.game

import kotlin.random.Random

const val WIN_VALUE = 2048

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** A numbered tile. [id] stays the same while a tile slides so the UI can animate it. */
data class Tile(
    val id: Long,
    val value: Int,
    val merged: Boolean = false,
    val isNew: Boolean = false,
)

data class GameState(
    val size: Int,
    val cells: List<Tile?>,
    val score: Int = 0,
    val won: Boolean = false,
    val keepPlaying: Boolean = false,
    val over: Boolean = false,
    val nextId: Long = 1,
) {
    operator fun get(row: Int, col: Int): Tile? = cells[row * size + col]

    val maxTile: Int get() = cells.maxOf { it?.value ?: 0 }

    /** True right after the player reaches [WIN_VALUE] and hasn't chosen to keep going. */
    val showWin: Boolean get() = won && !keepPlaying
}

class Game(private val random: Random = Random.Default) {

    fun newGame(size: Int): GameState =
        spawn(spawn(GameState(size = size, cells = List(size * size) { null })))

    /** Adds a 2 (90%) or 4 (10%) in a random empty cell. */
    fun spawn(state: GameState): GameState {
        val empty = state.cells.indices.filter { state.cells[it] == null }
        if (empty.isEmpty()) return state
        val index = empty[random.nextInt(empty.size)]
        val value = if (random.nextInt(10) == 0) 4 else 2
        val cells = state.cells.toMutableList()
        cells[index] = Tile(state.nextId, value, isNew = true)
        return state.copy(cells = cells, nextId = state.nextId + 1)
    }

    /** Slides and merges tiles, then spawns a new one. Returns null if nothing moved. */
    fun move(state: GameState, direction: Direction): GameState? {
        val slid = slide(state, direction) ?: return null
        val next = spawn(slid)
        return next.copy(over = !canMove(next))
    }

    /** Slides and merges tiles without spawning. Returns null if nothing moved. */
    fun slide(state: GameState, direction: Direction): GameState? {
        if (state.over) return null
        val n = state.size
        val cells = MutableList<Tile?>(n * n) { null }
        var gained = 0
        var moved = false
        var won = state.won

        for (line in 0 until n) {
            val indices = lineIndices(n, line, direction)
            val tiles = indices.mapNotNull { state.cells[it] }
            val result = ArrayList<Tile>(tiles.size)
            var i = 0
            while (i < tiles.size) {
                val tile = tiles[i]
                if (i + 1 < tiles.size && tiles[i + 1].value == tile.value) {
                    val value = tile.value * 2
                    result += Tile(tile.id, value, merged = true)
                    gained += value
                    if (value == WIN_VALUE) won = true
                    i += 2
                } else {
                    result += tile.copy(merged = false, isNew = false)
                    i += 1
                }
            }
            indices.forEachIndexed { k, index ->
                val before = state.cells[index]
                val after = result.getOrNull(k)
                cells[index] = after
                if (before?.id != after?.id || before?.value != after?.value) moved = true
            }
        }

        if (!moved) return null
        return state.copy(cells = cells, score = state.score + gained, won = won)
    }

    fun canMove(state: GameState): Boolean {
        val n = state.size
        if (state.cells.any { it == null }) return true
        for (r in 0 until n) for (c in 0 until n) {
            val v = state[r, c]?.value
            if (c + 1 < n && state[r, c + 1]?.value == v) return true
            if (r + 1 < n && state[r + 1, c]?.value == v) return true
        }
        return false
    }

    /** Cell indices of one row or column, ordered from the edge the tiles slide toward. */
    private fun lineIndices(n: Int, line: Int, direction: Direction): List<Int> = when (direction) {
        Direction.LEFT -> (0 until n).map { line * n + it }
        Direction.RIGHT -> (n - 1 downTo 0).map { line * n + it }
        Direction.UP -> (0 until n).map { it * n + line }
        Direction.DOWN -> (n - 1 downTo 0).map { it * n + line }
    }
}
