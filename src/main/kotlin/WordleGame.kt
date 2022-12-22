import java.io.File

const val WORD_LENGTH = 5
const val MAX_TURNS = 6

class WordleGame(private val secretWord: String) {
    private val guesses = mutableListOf<String>()
    val numGuesses
        get() = guesses.size
    val wordFound: Boolean
        get() = guesses.contains(secretWord)
    val isOver: Boolean
        get() = wordFound || numGuesses == MAX_TURNS

    /**
     * Accepts a [guess] and evaluates how closely it matches [secretWord].
     * This returns a feedback string the same length as [guess].
     * For every position where [guess] matches [secretWord], a star (*)
     * appears. For positions where the letter in [guess] corresponds to a
     * different position in [secretWord], a plus sign (+) appears. If there
     * is no match, a dot (.) appears.
     *
     * This throws [IllegalArgumentException] if the length of [guess] is different
     * from the length of [secretWord] or if [guess] contains any characters that
     * are not upper-case English letters.
     */
    fun receiveGuess(guess: String): String {
        require(guess.length == secretWord.length) {
            "Your guess was not $WORD_LENGTH letters long."
        }
        require(guess.all { it in 'A'..'Z' }) {
            "The guess must consist only of upper-case English letters."
        }
        if (!guesses.contains(guess)) {
            guesses.add(guess)
        }
        // Make a pass to identify letters in secretWord that are not in the corresponding
        // location of guess. This will give us a pool of letters that might be in the
        // wrong position of the guess.
        val letterPool: MutableList<Char> =
            guess.indices.mapNotNull { if (guess[it] != secretWord[it]) secretWord[it] else null }
                .toMutableList()

        return guess.indices.map { i ->
            when (guess[i]) {
                secretWord[i] -> '*'
                in letterPool -> {
                    letterPool.remove(guess[i])
                    '+'
                }
                else -> '.'
            }
        }.joinToString(separator = "")
    }

    companion object {
        private val WINNING_RESPONSES = listOf(
            "Genius",
            "Magnificent",
            "Impressive",
            "Splendid",
            "Great",
            "Phew"
        )

        fun getWinningResponse(numGuesses: Int) =
            if (numGuesses in 1..MAX_TURNS)
                WINNING_RESPONSES[numGuesses - 1]
            else
                WINNING_RESPONSES[MAX_TURNS - 1]

        private fun readWordsFromFile(filename: String): List<String> =
            File("data/dictionaries/$filename").readLines()

        fun playGame(gui: GUI) {
            // Actual Wordle solutions [secret-words.txt] from
            // https://medium.com/@owenyin/here-lies-wordle-2021-2027-full-answer-list-52017ee99e86
            val words = readWordsFromFile("secret-words.txt")
            val randomWord = words.random()
            val game = WordleGame(randomWord)

            for (turn in 0.until(MAX_TURNS)) {
                val guess = gui.readGuess()
                try {
                    val matchString = game.receiveGuess(guess)
                    gui.showFeedback(guess, matchString)
                    if (game.wordFound) {
                        gui.showWin(game.numGuesses)
                        return
                    }
                } catch (error: java.lang.IllegalArgumentException) {
                    gui.showError(error.message ?: "Error!")
                }
            }
            gui.showLoss(randomWord)
        }
    }
}
