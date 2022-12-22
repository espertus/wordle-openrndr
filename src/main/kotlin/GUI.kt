import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import java.lang.Thread.sleep
import java.lang.Thread.yield
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val ENTER_LABEL = "ENTER"
private const val BACKSPACE_LABEL = "<-"
private const val MESSAGE_TIMEOUT = 3000L // ms

private fun String.splitChars() = toCharArray().map { it.toString() }
private fun String.isNumber() = all { it.isDigit() }
private fun Button.changeLabel(text: String) {
    label = text
    requestRedraw()
}

private fun Char.toKeyState() =
    when (this) {
        '*' -> KeyState.Right
        '+' -> KeyState.Mispositioned
        '.' -> KeyState.Unused
        else -> throw AssertionError("Unreachable code reached with: $this")
    }

private val lightGray = Color.RGBa(ColorRGBa(.827, .839, .855))
private val darkGray = Color.RGBa(ColorRGBa(.471, .486, .494))
private val yellow = Color.RGBa(ColorRGBa(.788, .706, .345))
private val green = Color.RGBa(ColorRGBa(.416, .667, .392))
private val red = Color.RGBa(ColorRGBa.RED)
private val white = Color.RGBa(ColorRGBa.WHITE)
private val black = Color.RGBa(ColorRGBa.BLACK)

enum class KeyState(val color: Color.RGBa) {
    Special(lightGray),
    Unknown(lightGray),
    Unused(darkGray),
    Mispositioned(yellow),
    Right(green),
    Invisible(white)
}

class GUI {
    var turn = 0
    val input = StringBuilder(WORD_LENGTH)
    var wordEntered = false // has a guess been entered?
    var gameOver = false

    lateinit var gridButtonMap: Map<String, Button>
    lateinit var grid: List<List<Button>>
    lateinit var announcementElement: Button
    val keyStates: MutableMap<String, KeyState> =
        mutableMapOf<String, KeyState>()
            .withDefault { key ->
                when {
                    key.isNumber() -> KeyState.Invisible
                    key.length > 1 -> KeyState.Special
                    else -> KeyState.Unknown
                }
            }

    private fun display(msg: String, color: Color.RGBa, timeout: Long = 0L) {
        announcementElement.apply {
            label = msg
            style?.color = color
            requestRedraw()
            if (timeout != 0L) {
                thread(start = true) {
                    sleep(timeout)
                    label = ""
                    requestRedraw()
                }
            }
        }
    }

    fun readGuess(): String {
        while (true) {
            while (!wordEntered) {
                yield() // consider using wait/notify pattern
            }
            val word = input.toString()
            wordEntered = false
            if (WordleGame.isLegalWord(word)) {
                input.clear()
                return word
            }
            showError("Not in word list")
        }
    }

    fun showFeedback(guess: String, matchString: String) {
        turn++
        for (i in guess.indices) {
            val keyState = matchString[i].toKeyState()

            // Update the grid.
            val button = grid[turn - 1][i]
            button.label = guess[i].toString() // in case we used AI
            button.style?.background = keyState.color
            button.style?.color = white
            button.requestRedraw()

            // Update the map and keyboard.
            update(guess[i].toString(), keyState)
        }
    }

    fun showError(msg: String) {
        display(msg, red, MESSAGE_TIMEOUT)
    }

    fun showLoss(secretWord: String) {
        display(secretWord, black)
        gameOver = true
    }

    fun showWin(numGuesses: Int) {
        display(WordleGame.getWinningResponse(numGuesses), black)
        gameOver = true
    }

    private fun update(s: String, newState: KeyState) {
        val oldState = keyStates.getValue(s)
        if (newState != oldState) {
            val button = gridButtonMap[s]
                ?: throw AssertionError("No entry in buttonMap for '$s'")
            when (newState) {
                KeyState.Right -> {
                    keyStates[s] = newState
                }
                KeyState.Mispositioned, KeyState.Unused -> {
                    if (oldState == KeyState.Unknown) {
                        keyStates[s] = newState
                    }
                }
                else -> throw IllegalStateException("Unexpected state: $newState")
            }
            button.style?.background = keyStates.getValue(s).color
            button.style?.color = white
            button.requestRedraw()
        }
    }
}

fun main(args: Array<String>) = application {
    configure {
        width = 400
        height = 650
    }

    lateinit var gui: GUI

    program {
        val keyboardRows = listOf(
            listOf("5") + "QWERTYUIOP".splitChars(),
            listOf("20") + "ASDFGHJKL".splitChars(),
            listOf(
                "5",
                ENTER_LABEL,
                "Z",
                "X",
                "C",
                "V",
                "B",
                "N",
                "M",
                BACKSPACE_LABEL
            )
        )

        // We need to define legal letters because isLetter() accepts letters
        // from different alphabets.
        val legalLetters: List<Char> = ('A'..'Z').toList()

        fun processText(text: String) {
            // If a letter was clicked on, add it to the grid.
            if (text.length == 1 && text[0] in legalLetters) {
                if (gui.input.length < WORD_LENGTH) {
                    gui.grid[gui.turn][gui.input.length].changeLabel(text)
                    gui.input.append(text)
                }
            } else if (text == ENTER_LABEL) {
                if (gui.input.length == WORD_LENGTH) {
                    gui.wordEntered = true
                } else {
                    gui.showError("Not enough letters")
                }
                // Consider displaying an error if enter pressed prematurely.
            } else if (text == BACKSPACE_LABEL) {
                if (!gui.wordEntered && gui.input.isNotEmpty()) {
                    gui.grid[gui.turn][gui.input.length - 1].changeLabel("")
                    gui.input.deleteAt(gui.input.length - 1)
                }
            }
        }

        keyboard.keyDown.listen {
            // A motion to adjourn is always in order.
            if (it.name == "escape") {
                // This *should* exit the program gracefully but doesn't always work.
                program.application.exit()
                // This will definitely end the program.
                exitProcess(0)
            }

            if (!gui.gameOver) {
                when (it.name) {
                    "enter" -> processText(ENTER_LABEL)
                    "backspace" -> processText(BACKSPACE_LABEL)
                    else -> if (it.name.length == 1) processText(it.name[0].uppercase()) else System.err.println(
                        "Can't handle ${it.name}"
                    )
                }
            }
        }

        val cm = controlManager {
            styleSheet(has class_ "horizontal") {
                paddingLeft = 10.px
                paddingTop = 0.px
                //background = Color.RGBa(ColorRGBa(1.0, 1.0, 1.0))

                // ----------------------------------------------
                // The next two lines produce a horizontal layout
                // ----------------------------------------------
                display = Display.FLEX
                flexDirection = FlexDirection.Row
                width = 100.percent
            }

            styleSheet(has class_ "grid") {
                paddingLeft = 10.px
                paddingTop = 0.px
                //background = Color.RGBa(ColorRGBa(1.0, 1.0, 1.0))

                // ----------------------------------------------
                // The next two lines produce a horizontal layout
                // ----------------------------------------------
                display = Display.FLEX
                flexDirection = FlexDirection.Row
                width = 100.percent

                background = white
                color = white
            }

            styleSheet((has type "button").and(has class_ "grid")) {
                fontSize = 30.px
                color = black
                background = white
                borderColor = darkGray
                borderWidth = .5.px
                width = 40.px
                height = 50.px
            }

            styleSheet(has type "h1") {
                marginTop = 10.px
                marginLeft = 7.px
                marginBottom = 0.px //10.px
                color = black
            }

            styleSheet((has type "button").and(has class_ "spacer")) {
                color = white
                background = white
                borderColor = white
            }

            layout {
                val topArea = div("horizontal") {
                    style = styleSheet {
                        paddingTop = 20.px
                        paddingBottom = 20.px
                    }
                    button {
                        style = styleSheet {
                            fontSize = 20.px
                            background = white
                            width = 100.percent
                            height = 30.px
                        }
                    }
                }

                val gridDivs = 0.until(MAX_TURNS).map { row: Int ->
                    div("grid") {
                        // Put 1 spacer on each row
                        button("spacer") {
                            style = styleSheet {
                                width = 50.px
                            }
                        }
                        0.until(WORD_LENGTH).map {
                            button("grid") {
                                label = ""
                                style = styleSheet {
                                    // Needed so we can change background color later
                                }
                            }
                        }
                    }
                }

                h1 { " " } // just for space

                val keyboardDivs = keyboardRows.map { row: List<String> ->
                    div("horizontal") {
                        row.forEachIndexed { i, word ->
                            button {
                                label = if (word.isNumber()) "" else word
                                style = styleSheet {
                                    color = Color.RGBa(ColorRGBa.BLACK)
                                    background =
                                        gui.keyStates.getValue(word).color
                                    width =
                                        if (word.isNumber()) word.toInt().px else
                                            when (word.length) {
                                                5 -> 50.px
                                                2 -> 35.px
                                                else -> 25.px
                                            }

                                }

                                events.clicked.listen {
                                    if (!gui.gameOver) {
                                        processText(it.source.label)
                                    }
                                }
                            }
                        }
                    }
                }
                run {
                    gui.announcementElement = topArea.children[0] as Button
                    gui.grid = gridDivs.map { gridDiv: Div ->
                        gridDiv.children.filterIsInstance<Button>()
                            .filter { it.label.isEmpty() }
                    }
                    val buttons: List<Button> = keyboardDivs.flatMap { div ->
                        div.children.filterIsInstance<Button>()
                    }
                    gui.gridButtonMap =
                        buttons.associateBy { button -> button.label }
                }
            }
        }
        extend(cm)
        extend {
            // Set background color
            drawer.clear(1.0, 1.0, 1.0, 1.0)
        }
    }

    run {
        try {
            thread(start = true) {
                gui = GUI()
                WordleGame.playGame(gui)
            }
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message ?: e.toString())
            exitProcess(1)
        }
    }
}
