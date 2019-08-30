package de.smartsquare.hangman

import arrow.effects.IO
import arrow.effects.extensions.io.fx.fx
import arrow.effects.fix
import arrow.effects.liftIO
import arrow.optics.Lens
import arrow.syntax.collections.firstOption
import java.io.IOException
import kotlin.random.Random
import kotlin.streams.toList

fun putStrLn(line: String): IO<Unit> = IO { println(line) }
fun getStrLn(): IO<String> = IO { readLine() ?: throw IOException("Failed to read input!") }

object Hangman {

    data class State(val player: String)

    val dictionary: List<String> by lazy {
        javaClass.classLoader.getResource("words.txt")
                .openStream()
                .bufferedReader()
                .lines()
                .toList()
    }

    val hangman: IO<Unit> = fx {
        // at least: choose a new random word from the dictionary and start the game 'loop' , e.g.
        putStrLn("Let's try it together.").bind()
        val (playerName) = getPlayerName
        val initialState = State(playerName)
        gameLoop(initialState).bind()
        Unit
    }.fix()

    val getPlayerName: IO<String> = fx {
        putStrLn("Please enter your name: ").bind()
        val (name) = getStrLn()
        name
    }.fix()

    fun renderState(state: State): IO<Unit> {
        return putStrLn(state.player)
    }

    fun gameLoop(state: State): IO<State> = fx {
        renderState(state).bind()
        state
    }.fix()
}

fun main() {
    Hangman.hangman.unsafeRunSync()
}