package com.trinhquyetchien.markdowntray.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseHeaders() {
        val md = """
            # Header 1
            ## Header 2
            ### Header 3
        """.trimIndent()

        val nodes = MarkdownParser.parse(md)
        assertEquals(3, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.Header && (nodes[0] as MarkdownNode.Header).level == 1)
        assertTrue(nodes[1] is MarkdownNode.Header && (nodes[1] as MarkdownNode.Header).level == 2)
        assertTrue(nodes[2] is MarkdownNode.Header && (nodes[2] as MarkdownNode.Header).level == 3)
    }

    @Test
    fun testParseTasks() {
        val md = """
            - [ ] Task unchecked
            - [x] Task checked
            * [X] Task uppercase checked
              - [ ] Indented task
        """.trimIndent()

        val nodes = MarkdownParser.parse(md)
        assertEquals(4, nodes.size)

        val task1 = nodes[0] as MarkdownNode.Task
        assertEquals("Task unchecked", task1.text)
        assertEquals(false, task1.isChecked)
        assertEquals(0, task1.indentLevel)

        val task2 = nodes[1] as MarkdownNode.Task
        assertEquals("Task checked", task2.text)
        assertEquals(true, task2.isChecked)

        val task3 = nodes[2] as MarkdownNode.Task
        assertEquals("Task uppercase checked", task3.text)
        assertEquals(true, task3.isChecked)

        val task4 = nodes[3] as MarkdownNode.Task
        assertEquals("Indented task", task4.text)
        assertEquals(1, task4.indentLevel)
    }

    @Test
    fun testParseListAndQuotes() {
        val md = """
            - Bullet item
            1. Numbered item
            > Quote here
            ---
        """.trimIndent()

        val nodes = MarkdownParser.parse(md)
        assertEquals(4, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.BulletList)
        assertTrue(nodes[1] is MarkdownNode.NumberedList)
        assertTrue(nodes[2] is MarkdownNode.BlockQuote)
        assertTrue(nodes[3] is MarkdownNode.Divider)
    }
}
