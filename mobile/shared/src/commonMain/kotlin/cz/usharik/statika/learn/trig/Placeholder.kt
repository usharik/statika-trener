package cz.usharik.statika.learn.trig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.usharik.statika.learn.CardScope
import cz.usharik.statika.learn.CardText
import cz.usharik.statika.learn.Chips
import cz.usharik.statika.learn.L
import cz.usharik.statika.learn.Todo

/**
 * TODO(fáze 2): dočasná karta, dokud lekce nemá skutečné karty z návrhu kurzu. Má opravdový úkol,
 * aby i ona prošla CardGateTest (každá karta začíná zamčená).
 */
@Composable
internal fun CardScope.Placeholder(about: L) {
    var ok by remember { mutableStateOf(false) }
    CardText(about)
    CardText(L("Tahle lekce se připravuje.", "Этот урок готовится."))
    Todo(L("Klepni na „Rozumím“", "Нажми «Понятно»") to ok)
    Chips(listOf(L("Rozumím", "Понятно")), if (ok) 0 else null) { ok = true }
}
