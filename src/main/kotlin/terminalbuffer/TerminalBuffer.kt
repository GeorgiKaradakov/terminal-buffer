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

    // ========================================================================================
    // CURSOR MOVEMENT METHODS
    // ========================================================================================

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

    // ========================================================================================
    // TEXT EDITING METHODS
    // ========================================================================================

    fun writeText(text: String) {
        for (char in text) {
            when (char) {
                '\n' -> {
                    advanceCursorToNextLine()
                }

                '\r' -> {
                    moveCursorToStartOfLine()
                }

                else -> {
                    writeCharacterAtCursor(char)
                    advanceCursorAfterWrite()
                }
            }
        }
    }

    fun insertText(text: String) {
        for (char in text) {
            when (char) {
                '\n' -> {
                    advanceCursorToNextLine()
                }

                '\r' -> {
                    moveCursorToStartOfLine()
                }

                else -> {
                    insertCharacterAtCursor(char)
                    advanceCursorAfterWrite()
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

    // ========================================================================================
    // SCREEN MANIPULATION METHODS
    // ========================================================================================

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

    // ========================================================================================
    // CONTENT ACCESS METHODS
    // ========================================================================================

    fun getCellChar(
        row: Int,
        col: Int,
    ): Char {
        validateCoordinates(row, col)

        val actualChar =
            if (isPositionInScrollback(row)) {
                scrollback[row][col].char
            } else {
                screen[convertToScreenRow(row)][col].char
            }

        return if (actualChar == '\u0000') ' ' else actualChar
    }

    fun getCellAttributes(
        row: Int,
        col: Int,
    ): Attributes {
        validateCoordinates(row, col)

        return if (isPositionInScrollback(row)) {
            scrollback[row][col].attributes.copy()
        } else {
            screen[convertToScreenRow(row)][col].attributes.copy()
        }
    }

    fun getLine(lineIndex: Int): String {
        validateLineIndex(lineIndex)

        return if (isLineInScrollback(lineIndex)) {
            convertLineToString(scrollback[lineIndex])
        } else {
            convertLineToString(screen[convertToScreenRow(lineIndex)])
        }
    }

    fun getScreenContent(): String = screen.joinToString("\n") { line -> convertLineToString(line) }

    fun getAllContent(): String {
        val scrollbackContent = scrollback.joinToString("\n") { line -> convertLineToString(line) }
        val screenContent = getScreenContent()

        return (scrollbackContent + "\n" + screenContent).trimEnd()
    }

    // ========================================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================================

    private fun validateCoordinates(
        row: Int,
        col: Int,
    ) {
        if (row < 0 || row >= height + scrollback.size || col < 0 || col >= width) {
            throw IndexOutOfBoundsException("Index out of bounds: row=$row col=$col")
        }
    }

    private fun validateLineIndex(lineIndex: Int) {
        if (lineIndex < 0 || lineIndex >= height + scrollback.size) {
            throw IndexOutOfBoundsException("Line index out of bounds: lineInd=$lineIndex")
        }
    }

    private fun isPositionInScrollback(row: Int): Boolean = row < scrollback.size

    private fun isLineInScrollback(lineIndex: Int): Boolean = lineIndex < scrollback.size

    private fun convertToScreenRow(globalRow: Int): Int = globalRow - scrollback.size

    private fun convertLineToString(line: Array<Cell>): String =
        line.joinToString("") { cell ->
            if (cell.char == '\u0000') " " else cell.char.toString()
        }

    private fun writeCharacterAtCursor(char: Char) {
        screen[cursor.row][cursor.column].char = char
        screen[cursor.row][cursor.column].attributes = attributes
    }

    private fun insertCharacterAtCursor(insertedChar: Char) {
        var overflowChar: Char = insertedChar
        var tempCursor: Coord = cursor.copy()

        // Shift characters to the right, handling overflow
        while (overflowChar != '\u0000') {
            val currentCell = screen[tempCursor.row][tempCursor.column]
            val existingChar = currentCell.char

            // Replace current character with overflow character
            currentCell.char = overflowChar
            currentCell.attributes = attributes
            overflowChar = existingChar

            // Move to next position, wrapping lines as needed
            tempCursor = getNextPositionForInsertion(tempCursor)
        }
    }

    private fun getNextPositionForInsertion(currentPos: Coord): Coord =
        if (currentPos.column + 1 >= width) {
            // Need to wrap to next line
            if (currentPos.row + 1 >= height) {
                // Need to scroll
                scroll()
                Coord(row = height - 1, column = 0)
            } else {
                Coord(row = currentPos.row + 1, column = 0)
            }
        } else {
            // Move to next column
            Coord(row = currentPos.row, column = currentPos.column + 1)
        }

    private fun advanceCursorAfterWrite() {
        if (cursor.column + 1 >= width) {
            advanceCursorToNextLine()
        } else {
            moveCursorRight(1)
        }
    }

    private fun moveCursorToStartOfLine() {
        moveCursorLeft(cursor.column)
    }

    private fun advanceCursorToNextLine() {
        if (cursor.row + 1 >= height) {
            scrollScreenAndPositionCursor()
        } else {
            moveToStartOfNextLine()
        }
    }

    private fun moveToStartOfNextLine() {
        moveCursorDown(1)
        moveCursorLeft(cursor.column)
    }

    private fun scrollScreenAndPositionCursor() {
        scroll()
        cursor = cursor.copy(row = height - 1, column = 0)
    }

    // ========================================================================================
    // SCROLLING AND BUFFER MANAGEMENT
    // ========================================================================================

    private fun shiftLinesUpToScrollback() {
        // Ensure scrollback doesn't exceed size limit
        if (scrollback.size >= scrollbackSize) {
            scrollback.removeFirst()
        }

        // Move top screen line to scrollback
        scrollback.addLast(screen[0])

        // Shift all screen lines up by one position
        for (lineIndex in 0 until height - 1) {
            screen[lineIndex] = screen[lineIndex + 1]
        }
    }

    private fun scroll() {
        shiftLinesUpToScrollback()
        screen[height - 1] = Array(width) { Cell() }
    }
}

