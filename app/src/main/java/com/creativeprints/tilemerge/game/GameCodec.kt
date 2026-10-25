package com.creativeprints.tilemerge.game

/** Turns a [GameState] into a short string for saving, and back. */
object GameCodec {
    fun encode(state: GameState): String = listOf(
        state.size,
        state.score,
        if (state.won) 1 else 0,
        if (state.keepPlaying) 1 else 0,
        if (state.over) 1 else 0,
        state.cells.joinToString(",") { (it?.value ?: 0).toString() },
    ).joinToString(";")

    fun decode(text: String?): GameState? = runCatching {
        val parts = text!!.split(";")
        val size = parts[0].toInt()
        val values = parts[5].split(",").map { it.toInt() }
        require(values.size == size * size)
        var id = 1L
        GameState(
            size = size,
            score = parts[1].toInt(),
            won = parts[2] == "1",
            keepPlaying = parts[3] == "1",
            over = parts[4] == "1",
            cells = values.map { v -> if (v == 0) null else Tile(id++, v) },
            nextId = id,
        )
    }.getOrNull()
}
