import java.io.File

const val WORD_LENGTH = 5
const val MAX_TURNS = 6

class WordleGame(val secretWord: String) {
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
    fun makeGuess(guess: String): String {
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

        // Actual Wordle solutions [secret-words.txt] from
        // https://medium.com/@owenyin/here-lies-wordle-2021-2027-full-answer-list-52017ee99e86
        val secretWords = readWordsFromFile("secret-words.txt")
        private val legalWords = readWordsFromFile("legal-words.txt")

        fun isLegalWord(word: String) = legalWords.contains(word)

        fun getWinningResponse(numGuesses: Int) =
            WINNING_RESPONSES[MAX_TURNS.coerceAtMost(numGuesses) - 1]

        private fun readWordsFromFile(filename: String): List<String> =
            File("data/dictionaries/$filename").readLines()

        fun makeGame() = WordleGame(secretWords.random())
    }
}
