package com.trinhquyetchien.markdowntray.data

sealed class MarkdownNode {
    data class Header(val level: Int, val text: String, val lineIndex: Int) : MarkdownNode()
    data class Task(
        val lineIndex: Int,
        val text: String,
        val isChecked: Boolean,
        val indentLevel: Int
    ) : MarkdownNode()
    data class NormalText(val text: String, val lineIndex: Int) : MarkdownNode()
    object Divider : MarkdownNode()
}

object MarkdownParser {
    private val taskRegex = Regex("^(\\s*)[-*]\\s*\\[([ xX])\\]\\s*(.*)")
    private val headerRegex = Regex("^(#{1,6})\\s+(.*)")
    private val hrRegex = Regex("^(\\s*[-*_]\\s*){3,}$")

    fun parse(content: String): List<MarkdownNode> {
        val lines = content.lines()
        val nodes = mutableListOf<MarkdownNode>()

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val taskMatch = taskRegex.matchEntire(line)
            if (taskMatch != null) {
                val indent = taskMatch.groupValues[1].length / 2
                val checkChar = taskMatch.groupValues[2]
                val isChecked = checkChar.equals("x", ignoreCase = true)
                val taskText = taskMatch.groupValues[3]
                nodes.add(MarkdownNode.Task(index, taskText, isChecked, indent))
                continue
            }

            val headerMatch = headerRegex.matchEntire(trimmed)
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val headerText = headerMatch.groupValues[2]
                nodes.add(MarkdownNode.Header(level, headerText, index))
                continue
            }

            if (hrRegex.matches(trimmed)) {
                nodes.add(MarkdownNode.Divider)
                continue
            }

            nodes.add(MarkdownNode.NormalText(line, index))
        }

        return nodes
    }
}
