package com.example.logviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

data class LogEntry(
    val id: Int,
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
) {
    fun getLevelColor(): Color {
        return when (level) {
            "E" -> Color.Red
            "W" -> Color(0xFFFFA500) // Orange
            "I" -> Color.Green
            "D" -> Color.Blue
            "V" -> Color.Gray
            else -> Color.White
        }
    }

    fun getHighlightedMessage(query: String): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(message)
        val regex = Regex(query, RegexOption.IGNORE_CASE)
        return buildAnnotatedString {
            var lastIndex = 0
            regex.findAll(message).forEach { match ->
                append(message.substring(lastIndex, match.range.first))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, background = Color.Yellow)) {
                    append(match.value)
                }
                lastIndex = match.range.last + 1
            }
            append(message.substring(lastIndex))
        }
    }

    fun getHighlightedTag(query: String): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(tag)
        val regex = Regex(query, RegexOption.IGNORE_CASE)
        return buildAnnotatedString {
            var lastIndex = 0
            regex.findAll(tag).forEach { match ->
                append(tag.substring(lastIndex, match.range.first))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, background = Color.Yellow)) {
                    append(match.value)
                }
                lastIndex = match.range.last + 1
            }
            append(tag.substring(lastIndex))
        }
    }
}
