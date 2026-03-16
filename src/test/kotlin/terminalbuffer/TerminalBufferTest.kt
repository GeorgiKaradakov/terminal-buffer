package terminalbuffer

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("TerminalBuffer Complete Implementation Tests")
class TerminalBufferTest {
    private lateinit var buffer: TerminalBuffer

    @BeforeEach
    fun setUp() {
        buffer = TerminalBuffer(width = 15, height = 5, scrollbackSize = 100)
    }

    @Nested
    @DisplayName("Constructor Edge Cases")
    inner class ConstructorEdgeCases {
        @Test
        @DisplayName("Should handle minimum valid dimensions")
        fun `should handle minimum valid dimensions`() {
            val minBuffer = TerminalBuffer(width = 1, height = 1, scrollbackSize = 0)
            assertEquals(1, minBuffer.width)
            assertEquals(1, minBuffer.height)
            assertEquals(0, minBuffer.scrollbackSize)
            assertEquals(Coord(0, 0), minBuffer.cursor)
        }

        @Test
        @DisplayName("Should handle reasonable large dimensions")
        fun `should handle reasonable large dimensions`() {
            val largeBuffer = TerminalBuffer(width = 100, height = 50, scrollbackSize = 1000)
            assertEquals(100, largeBuffer.width)
            assertEquals(50, largeBuffer.height)
            assertEquals(1000, largeBuffer.scrollbackSize)
        }

        @Test
        @DisplayName("Should initialize with empty content")
        fun `should initialize with empty content`() {
            for (row in 0 until buffer.height) {
                for (col in 0 until buffer.width) {
                    val char = buffer.getCellChar(row, col)
                    assertEquals(' ', char) // Default should be space
                }
            }
        }
    }

    @Nested
    @DisplayName("WriteText Comprehensive Tests")
    inner class WriteTextComprehensiveTests {
        @Test
        @DisplayName("Should write text and move cursor to exact position")
        fun `should write text and move cursor to exact end position`() {
            buffer.writeText("Hello")
            assertEquals(Coord(0, 5), buffer.cursor)
            assertEquals("Hello", buffer.getLine(0).substring(0, 5))
        }

        @Test
        @DisplayName("should respect carriage return and overwrite existing content")
        fun `should overwrite content after carraige return`() {
          buffer.writeText("Hello")
          buffer.writeText("\rABC")

          assertEquals("ABClo", buffer.getLine(0).substring(0,5))
        }


        @Test
        @DisplayName("Should override existing content completely")
        fun `should override existing content completely`() {
            buffer.writeText("Original")
            buffer.moveCursorLeft(100)
            buffer.writeText("New")

            val line = buffer.getLine(0)
            assertEquals("Newginal", line.trimEnd())
        }

        @Test
        @DisplayName("Should handle text exactly at line boundary")
        fun `should handle text exactly at line boundary`() {
            val exactText = "123456789012345" // Exactly 10 characters for width 10
            buffer.writeText(exactText)
            // Cursor should be at end of line (9) or wrapped to next line (1,0)
            assertTrue(buffer.cursor == Coord(1, 0))
            assertEquals(exactText, buffer.getLine(0))
        }

        @Test
        @DisplayName("Should handle text overflow at line end")
        fun `should handle text overflow at line end`() {
            buffer.moveCursorRight(13) // Position at column 8
            buffer.writeText("LongText") // 8 characters, only 2 fit

            val line = buffer.getLine(0)
            assertEquals("Lo", line.substring(13, 15)) // Only first 2 chars fit
            // Cursor should be at end of line or wrap to next line
            assertTrue(buffer.cursor == Coord(1, 6))
        }

        @Test
        @DisplayName("Should handle newlines in text")
        fun `should handle newlines in text`() {
            buffer.writeText("Line1\nLine2")

            assertEquals("Line1", buffer.getLine(0).trim())
            assertEquals("Line2", buffer.getLine(1).trim())
            assertEquals(Coord(1, 5), buffer.cursor) // Should be after "Line2"
        }

        @Test
        @DisplayName("Should handle multiple newlines")
        fun `should handle multiple newlines`() {
            buffer.writeText("A\n\nB")

            assertEquals("A", buffer.getLine(0).trim())
            assertEquals("", buffer.getLine(1).trim()) // Empty line
            assertEquals("B", buffer.getLine(2).trim())
            assertEquals(Coord(2, 1), buffer.cursor)
        }

        @Test
        @DisplayName("Should handle empty string")
        fun `should handle empty string without moving cursor`() {
            val initialPos = buffer.cursor
            buffer.writeText("")
            assertEquals(initialPos, buffer.cursor)
        }

        @Test
        @DisplayName("Should handle very long single line text")
        fun `should handle very long single line text`() {
            val longText = "a".repeat(100)
            buffer.writeText(longText)

            // Should wrap or truncate appropriately
            val totalContent = buffer.getScreenContent()
            assertTrue(totalContent.contains("a"))

            // Cursor should be within valid bounds
            assertTrue(buffer.cursor.row < buffer.height)
            assertTrue(buffer.cursor.column < buffer.width)
        }

        @Test
        @DisplayName("Should write at every valid position")
        fun `should write at every valid position`() {
            for (row in 0 until buffer.height) {
                for (col in 0 until buffer.width) {
                    // Move to position
                    buffer.moveCursorUp(100)
                    buffer.moveCursorLeft(100)
                    buffer.moveCursorDown(row)
                    buffer.moveCursorRight(col)

                    buffer.writeText("X")
                    val char = buffer.getCellChar(row, col)
                    assertEquals('X', char)
                }
            }
        }
    }

    @Nested
    @DisplayName("InsertText Comprehensive Tests")
    inner class InsertTextComprehensiveTests {
        @Test
        @DisplayName("Should insert text without destroying existing content")
        fun `should insert text without destroying existing content`() {
            buffer = TerminalBuffer(width = 30, height = 5, scrollbackSize = 100)
            buffer.writeText("HelloWorld")
            buffer.moveCursorLeft(5) // Position after "Hello"
            buffer.insertText(" Beautiful ")

            val line = buffer.getLine(0)
            assertEquals("Hello Beautiful World", line.trim())
        }

        @Test
        @DisplayName("Should shift content to the right correctly")
        fun `should shift content to the right correctly`() {
            buffer.writeText("ABCDEF")
            buffer.moveCursorLeft(3) // Position at 'D'
            buffer.insertText("123")

            val line = buffer.getLine(0)
            assertEquals("ABC123DEF", line.trim())
            assertEquals(Coord(0, 6), buffer.cursor) // After inserted text
        }

        @Test
        @DisplayName("Should handle insertion at line start")
        fun `should handle insertion at line start`() {
            buffer.writeText("World")
            buffer.moveCursorLeft(100) // Go to start
            buffer.insertText("Hello ")

            assertEquals("Hello World", buffer.getLine(0).trim())
            assertEquals(Coord(0, 6), buffer.cursor)
        }

        @Test
        @DisplayName("Should handle insertion at line end")
        fun `should handle insertion at line end`() {
            buffer.writeText("Hello")
            buffer.insertText(" World")

            assertEquals("Hello World", buffer.getLine(0).trim())
            // Cursor should wrap to next line since "Hello World" is 11 chars > width 10
            assertTrue(buffer.cursor == Coord(0, 11))
        }

        @Test
        @DisplayName("Should handle insertion with newlines")
        fun `should handle insertion with newlines`() {
            buffer.writeText("AB")
            buffer.moveCursorLeft(1) // Position at 'B'
            buffer.insertText("X\nY")

            assertEquals("AXB", buffer.getLine(0).trim())
            assertEquals("Y", buffer.getLine(1).trim())
        }

        @Test
        @DisplayName("Should handle empty insertion")
        fun `should handle empty insertion`() {
            buffer.writeText("Test")
            val initialPos = buffer.cursor
            buffer.insertText("")

            assertEquals("Test", buffer.getLine(0).trim())
            assertEquals(initialPos, buffer.cursor)
        }

        @Test
        @DisplayName("Should handle insertion at screen bottom")
        fun `should handle insertion at screen bottom`() {
            buffer.moveCursorDown(4) // Bottom row
            buffer.writeText("Bottom")
            buffer.moveCursorLeft(3)
            buffer.insertText("MID")

            assertEquals("BotMIDtom", buffer.getLine(4).trim())
        }

        @Test
        @DisplayName("Should preserve content after insertion point")
        fun `should preserve content after insertion point`() {
            buffer.writeText("1234567890")
            buffer.moveCursorLeft(5) // Position at '6'
            buffer.insertText("ABC")

            val line = buffer.getLine(0)
            assertTrue(line.contains("12345ABC67890"))
        }
    }

    @Nested
    @DisplayName("FillLine Comprehensive Tests")
    inner class FillLineComprehensiveTests {
        @Test
        @DisplayName("Should fill entire line with character")
        fun `should fill entire line with character`() {
            buffer.moveCursorDown(2)
            buffer.moveCursorRight(3) // Position at (2,3)
            buffer.fillLine('*')

            val line = buffer.getLine(2)
            assertEquals("*".repeat(buffer.width), line)
            // Cursor row should stay the same
            assertEquals(2, buffer.cursor.row)
        }

        @Test
        @DisplayName("Should override all existing content on line")
        fun `should override all existing content on line`() {
            buffer = TerminalBuffer(width = 25, height = 5, scrollbackSize = 20)
            buffer.writeText("Existing content here")
            buffer.moveCursorLeft(5) // Move somewhere in the line
            buffer.fillLine('#')

            val line = buffer.getLine(0)
            assertEquals("#".repeat(buffer.width), line)
        }

        @Test
        @DisplayName("Should work regardless of cursor column position")
        fun `should work regardless of cursor column position`() {
            val testRow = 1
            buffer.moveCursorDown(testRow)

            // Test from different column positions
            val positions = listOf(0, buffer.width / 2, buffer.width - 1)
            val chars = listOf('A', 'B', 'C')

            positions.zip(chars).forEach { (col, char) ->
                buffer.moveCursorLeft(100)
                buffer.moveCursorRight(col)
                buffer.fillLine(char)

                val line = buffer.getLine(testRow)
                assertEquals(char.toString().repeat(buffer.width), line)
            }
        }

        @Test
        @DisplayName("Should handle special characters")
        fun `should handle special characters`() {
            val specialChars = listOf('█', '▓', '░', '◆', '◇', '●', '○', '♦', '♠', '♣')

            specialChars.forEachIndexed { index, char ->
                if (index < buffer.height) {
                    buffer.moveCursorUp(100)
                    buffer.moveCursorDown(index)
                    buffer.fillLine(char)

                    val line = buffer.getLine(index)
                    assertEquals(char.toString().repeat(buffer.width), line)
                }
            }
        }

        @Test
        @DisplayName("Should handle null character")
        fun `should handle null character`() {
            buffer.fillLine('\u0000')
            val line = buffer.getLine(0)
            assertEquals(" ".repeat(buffer.width), line)
        }

        @Test
        @DisplayName("Should fill all lines independently")
        fun `should fill all lines independently`() {
            for (row in 0 until buffer.height) {
                buffer.moveCursorUp(100)
                buffer.moveCursorDown(row)
                buffer.fillLine(('0' + row).toChar())
            }

            for (row in 0 until buffer.height) {
                val expectedChar = ('0' + row).toChar()
                val line = buffer.getLine(row)
                assertEquals(expectedChar.toString().repeat(buffer.width), line)
            }
        }

        @Test
        @DisplayName("Should not affect other lines")
        fun `should not affect other lines`() {
            // Set up content on all lines
            for (row in 0 until buffer.height) {
                buffer.moveCursorUp(100)
                buffer.moveCursorDown(row)
                buffer.moveCursorLeft(buffer.cursor.column)
                buffer.writeText("Line$row")
            }

            // Fill middle line
            val targetRow = buffer.height / 2
            buffer.moveCursorUp(100)
            buffer.moveCursorDown(targetRow)
            buffer.fillLine('X')

            // Check other lines are unchanged
            for (row in 0 until buffer.height) {
                val line = buffer.getLine(row)
                if (row == targetRow) {
                    assertEquals("X".repeat(buffer.width), line)
                } else {
                    assertTrue(line.startsWith("Line$row"))
                }
            }
        }
    }

    @Nested
    @DisplayName("Content Access Comprehensive Tests")
    inner class ContentAccessComprehensiveTests {
        @Test
        @DisplayName("Should get correct cell character at all positions")
        fun `should get correct cell character at all positions`() {
            // Fill buffer with known pattern
            for (row in 0 until buffer.height) {
                for (col in 0 until buffer.width) {
                    buffer.moveCursorUp(100)
                    buffer.moveCursorLeft(100)
                    buffer.moveCursorDown(row)
                    buffer.moveCursorRight(col)
                    val char = ('A' + (row * buffer.width + col) % 26).toChar()
                    buffer.writeText(char.toString())
                }
            }

            // Verify all positions
            for (row in 0 until buffer.height) {
                for (col in 0 until buffer.width) {
                    val expectedChar = ('A' + (row * buffer.width + col) % 26).toChar()
                    val char = buffer.getCellChar(row, col)
                    assertEquals(expectedChar, char)
                }
            }
        }

        @Test
        @DisplayName("Should get correct attributes at all positions")
        fun `should get correct attributes at all positions`() {
            for (row in 0 until buffer.height) {
                for (col in 0 until buffer.width) {
                    val attrs = buffer.getCellAttributes(row, col)
                    assertNotNull(attrs)
                    // Default attributes should be consistent
                    assertNotNull(attrs.foreground)
                    assertNotNull(attrs.background)
                    assertNotNull(attrs.style)
                }
            }
        }

        @Test
        @DisplayName("Should throw exception for out-of-bounds coordinates")
        fun `should throw exception for out-of-bounds coordinates`() {
            val outOfBounds =
                listOf(
                    Coord(-1, 0),
                    Coord(0, -1),
                    Coord(-1, -1),
                    Coord(buffer.height, 0),
                    Coord(0, buffer.width),
                    Coord(buffer.height, buffer.width),
                    Coord(1000, 1000),
                    Coord(-1000, -1000),
                )

            outOfBounds.forEach { coord ->
                assertThrows<IndexOutOfBoundsException> {
                    buffer.getCellChar(coord.row, coord.column)
                }
                assertThrows<IndexOutOfBoundsException> {
                    buffer.getCellAttributes(coord.row, coord.column)
                }
            }
        }

        @Test
        @DisplayName("Should get correct line content")
        fun `should get correct line content`() {
            val testLines =
                listOf(
                    "First", // 5 chars - fits in width 10
                    "Second", // 6 chars - fits in width 10  
                    "Third", // 5 chars - fits in width 10
                    "", // empty line
                    "Last", // 4 chars - fits in width 10
                )

            testLines.forEachIndexed { index, content ->
                if (index < buffer.height) {
                    buffer.moveCursorUp(100)
                    buffer.moveCursorLeft(100)
                    buffer.moveCursorDown(index)
                    buffer.writeText(content)
                }
            }

            testLines.forEachIndexed { index, expected ->
                if (index < buffer.height) {
                    val actual = buffer.getLine(index).trim()
                    assertEquals(expected, actual)
                }
            }
        }

        @Test
        @DisplayName("Should throw exception for invalid line indices")
        fun `should throw exception for invalid line indices`() {
            val invalidIndices = listOf(-1, -100, buffer.height, buffer.height + 1, 1000)

            invalidIndices.forEach { index ->
                assertThrows<IndexOutOfBoundsException> {
                    buffer.getLine(index)
                }
            }
        }

        @Test
        @DisplayName("Should get complete screen content")
        fun `should get complete screen content`() {
            // Write distinctive content to each line
            for (row in 0 until buffer.height) {
                buffer.moveCursorUp(100)
                buffer.moveCursorLeft(100)
                buffer.moveCursorDown(row)
                buffer.writeText("Row$row")
            }

            val screenContent = buffer.getScreenContent()
            assertNotNull(screenContent)

            // Should contain all row content
            for (row in 0 until buffer.height) {
                assertTrue(screenContent.contains("Row$row"))
            }
        }

        @Test
        @DisplayName("Should preserve line endings in screen content")
        fun `should preserve line structure in screen content`() {
            buffer.writeText("Line1")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("Line2")

            val content = buffer.getScreenContent()
            // Should maintain line structure (newlines or consistent formatting)
            assertTrue(content.indexOf("Line1") < content.indexOf("Line2"))
        }
    }

    @Nested
    @DisplayName("Scrollback Functionality Tests")
    inner class ScrollbackTests {
        @Test
        @DisplayName("Should maintain scrollback when lines scroll off")
        fun `should maintain scrollback when lines scroll off screen`() {
            // Fill buffer completely
            for (row in 0 until buffer.height) {
                buffer.moveCursorUp(100)
                buffer.moveCursorLeft(100)
                buffer.moveCursorDown(row)
                buffer.writeText("OriginalLine$row")
            }

            // Add more lines to cause scrolling
            for (i in 0 until 3) {
                buffer.moveCursorDown(100) // Go to bottom
                buffer.moveCursorLeft(100)
                buffer.writeText("\nNewLine$i") // This should cause scrolling
            }

            // All content should include original lines in scrollback
            val allContent = buffer.getAllContent()
            assertTrue(allContent.contains("OriginalLine0"))
        }

        @Test
        @DisplayName("Should respect scrollback size limit")
        fun `should respect scrollback size limit`() {
            val smallBuffer = TerminalBuffer(width = 10, height = 3, scrollbackSize = 2)

            // Add many lines beyond scrollback capacity
            for (i in 0 until 10) {
                smallBuffer.moveCursorDown(100)
                smallBuffer.moveCursorLeft(100)
                smallBuffer.writeText("\nLine$i")
            }

            val allContent = smallBuffer.getAllContent()
            // Should not contain very old lines (beyond scrollback limit)
            assertFalse(allContent.contains("Line0"))
            assertFalse(allContent.contains("Line1"))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    inner class EdgeCasesTests {
        @Test
        @DisplayName("Should handle operations at exact boundaries")
        fun `should handle operations at exact screen boundaries`() {
            val corners =
                listOf(
                    Coord(0, 0), // Top-left
                    Coord(0, buffer.width - 1), // Top-right
                    Coord(buffer.height - 1, 0), // Bottom-left
                    Coord(buffer.height - 1, buffer.width - 1), // Bottom-right
                )

            corners.forEachIndexed { index, corner ->
                // Move to corner
                buffer.moveCursorUp(100)
                buffer.moveCursorLeft(100)
                buffer.moveCursorDown(corner.row)
                buffer.moveCursorRight(corner.column)

                assertEquals(corner, buffer.cursor)

                // Perform all operations at corner
                buffer.writeText("C$index")
                val char = buffer.getCellChar(corner.row, corner.column)
                assertEquals('C', char)

                buffer.insertText("I")
                buffer.fillLine(('A' + index).toChar())

                // Verify cursor remains in bounds
                assertTrue(buffer.cursor.row >= 0)
                assertTrue(buffer.cursor.row < buffer.height)
                assertTrue(buffer.cursor.column >= 0)
                assertTrue(buffer.cursor.column < buffer.width)
            }
        }

        @Test
        @DisplayName("Should handle maximum length text operations")
        fun `should handle maximum length text operations`() {
            val maxText = "X".repeat(buffer.width * buffer.height)
            buffer.writeText(maxText)

            // Should handle gracefully without crashing
            val allContent = buffer.getAllContent()
            assertTrue(allContent.contains("X"))
        }

        @Test
        @DisplayName("Should handle rapid successive operations")
        fun `should handle rapid successive operations`() {
            repeat(1000) { i ->
                val op = i % 4
                when (op) {
                    0 -> {
                        buffer.writeText("W$i")
                    }

                    1 -> {
                        buffer.insertText("I$i")
                    }

                    2 -> {
                        buffer.fillLine(('A' + i % 26).toChar())
                    }

                    3 -> {
                        buffer.moveCursorDown(i % buffer.height)
                        buffer.moveCursorRight(i % buffer.width)
                    }
                }

                // Verify state consistency
                assertTrue(buffer.cursor.row >= 0)
                assertTrue(buffer.cursor.row < buffer.height)
                assertTrue(buffer.cursor.column >= 0)
                assertTrue(buffer.cursor.column < buffer.width)
            }
        }

        @Test
        @DisplayName("Should maintain buffer integrity after complex operations")
        fun `should maintain buffer integrity after complex operations`() {
            // Perform complex sequence of operations
            buffer.writeText("Start")
            buffer.insertText(" Middle")
            buffer.writeText(" End")
            buffer.moveCursorDown(1)
            buffer.fillLine('=')
            buffer.moveCursorDown(1)
            buffer.writeText("Line with\nnewlines\nhere")

            // Verify all content access still works
            for (row in 0 until buffer.height) {
                assertDoesNotThrow { buffer.getLine(row) }
                for (col in 0 until buffer.width) {
                    assertDoesNotThrow { buffer.getCellChar(row, col) }
                    assertDoesNotThrow { buffer.getCellAttributes(row, col) }
                }
            }

            assertDoesNotThrow { buffer.getScreenContent() }
            assertDoesNotThrow { buffer.getAllContent() }
        }

        @Test
        @DisplayName("Should handle empty buffer operations consistently")
        fun `should handle operations on completely empty buffer`() {
            val emptyBuffer = TerminalBuffer(width = 10, height = 5, scrollbackSize = 0)

            // All operations should work on empty buffer
            emptyBuffer.writeText("")
            emptyBuffer.insertText("")
            emptyBuffer.fillLine(' ')

            // Content access should return consistent results (may be spaces, not necessarily empty)
            val screenContent = emptyBuffer.getScreenContent()
            val allContent = emptyBuffer.getAllContent()
            assertNotNull(screenContent)
            assertNotNull(allContent)

            for (row in 0 until emptyBuffer.height) {
                val line = emptyBuffer.getLine(row)
                assertTrue(line.trim().isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("Integration and Complex Scenarios")
    inner class IntegrationTests {
        @Test
        @DisplayName("Should handle complete document editing workflow")
        fun `should handle complete document editing workflow`() {
            buffer = TerminalBuffer(width = 50, height = 20, scrollbackSize = 100)
            // Create a document
            buffer.writeText("DOCUMENT TITLE")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.fillLine('=')

            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("Chapter 1: Introduction")

            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("This is the content.")

            // Edit the document
            buffer.moveCursorUp(1)
            buffer.moveCursorLeft(100)
            buffer.moveCursorRight(8) // After "Chapter "
            buffer.insertText("One: ")

            // Verify final state
            assertTrue(buffer.getLine(0).contains("DOCUMENT TITLE"))
            assertTrue(buffer.getLine(1).contains("="))
            assertTrue(buffer.getLine(2).contains("Chapter One: 1: Introduction"))
            assertTrue(buffer.getLine(3).contains("This is the content."))
        }

        @Test
        @DisplayName("Should handle terminal-like input simulation")
        fun `should handle terminal-like input simulation`() {
            buffer = TerminalBuffer(width = 55, height = 10, scrollbackSize = 100)
          
            // Simulate terminal prompt
            buffer.writeText("$ ls -la")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)

            // Simulate command output
            val files =
                listOf(
                    "drwxr-xr-x 2 user user  4096 Jan 15 10:30 .",
                    "drwxr-xr-x 3 user user  4096 Jan 15 10:29 ..",
                    "-rw-r--r-- 1 user user   220 Jan 15 10:30 file.txt",
                )

            files.forEach { file ->
                buffer.writeText(file)
                buffer.moveCursorDown(1)
                buffer.moveCursorLeft(100)
            }

            // Add new prompt
            buffer.writeText("$ ")

            // Verify content structure
            val screenContent = buffer.getScreenContent()
            assertTrue(screenContent.contains("$ ls -la"))
            assertTrue(screenContent.contains("file.txt"))
            assertTrue(screenContent.contains("$ "))
        }

        @Test
        @DisplayName("Should handle code editor simulation")
        fun `should handle code editor simulation`() {
          buffer = TerminalBuffer(width = 25, height = 10, scrollbackSize = 100)
            // Write code
            buffer.writeText("function hello() {")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("  console.log('Hello');")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("}")

            // Edit code - insert parameter
            buffer.moveCursorUp(2) // Go to function line
            buffer.moveCursorLeft(100)
            buffer.moveCursorRight(15) // After "function hello"
            buffer.insertText("name")

            // Edit string
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.moveCursorRight(20) // Inside string
            buffer.writeText(", ' + name)")

            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.writeText("} // End of function")


            // Verify code structure
            assertTrue(buffer.getLine(0).contains("function hello(name) {", true))
            assertTrue(buffer.getLine(1).contains("console.log", true))
            assertTrue(buffer.getLine(2).contains("name", true))
            assertTrue(buffer.getLine(3).contains("}", true))
        }

        @Test
        @DisplayName("Should handle table creation and editing")
        fun `should handle table creation and editing`() {
            buffer = TerminalBuffer(width = 31, height = 10, scrollbackSize = 100)
            // Create table header
            buffer.writeText("| Name     | Age | City     |")
            buffer.moveCursorDown(1)
            buffer.moveCursorLeft(100)
            buffer.fillLine('-')

            // Add table rows
            val rows =
                listOf(
                    "| Alice    | 30  | New York |",
                    "| Bob      | 25  | London   |",
                    "| Charlie  | 35  | Tokyo    |",
                )

            rows.forEach { row ->
                buffer.moveCursorDown(1)
                buffer.moveCursorLeft(100)
                buffer.writeText(row)
            }

            // Edit a cell
            buffer.moveCursorUp(1) // Go to Bob's row
            buffer.moveCursorLeft(100)
            buffer.moveCursorRight(12) // Position at age
            buffer.writeText("26")

            // Verify table structure
            assertTrue(buffer.getLine(0).contains("Name"))
            assertTrue(buffer.getLine(1).contains("-"))
            assertTrue(buffer.getLine(3).contains("26"))
        }

        @Test
        @DisplayName("Should handle line wrapping edge cases")
        fun `should handle line wrapping edge cases`() {
            // Test wrapping at exact boundary
            val exactFit = "A".repeat(buffer.width)
            buffer.writeText(exactFit)
            // Cursor should be at end of line or wrapped to next line
            assertTrue(buffer.cursor.column == buffer.width - 1 || buffer.cursor == Coord(1, 0))

            // Add one more character - should wrap
            buffer.writeText("B")
            assertEquals(Coord(1, 1), buffer.cursor)
            assertEquals("B", buffer.getLine(1).substring(0, 1))
        }

        @Test
        @DisplayName("Should handle cursor movement after text operations")
        fun `should handle cursor movement after text operations`() {
            // writeText should move cursor
            buffer.writeText("Hello")
            assertEquals(5, buffer.cursor.column)

            // insertText should move cursor
            buffer.moveCursorLeft(2)
            val beforeInsert = buffer.cursor.column
            buffer.insertText("XX")
            assertEquals(beforeInsert + 2, buffer.cursor.column)

            // fillLine cursor behavior
            val beforeFill = buffer.cursor
            buffer.fillLine('*')
            // Cursor row should stay same, column behavior may vary
            assertEquals(beforeFill.row, buffer.cursor.row)
        }
    }
}
