package cz.usharik.statika.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.model.EqSpec
import cz.usharik.statika.model.Problem
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.unknownsOf
import cz.usharik.statika.trainer.SolveStep
import cz.usharik.statika.trainer.Term
import cz.usharik.statika.trainer.doneSummary
import cz.usharik.statika.trainer.eqTitle
import cz.usharik.statika.trainer.paren
import cz.usharik.statika.trainer.unitOf
import kotlinx.coroutines.delay
import kotlin.math.abs

/** Název rovnice; u kloubu se šipkou zvolené části. */
fun eqName(spec: EqSpec): AnnotatedString = buildAnnotatedString {
    if (spec is EqSpec.Hinge) {
        appendSym("M_${spec.point}")
        withStyle(SpanStyle(fontSize = 11.sp)) { append(if (spec.side.key == "left") "←" else "→") }
        append(" = 0")
    } else appendRich(eqTitle(spec))
}

/** Rovnice s čísly; už spočítané neznámé se zobrazí dosazené (zeleně). */
private fun numericEq(terms: List<Term>, solved: Map<String, Double>, c: StatikaColors): AnnotatedString = buildAnnotatedString {
    terms.forEachIndexed { i, x ->
        val neg = x.c < 0
        append(if (neg) (if (i > 0) " − " else "−") else if (i > 0) " + " else "")
        val arm = if (x.arm != null) abs(x.c) else null
        val uid = x.it.unknownId
        if (uid != null) {
            val v = solved[uid]
            if (v != null) withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.SemiBold)) { append(paren(v)) }
            else appendSym(x.it.sym, c.react)
        } else append(fmt(x.it.value!!))
        if (arm != null) append("·" + fmt(arm))
    }
    append(" = 0")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepSolveView(st: SolveStep) {
    val tr = LocalTr.current
    val c = LocalColors.current
    StepPanel(tr("s4Title"), tr("s4Intro"), 7 to 4) {
        val solved = st.solvedSet.associateWith { st.answer.getValue(it) }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            st.eqs.forEachIndexed { i, e ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(eqName(e.spec), color = c.inkSoft, modifier = Modifier.padding(end = 10.dp))
                    Text(numericEq(st.eqTerms[i], solved, c), modifier = Modifier.weight(1f))
                }
            }
        }
        FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val open = st.unknowns.filter { st.status[it.id] != cz.usharik.statika.trainer.FieldState.OK }
            for (u in st.unknowns) {
                NumField(
                    buildAnnotatedString { appendSym(u.sym, c.react); append(" =") },
                    st.vals[u.id] ?: "",
                    { st.setVal(u.id, it) },
                    st.status[u.id],
                    width = 96.dp,
                    onDone = st::check,
                    suffix = unitOf(u),
                    next = u != open.lastOrNull() && u in open,
                    signed = true,
                )
            }
        }
        Notes(st.msgs)
        Actions(st.allOk) {
            if (!st.allOk) {
                HintButton(if (st.hinted) 1 else 0, st::hint)
                PrimaryButton(tr("check"), st::check)
            } else PrimaryButton(tr("next") + " →", st::done)
        }
    }
}

@Composable
fun StepDoneView(p: Problem, results: Map<String, Double>, hints: Int, onRetry: () -> Unit, onNext: () -> Unit, banner: @Composable () -> Unit) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val sum = remember(p, results, hints) { doneSummary(p, results, hints) }
    StepPanel(tr("s5Title")) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 1..3) {
                val s = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(150L * i)
                    s.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                Text("★", fontSize = 38.sp, color = if (i <= sum.stars) c.star else c.line, modifier = Modifier.scale(s.value))
            }
        }
        Text(tr("s5Text"), color = c.inkSoft, modifier = Modifier.padding(top = 6.dp))
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (u in unknownsOf(p)) {
                Text(
                    buildAnnotatedString {
                        appendSym(u.sym, c.react)
                        append(" = ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(fmt(results.getValue(u.id))) }
                        append(" " + unitOf(u))
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Text(
            buildAnnotatedString {
                append(tr("s5Check") + ": Σ")
                appendSym("M_${sum.point.label}")
                append(" = ${fmt(sum.residual, 3)} ≈ 0 ✓ · ${tr("hintsUsed")}: $hints")
            },
            style = MaterialTheme.typography.bodySmall,
            color = c.inkSoft,
            modifier = Modifier.padding(top = 10.dp),
        )
        Actions {
            SecondaryButton(tr("retry"), onRetry)
            PrimaryButton(tr("nextProblem") + " →", onNext)
        }
        banner()
    }
}
