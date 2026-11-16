package cz.usharik.statika

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import cz.usharik.statika.platform.systemLanguageCode
import cz.usharik.statika.ui.StatikaApp
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val graph get() = (application as StatikaApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // odkaz na úlohu (statika://12345, …/statika-trener/#12345 nebo extra „seed“) – jen při skutečném otevření,
        // ne po obnovení stavu ani při spuštění ze seznamu posledních aplikací (tam je pořád původní intent)
        if (savedInstanceState == null && (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) openLink(intent)
        setContent {
            StatikaApp(graph, systemDark = isSystemDark(), systemLang = systemLanguageCode(Locale.getDefault().toLanguageTag())) { dark ->
                // ikony stavového řádku podle tématu aplikace (může se lišit od systému)
                val status = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                // Android 7 neumí tmavé ikony navigační lišty – světlé téma dostane tmavý závoj
                val nav = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, 0x801B2330.toInt())
                enableEdgeToEdge(status, nav)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // souhlas (UMP), který dosud nevyšel, a tiché ověření nákupu – společná pravidla (AppGraph.onForeground)
        graph.onForeground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openLink(intent)
    }

    private fun openLink(intent: Intent?) {
        if (intent == null) return
        if (!graph.links.open(intent.dataString)) intent.getIntExtra("seed", 0).takeIf { it > 0 }?.let(graph.links::open)
    }
}

@Composable
private fun isSystemDark(): Boolean {
    val cfg = LocalConfiguration.current
    return (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
