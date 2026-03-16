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
    fun moveCursorUp(rows: Int) {}

    fun moveCursorDown(rows: Int) {}

    fun moveCursorRight(columns: Int) {}

    fun moveCursorLeft(columns: Int) {}

    // editing
    fun writeText(text: String) {}

    fun insertText(text: String) {}

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
}

data class Coord(
    val row: Int,
    val column: Int,
)
