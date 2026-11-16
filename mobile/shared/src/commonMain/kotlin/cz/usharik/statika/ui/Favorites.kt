package cz.usharik.statika.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.usharik.statika.i18n.LocalTr
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.generate
import cz.usharik.statika.trainer.DrawingView
import cz.usharik.statika.trainer.Favorite
import cz.usharik.statika.trainer.FavoritesRepository

/** Seznam oblíbených úloh: náhled výkresu, číslo, typ a úroveň; klepnutí úlohu otevře. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    favorites: FavoritesRepository,
    current: Pair<Int, GenOptions>,
    onDismiss: () -> Unit,
    onOpen: (Favorite) -> Unit,
    onDelete: (Favorite) -> Unit,
) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(
                tr("favorites") + if (favorites.items.isNotEmpty()) " (${favorites.items.size})" else "",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = c.ink,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            )
            if (favorites.items.isEmpty()) {
                Text(tr("favEmpty"), color = c.inkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 32.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    items(favorites.items, key = { it.seed.toString() + "/" + it.opts.level + "/" + it.opts.types }) { f ->
                        FavoriteRow(f, f.same(current.first, current.second), { onOpen(f) }, { onDelete(f) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(f: Favorite, isCurrent: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val tr = LocalTr.current
    val c = LocalColors.current
    val problem = remember(f.seed, f.opts) { runCatching { generate(f.seed, f.opts) }.getOrNull() }
    Surface(
        onClick = onOpen, shape = CardShape, color = c.paper,
        border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) c.activeBorder else c.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            // malý náhled výkresu, ať se úloha pozná na první pohled
            Box(Modifier.width(132.dp)) {
                if (problem != null) BeamCanvas(
                    problem = problem, released = emptySet(), resolved = emptySet(), highlight = null,
                    view = DrawingView(), results = null, activeSupport = null, compact = true,
                    animKey = "fav" to f.seed, description = tr("drawing"),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("problemNo") + " " + f.seed, fontWeight = FontWeight.SemiBold, color = c.ink)
                Text(
                    (problem?.let { tr("type_${it.type.key}") + " · " } ?: "") + tr("level${f.opts.level}"),
                    style = MaterialTheme.typography.bodySmall, color = c.inkSoft,
                )
                val status = listOfNotNull(
                    if (f.solved) "✓ " + tr("favSolved") else null,
                    if (isCurrent) tr("favCurrent") else null,
                ).joinToString(" · ")
                if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall, color = if (f.solved) c.accent else c.inkSoft)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = tr("favDelete"), tint = c.inkSoft) }
        }
    }
}
