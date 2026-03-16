package terminalbuffer

class Cell {
    var char: Char = ' '
    var attributes: Attributes = Attributes(Color.DEFAULT, Color.DEFAULT, CellStyle.NORMAL)
}
