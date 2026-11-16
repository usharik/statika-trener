package cz.usharik.statika.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.model.PointForce
import cz.usharik.statika.model.ReactionSet
import cz.usharik.statika.model.Udl
import cz.usharik.statika.model.fmt
import cz.usharik.statika.model.reactionSetOf
import cz.usharik.statika.trainer.HingeAnswer
import cz.usharik.statika.trainer.LoadsStep
import cz.usharik.statika.trainer.ReleaseStep
import cz.usharik.statika.trainer.ReleaseTask
import cz.usharik.statika.trainer.setLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepReleaseView(st: ReleaseStep) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val learn = when (st.task) {
        is ReleaseTask.SupportTask -> 6 to 0
        is ReleaseTask.HingeTask -> 9 to 0
        ReleaseTask.Count -> 9 to 2
    }
    StepPanel(tr("s1Title"), tr("s1Intro"), learn) {
        Text(rich(tr("s1Convention")), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 6.dp))
        DoneLog(st.log)
        Column(Modifier.padding(top = 14.dp)) {
            when (val task = st.task) {
                is ReleaseTask.SupportTask -> {
                    Text(tr("supportN", "p" to task.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val correct = reactionSetOf(task.type)
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        for (set in ReactionSet.entries) {
                            val selected = st.picked == set
                            ChoiceTile(selected, selected && set == correct, !st.solved || selected, { st.pickSet(set) }, Modifier.weight(1f)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SetIcon(set)
                                    Text(rich(setLabel(set, task.label)), color = c.react, textAlign = TextAlign.Center)
                                    Text(tr("set_${set.key}"), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                is ReleaseTask.HingeTask -> {
                    Text(tr("hingeN", "p" to task.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(tr("hingeQ"), modifier = Modifier.padding(top = 6.dp))
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (a in HingeAnswer.entries) {
                            val selected = st.picked == a
                            ChoiceTile(selected, selected && a == HingeAnswer.FORCES, !st.solved || selected, { st.pickHinge(a) }) {
                                Text(tr("hinge_${a.key}"), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                ReleaseTask.Count -> {
                    Text(tr("countTitle"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumField(labelWithVar(tr("countUnknowns"), "n"), st.nU, { st.nU = it }, width = 80.dp, onDone = st::checkCount, next = true)
                        NumField(labelWithVar(tr("countEqs"), "r"), st.nE, { st.nE = it }, width = 80.dp, onDone = st::checkCount)
                    }
                }
            }
            Notes(st.msgs)
            Actions(st.solved to st.idx) {
                if (!st.solved) {
                    if (st.hintLevel < st.maxHint) HintButton(st.hintLevel, st::hint)
                    if (st.task == ReleaseTask.Count) PrimaryButton(tr("check"), st::checkCount)
                } else PrimaryButton(tr("next") + " →", st::advance)
            }
        }
    }
}

private fun labelWithVar(label: String, v: String) = buildAnnotatedString {
    append("$label ")
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(v) }
    append(" =")
}

/** Malý piktogram sady reakcí. */
@Composable
private fun SetIcon(set: ReactionSet) {
    val c = LocalColors.current
    Canvas(Modifier.width(80.dp).height(56.dp)) {
        val k = size.width / 80f
        fun o(x: Float, y: Float) = Offset(x * k, y * k)
        drawLine(c.ink.copy(alpha = 0.18f), o(14f, 22f), o(74f, 22f), 5f * k, StrokeCap.Round)
        drawCircle(c.ink, 3.5f * k, o(44f, 22f))
        val col = c.react
        if (set != ReactionSet.Y) {
            drawLine(col, o(10f, 22f), o(33f, 22f), 2.2f * k)
            drawPath(Path().apply { moveTo(40f * k, 22f * k); lineTo(31f * k, 17.5f * k); lineTo(31f * k, 26.5f * k); close() }, col)
        }
        if (set != ReactionSet.X) {
            drawLine(col, o(44f, 52f), o(44f, 33f), 2.2f * k)
            drawPath(Path().apply { moveTo(44f * k, 26f * k); lineTo(39.5f * k, 35f * k); lineTo(48.5f * k, 35f * k); close() }, col)
        }
        if (set == ReactionSet.XYM) {
            // oblouk M56,26 A13,13 0 1 0 31,14 – SVG poloměr zvětší na polovinu tětivy: půlkruh kolem (43,5; 20)
            val r = 13.87f
            drawArc(col, 25.6f, -180f, false, o(43.5f - r, 20f - r), Size(2 * r * k, 2 * r * k), style = Stroke(2f * k))
            drawPath(Path().apply { moveTo(28.5f * k, 19.5f * k); lineTo(29f * k, 10f * k); lineTo(36f * k, 14.5f * k); close() }, col)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepLoadsView(st: LoadsStep) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val task = st.task
    if (task == null) {
        StepPanel(tr("s2Title")) {
            if (st.log.isNotEmpty()) DoneLog(st.log) else Text(tr("s2Nothing"), modifier = Modifier.padding(top = 6.dp))
            for (m in st.moments) Text(rich(tr("s2Moment", "m" to m.label)), style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = 8.dp))
            Actions { PrimaryButton(tr("next") + " →", st::done) }
        }
        return
    }
    StepPanel(tr("s2Title"), tr("s2Intro"), if (task is Udl) 8 to 1 else 3 to 1) {
        DoneLog(st.log)
        val q = st.resultant
        val taskText = when (task) {
            is Udl -> tr("udlTask", "q" to fmt(task.q), "l" to fmt(task.x2 - task.x1), "Q" to q)
            is PointForce -> tr("forceTask", "F" to task.label, "v" to fmt(task.f), "a" to (task.alpha ?: 0))
            else -> ""
        }
        Text(rich(taskText), modifier = Modifier.padding(top = 14.dp))
        // šikmá síla = sin a cos: trvalý odkaz do kurzu trigonometrie (po zavření se vrátí sem)
        if (task is PointForce && task.alpha != null) {
            val opener = cz.usharik.statika.learn.LocalLearn.current
            Text(
                "📖 " + tr("trigLink"), color = c.react, style = MaterialTheme.typography.bodySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
                    .clickable { opener.open(cz.usharik.statika.learn.LearnNav.Open(102, 0, fromTrainer = true)) }
                    .padding(vertical = 4.dp),
            )
        }
        val (la, lb) = if (task is Udl) tr("udlQ", "Q" to q) to tr("udlPos", "Q" to q) else (task as PointForce).let { tr("fx", "F" to it.label) to tr("fy", "F" to it.label) }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField(rich(la), st.a, { st.a = it }, st.state.first, onDone = st::check, next = true)
            NumField(rich(lb), st.b, { st.b = it }, st.state.second, onDone = st::check)
        }
        Notes(st.msgs)
        Actions(st.solved to st.idx) {
            if (!st.solved) {
                if (st.hintLevel < 3) HintButton(st.hintLevel, st::hint)
                PrimaryButton(tr("check"), st::check)
            } else PrimaryButton(tr("next") + " →", st::advance)
        }
    }
}

@Composable
fun RowCenter(content: @Composable () -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) { content() }
