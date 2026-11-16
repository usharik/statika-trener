package cz.usharik.statika.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/** Značka „R_Ax“ → kurzívou R se spodním indexem Ax. */
fun AnnotatedString.Builder.appendSym(s: String, color: Color? = null) {
    val i = s.indexOf('_')
    val main = if (i < 0) s else s.substring(0, i)
    val sub = if (i < 0) "" else s.substring(i + 1)
    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color ?: Color.Unspecified)) { append(main) }
    if (sub.isNotEmpty()) {
        withStyle(SpanStyle(baselineShift = BaselineShift(-0.2f), fontSize = 0.75.em, color = color ?: Color.Unspecified)) { append(sub) }
    }
}

fun sym(s: String, color: Color? = null): AnnotatedString = buildAnnotatedString { appendSym(s, color) }

private val TOKENS = Regex("""\[\[[^\]]+]]|\*\*[^*]+\*\*""")

/** Text s tokeny [[značka]] a **tučně**. */
fun AnnotatedString.Builder.appendRich(text: String) {
    var pos = 0
    for (m in TOKENS.findAll(text)) {
        append(text.substring(pos, m.range.first))
        val v = m.value
        if (v.startsWith("[[")) appendSym(v.substring(2, v.length - 2))
        else withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { appendRich(v.substring(2, v.length - 2)) }
        pos = m.range.last + 1
    }
    append(text.substring(pos))
}

fun rich(text: String): AnnotatedString = buildAnnotatedString { appendRich(text) }
