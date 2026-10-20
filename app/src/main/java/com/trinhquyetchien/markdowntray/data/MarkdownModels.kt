package com.trinhquyetchien.markdowntray.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

sealed class MarkdownNode {
    data class Header(val level: Int, val text: String, val lineIndex: Int) : MarkdownNode()
    data class Task(
        val lineIndex: Int,
        val text: String,
        val isChecked: Boolean,
        val indentLevel: Int
    ) : MarkdownNode()
    data class BulletList(val lineIndex: Int, val text: String, val indentLevel: Int) : MarkdownNode()
    data class NumberedList(val lineIndex: Int, val number: String, val text: String, val indentLevel: Int) : MarkdownNode()
    data class BlockQuote(val lineIndex: Int, val text: String) : MarkdownNode()
    data class NormalText(val text: String, val lineIndex: Int) : MarkdownNode()
    object Divider : MarkdownNode()
}

object MarkdownParser {
    private val taskRegex = Regex("^(\\s*)[-*]\\s*\\[([ xX])\\]\\s*(.*)")
    private val headerRegex = Regex("^(#{1,6})\\s+(.*)")
    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)")
    private val numberedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)")
    private val quoteRegex = Regex("^>\\s*(.*)")
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

            val quoteMatch = quoteRegex.matchEntire(trimmed)
            if (quoteMatch != null) {
                nodes.add(MarkdownNode.BlockQuote(index, quoteMatch.groupValues[1]))
                continue
            }

            val bulletMatch = bulletRegex.matchEntire(line)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1].length / 2
                nodes.add(MarkdownNode.BulletList(index, bulletMatch.groupValues[2], indent))
                continue
            }

            val numMatch = numberedRegex.matchEntire(line)
            if (numMatch != null) {
                val indent = numMatch.groupValues[1].length / 2
                nodes.add(MarkdownNode.NumberedList(index, numMatch.groupValues[2], numMatch.groupValues[3], indent))
                continue
            }

            nodes.add(MarkdownNode.NormalText(line, index))
        }

        return nodes
    }
}

object MarkdownAnnotator {
    private val inlineTokenRegex = Regex(
        """(\*\*(.+?)\*\*)|(__([^_]+)__)|(\*([^*]+)\*)|(_([^_]+)_)|(`([^`]+)`)|(~~([^~]+)~~)|(\[([^\]]+)\]\(([^)]+)\))|(#[a-zA-Z0-9_\p{L}]+)"""
    )

    fun annotate(
        rawText: String,
        primaryColor: Color,
        secondaryColor: Color,
        codeBgColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            var cursor = 0
            for (match in inlineTokenRegex.findAll(rawText)) {
                if (match.range.first > cursor) {
                    append(rawText.substring(cursor, match.range.first))
                }

                val fullMatch = match.value
                when {
                    fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                        val inner = fullMatch.substring(2, fullMatch.length - 2)
                        val start = length
                        append(inner)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                    }
                    fullMatch.startsWith("__") && fullMatch.endsWith("__") -> {
                        val inner = fullMatch.substring(2, fullMatch.length - 2)
                        val start = length
                        append(inner)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                    }
                    fullMatch.startsWith("`") && fullMatch.endsWith("`") -> {
                        val inner = fullMatch.substring(1, fullMatch.length - 1)
                        val start = length
                        append(" $inner ")
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                fontWeight = FontWeight.Medium
                            ),
                            start,
                            length
                        )
                    }
                    fullMatch.startsWith("~~") && fullMatch.endsWith("~~") -> {
                        val inner = fullMatch.substring(2, fullMatch.length - 2)
                        val start = length
                        append(inner)
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                    }
                    fullMatch.startsWith("[") && fullMatch.contains("](") && fullMatch.endsWith(")") -> {
                        val label = fullMatch.substring(1, fullMatch.indexOf("]("))
                        val start = length
                        append(label)
                        addStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            ),
                            start,
                            length
                        )
                    }
                    fullMatch.startsWith("#") -> {
                        val start = length
                        append(fullMatch)
                        addStyle(
                            SpanStyle(
                                color = secondaryColor,
                                fontWeight = FontWeight.SemiBold
                            ),
                            start,
                            length
                        )
                    }
                    (fullMatch.startsWith("*") && fullMatch.endsWith("*")) ||
                    (fullMatch.startsWith("_") && fullMatch.endsWith("_")) -> {
                        val inner = fullMatch.substring(1, fullMatch.length - 1)
                        val start = length
                        append(inner)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                    }
                    else -> {
                        append(fullMatch)
                    }
                }
                cursor = match.range.last + 1
            }

            if (cursor < rawText.length) {
                append(rawText.substring(cursor))
            }
        }
    }
}
