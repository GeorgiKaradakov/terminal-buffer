package terminalbuffer

data class Attributes(
    val foreground: Color = Color.DEFAULT,
    val background: Color = Color.DEFAULT,
    val style: CellStyle = CellStyle.NORMAL,
)
