package com.summarizer.app.presentation.overlay

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 26.sp,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = parseMarkdown(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        style = style,
        letterSpacing = 0.5.sp
    )
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trimStart()
            val isBullet = trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ")
            
            if (isBullet) {
                // Retain some indentation
                val indentCount = line.length - trimmedLine.length
                append(" ".repeat(indentCount))
                append("•  ")
                parseInlineStyles(trimmedLine.substring(2))
                
                // Add slightly more spacing between list items if desired, but default is fine
            } else {
                val fullyTrimmed = line.trim()
                if (fullyTrimmed.startsWith("### ")) {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)) { 
                        parseInlineStyles(fullyTrimmed.substring(4))
                    }
                    if (index < lines.size - 1) append("\n") // Extra padding below heading
                } else if (fullyTrimmed.startsWith("**") && fullyTrimmed.endsWith("**") && fullyTrimmed.length > 4 && fullyTrimmed.indexOf("**", 2) == fullyTrimmed.length - 2) {
                    // Entire line is bold (acts as a heading)
                    if (index > 0) append("\n") // Extra space above
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)) {
                        parseInlineStyles(fullyTrimmed.substring(2, fullyTrimmed.length - 2))
                    }
                    if (index < lines.size - 1) append("\n") // Extra padding below heading
                } else {
                    parseInlineStyles(line)
                }
            }
            if (index < lines.size - 1) {
                append("\n")
                // Add extra spacing after non-empty lines for vastly improved readability
                if (line.isNotBlank() && lines[index+1].isNotBlank()) {
                    append("\n")
                }
            }
        }
    }
}

fun AnnotatedString.Builder.parseInlineStyles(text: String) {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified)) {
                    append(text.substring(i + 2, endIdx))
                }
                i = endIdx + 2
                continue
            }
        }
        if (text.startsWith("*", i) && (i == 0 || text[i-1] == ' ' || text[i-1] == '\n')) {
            val endIdx = text.indexOf("*", i + 1)
            if (endIdx != -1) {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(text.substring(i + 1, endIdx))
                }
                i = endIdx + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
