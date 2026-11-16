package cz.usharik.statika.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import cz.usharik.statika.platform.IosAppGraph
import cz.usharik.statika.platform.systemLanguageCode
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

/** Vzhled systému – z obrazovky, ne z okna (okno přebírá téma aplikace kvůli stavovému řádku). */
private fun systemIsDark() = UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** Celá aplikace jako view controller pro SwiftUI. */
fun MainViewController(app: IosAppGraph): UIViewController = ComposeUIViewController {
    // přepočítat při změně vzhledu (Compose překreslí při změně traitů) i po návratu do aplikace
    val traitDark = isSystemInDarkTheme()
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ -> tick++ }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
    }
    val dark = remember(tick, traitDark) { systemIsDark() }
    val lang = remember { systemLanguageCode(NSLocale.preferredLanguages.firstOrNull() as? String) }
    // start i každý návrat do aplikace: souhlas, který dosud nevyšel, a tiché ověření nákupu (společná pravidla)
    LaunchedEffect(tick) { app.graph.onForeground() }
    StatikaApp(app.graph, systemDark = dark, systemLang = lang) { appDark ->
        // stavový řádek podle tématu aplikace (může se lišit od systému)
        UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle =
            if (appDark == systemIsDark()) UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            else if (appDark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }
}

