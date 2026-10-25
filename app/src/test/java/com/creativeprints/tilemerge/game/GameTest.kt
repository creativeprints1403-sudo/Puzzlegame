package com.creativeprints.tilemerge.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private val game = Game(Random(42))

    private fun board(vararg rows: List<Int>): GameState {
        var id = 1L
        val cells = rows.flatMap { row -> row.map { if (it == 0) null else Tile(id++, it) } }
        return GameState(size = rows.size, cells = cells, nextId = id)
    }

    private fun GameState.values() = cells.map { it?.value ?: 0 }.chunked(size)

    @Test fun mergesPairsLeft() {
        val s = game.slide(board(listOf(2, 2, 2, 2), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0)), Direction.LEFT)!!
        assertEquals(listOf(4, 4, 0, 0), s.values()[0])
        assertEquals(8, s.score)
    }

    @Test fun mergesEachTileOnlyOnce() {
        val s = game.slide(board(listOf(4, 4, 8, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0)), Direction.LEFT)!!
        assertEquals(listOf(8, 8, 0, 0), s.values()[0])
    }

    @Test fun slidesRightAndDown() {
        val start = board(listOf(2, 0, 2, 4), listOf(0, 0, 0, 0), listOf(2, 0, 0, 0), listOf(0, 0, 0, 0))
        assertEquals(listOf(0, 0, 4, 4), game.slide(start, Direction.RIGHT)!!.values()[0])
        val down = game.slide(start, Direction.DOWN)!!.values()
        assertEquals(listOf(4, 0, 2, 4), down[3])
    }

    @Test fun noMoveReturnsNull() {
        assertNull(game.slide(board(listOf(2, 4, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0)), Direction.LEFT))
    }

    @Test fun reachingWinValueSetsWon() {
        val s = game.slide(board(listOf(1024, 1024, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0)), Direction.LEFT)!!
        assertTrue(s.won)
        assertTrue(s.showWin)
    }

    @Test fun detectsGameOver() {
        val full = board(listOf(2, 4, 2, 4), listOf(4, 2, 4, 2), listOf(2, 4, 2, 4), listOf(4, 2, 4, 2))
        assertFalse(game.canMove(full))
        val mergeable = board(listOf(2, 2, 4, 8), listOf(4, 8, 16, 32), listOf(8, 16, 32, 64), listOf(16, 32, 64, 128))
        assertTrue(game.canMove(mergeable))
    }

    @Test fun moveSpawnsOneTile() {
        val start = board(listOf(2, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val s = game.move(start, Direction.RIGHT)!!
        assertEquals(2, s.cells.count { it != null })
    }

    @Test fun newGameHasTwoTiles() {
        for (size in 4..6) assertEquals(2, game.newGame(size).cells.count { it != null })
    }

    @Test fun codecRoundTrips() {
        val s = game.newGame(5).copy(score = 1234, won = true, keepPlaying = true)
        val back = GameCodec.decode(GameCodec.encode(s))
        assertNotNull(back)
        assertEquals(s.values(), back!!.values())
        assertEquals(1234, back.score)
        assertTrue(back.keepPlaying)
        assertNull(GameCodec.decode("garbage"))
    }
}
