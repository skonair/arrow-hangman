package de.smartsquare.hangman.solution

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

    data class State(val name: String, val guesses: Set<Char> = emptySet(), val word: String) {

        val failures: Int = (guesses.toSet().minus(word.toSet())).size

        val playerLost: Boolean = failures > 8

        val playerWon: Boolean = (word.toSet().minus(guesses)).isEmpty()
    }

    val stateLens: Lens<State, Set<Char>> = Lens(
            get = { state -> state.guesses},
            set = { state, value -> state.copy(guesses = value)}
    )

    val dictionary: List<String> by lazy {
        javaClass.classLoader.getResource("words.txt")
                .openStream()
                .bufferedReader()
                .lines()
                .toList()
    }

    val hangman: IO<Unit> = fx {
        putStrLn("Welcome to purely functional hangman").bind()
        val (name) = getName
        putStrLn("Welcome $name. Let's begin!").bind()
        val (word) = chooseWord
        val state = State(name, word = word)
        renderState(state).bind()
        gameLoop(state).bind()
        Unit
    }.fix()

    fun gameLoop(state: State): IO<State> = fx {
        val guess = getChoice().bind()
        val updatedState = stateLens.lift { it.plus(guess) }(state)
        renderState(updatedState).bind()
        val loop = when {
            updatedState.playerWon -> putStrLn("Congratulations ${state.name} you won the game").map { false }
            updatedState.playerLost -> putStrLn("Sorry ${state.name} you lost the game. The word was ${state.word}").map { false }
            updatedState.word.contains(guess) -> putStrLn("You guessed correctly!").map { true }
            else -> putStrLn("That's wrong, but keep trying").map { true }
        }.bind()
        if (loop) gameLoop(updatedState).bind() else updatedState
    }.fix()

    val getName: IO<String> = fx {
        putStrLn("What is your name: ").bind()
        val (name) = getStrLn()
        name
    }.fix()

    fun getChoice(): IO<Char> = fx {
        putStrLn("Please enter a letter").bind()
        val line = getStrLn().bind()
        val char = line.toLowerCase().trim().firstOption().fold(
                {
                    putStrLn("You did not enter a character")
                            .flatMap { getChoice() }
                },
                {
                    IO.just(it)
                }
        ).bind()
        char
    }.fix()

    fun nextInt(max: Int): IO<Int> = IO { Random.nextInt(max) }

    val chooseWord: IO<String> = fx {
        val rand = nextInt(dictionary.size).bind()
        dictionary[rand].liftIO().bind()
    }.fix()

    fun renderState(state: State): IO<Unit> {
        val word = state.word.toList().map { if (state.guesses.contains(it)) " $it " else "   " }
                .joinToString("")
        val line = state.word.map { " - " }.joinToString("")
        val guesses = "Guesses: ${state.guesses.toList().sorted().joinToString("")}"
        val text = "$word\n$line\n\n$guesses\n"
        return putStrLn(text)
    }
}

fun main() {
    Hangman.hangman.unsafeRunSync()
}