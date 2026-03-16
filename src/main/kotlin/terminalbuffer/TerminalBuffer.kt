package terminalbuffer

data class Coord(
    val row: Int,
    val column: Int,
)

class TerminalBuffer(
    val width: Int,
    val height: Int,
    val scrollbackSize: Int,
) {
    private val screen: Array<Array<Cell>> = Array(height) { Array(width) { Cell() } }
    private val scrollback: ArrayDeque<Array<Cell>> = ArrayDeque()
    private var attributes: Attributes = Attributes()
    var cursor: Coord = Coord(0, 0)
        private set

    // Methods

    // Cursor
    fun moveCursorUp(rows: Int) {
        require(rows >= 0) { "Rows must be non-negative" }
        cursor = cursor.copy(row = (cursor.row - rows).coerceAtLeast(0))
    }

    fun moveCursorDown(rows: Int) {
        require(rows >= 0) { "Rows must be non-negative" }
        cursor = cursor.copy(row = (cursor.row + rows).coerceAtMost(height - 1))
    }

    fun moveCursorLeft(columns: Int) {
        require(columns >= 0) { "Columns must be non-negative" }
        cursor = cursor.copy(column = (cursor.column - columns).coerceAtLeast(0))
    }

    fun moveCursorRight(columns: Int) {
        require(columns >= 0) { "Columns must be non-negative" }
        cursor = cursor.copy(column = (cursor.column + columns).coerceAtMost(width - 1))
    }

    // editing
    fun writeText(text: String) {
        for (char in text) {
            if (char == '\n') {
                advanceLine()
            } else if (char == '\r') {
                moveCursorLeft(cursor.column)
            } else {
                screen[cursor.row][cursor.column].char = char
                screen[cursor.row][cursor.column].attributes = attributes

                if (cursor.column + 1 >= width) {
                    advanceLine()
                } else {
                    moveCursorRight(1)
                }
            }
        }
    }

    fun insertText(text: String) {
        for (char in text) {
            if (char == '\n') {
                advanceLine()
            } else if (char == '\r') {
                moveCursorLeft(cursor.column)
            } else {
                var overflow: Char = char
                var tempCursor: Coord = cursor.copy()

                while (overflow != '\u0000') {
                    val currentCell = screen[tempCursor.row][tempCursor.column]
                    val temp = currentCell.char
                    currentCell.char = overflow
                    currentCell.attributes = attributes
                    overflow = temp

                    if (tempCursor.column + 1 >= width) {
                        if (tempCursor.row + 1 >= height) {
                            scroll()
                            tempCursor = tempCursor.copy(row = height - 1, column = 0)
                        } else {
                            tempCursor = tempCursor.copy(row = tempCursor.row + 1, column = 0)
                        }
                    } else {
                        tempCursor = tempCursor.copy(column = tempCursor.column + 1)
                    }
                }

                if (cursor.column + 1 >= width) {
                    advanceLine()
                } else {
                    moveCursorRight(1)
                }
            }
        }
    }

    fun fillLine(character: Char = '\u0000') {
        for (col in 0 until width) {
            screen[cursor.row][col].char = character
            screen[cursor.row][col].attributes = attributes
        }
    }

    fun clearScreen() {
        screen.forEach { line ->
            line.forEach { cell ->
                cell.char = '\u0000'
                cell.attributes = Attributes()
            }
        }

        cursor = cursor.copy(row = 0, column = 0)
    }

    fun clearScrollbackAndScreen() {
        scrollback.clear()
        clearScreen()
    }

    // content access
    fun getCellChar(
        row: Int,
        col: Int,
    ): Char {
        if (row < 0 || row >= height + scrollback.size || col < 0 || col >= width) {
            throw IndexOutOfBoundsException("Index out of bounds: row=$row col=$col")
        }

        val outChar = if (row < scrollback.size) scrollback[row][col].char else screen[row - scrollback.size][col].char
        return if (outChar == '\u0000') ' ' else outChar
    }

    fun getCellAttributes(
        row: Int,
        col: Int,
    ): Attributes {
        if (row < 0 || row >= height + scrollback.size || col < 0 || col >= width) {
            throw IndexOutOfBoundsException("Index out of bounds: row=$row col=$col")
        }

        return if (row < scrollback.size) scrollback[row][col].attributes.copy() else screen[row - scrollback.size][col].attributes.copy()
    }

    fun getLine(lineInd: Int): String {
        if (lineInd < 0 || lineInd >= height + scrollback.size) {
            throw IndexOutOfBoundsException("Line index out of bounds: lineInd=$lineInd")
        }

        return if (lineInd < scrollback.size) {
            scrollback[lineInd].joinToString("") { cell -> if (cell.char == '\u0000') " " else cell.char.toString() }
        } else {
            screen[lineInd - scrollback.size].joinToString("") { cell -> if (cell.char == '\u0000') " " else cell.char.toString() }
        }
    }

    fun getScreenContent(): String =
        screen.joinToString("\n") { line ->
            line.joinToString("") { cell -> if (cell.char == '\u0000') " " else cell.char.toString() }
        }

    // TODO: Fix the newline at the beginning when scrollback is empty
    fun getAllContent(): String =
        (
            scrollback.joinToString("\n") { line ->
                line.joinToString("") { cell -> if (cell.char == '\u0000') " " else cell.char.toString() }
            } + "\n" + getScreenContent()
        ).trimEnd()

    // utility functions
    private fun shiftLines() {
        if (scrollback.size >= scrollbackSize) {
            scrollback.removeFirst()
        }

        scrollback.addLast(screen[0])
        for (i in 0 until height - 1) {
            screen[i] = screen[i + 1]
        }
    }

    private fun scroll() {
        shiftLines()
        screen[height - 1] = Array(width) { Cell() }
    }

    private fun clearLastLineAndMoveCursor() {
        scroll()
        cursor = cursor.copy(row = height - 1, column = 0)
    }

    private fun advanceLine() {
        if (cursor.row + 1 >= height) {
            clearLastLineAndMoveCursor()
        } else {
            moveCursorDown(1)
            moveCursorLeft(cursor.column)
        }
    }
}
