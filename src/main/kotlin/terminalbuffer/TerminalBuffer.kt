package terminalbuffer

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

    fun fillLine(character: Char) {}

    // content access
    fun getCellChar(pos: Coord): Cell {
        // TODO: Implement getCellChar
        return Cell()
    }

    fun getCellAttributes(pos: Coord): Attributes {
        // TODO: Implement getCellAttributes
        return Attributes(Color.DEFAULT, Color.DEFAULT, CellStyle.NORMAL)
    }

    fun getLine(lineInd: Int): String {
        // TODO: Implement getLine
        return ""
    }

    fun getScreenContent(): String {
        // TODO: Implement getScreenContent
        return ""
    }

    fun getAllContent(): String {
        // TODO: Implement getAllContent
        return ""
    }

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

data class Coord(
    val row: Int,
    val column: Int,
)
