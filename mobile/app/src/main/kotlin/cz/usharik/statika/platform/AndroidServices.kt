package cz.usharik.statika.platform

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.graphics.Canvas
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cz.usharik.statika.BuildConfig
import java.io.File
import java.lang.ref.WeakReference

/** Aktuální Activity pro nákupní dialog, reklamu a sdílení. */
object CurrentActivity : Application.ActivityLifecycleCallbacks {
    private var ref: WeakReference<Activity>? = null
    val activity: Activity? get() = ref?.get()

    override fun onActivityResumed(activity: Activity) {
        ref = WeakReference(activity)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        ref = WeakReference(activity)
    }
    override fun onActivityDestroyed(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

/** [name] = soubor SharedPreferences; do zálohy jde jen „statika“ (res/xml/backup_rules.xml, data_extraction_rules.xml). */
class PrefsStore(context: Context, name: String = "statika") : KeyValueStore {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putString(key: String, value: String?) = prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
}

/**
 * Firebase Analytics + Crashlytics; bez google-services.json (vývojová sestavení) jen do logu.
 * Úpravy událostí, podržení do souhlasu a rozhodnutí z TCF jsou společné ([NormalizedAnalytics], [analyticsConsent]).
 */
fun firebaseAnalytics(context: Context): Analytics {
    if (!BuildConfig.HAS_FIREBASE || FirebaseApp.getApps(context).isEmpty()) {
        return NormalizedAnalytics(object : AnalyticsSink {
            override fun log(name: String, params: Map<String, Any>) {
                Log.d("Analytics", "$name $params")
            }
            override fun setConsent(consent: AnalyticsConsent) {
                Log.d("Analytics", "consent $consent")
            }
        })
    }
    val fa = FirebaseAnalytics.getInstance(context)
    return NormalizedAnalytics(object : AnalyticsSink {
        override fun setConsent(consent: AnalyticsConsent) {
            // v manifestu je vše výchozím stavem zakázané (i Crashlytics); personalizace reklam se nepovoluje nikdy
            fun st(on: Boolean) = if (on) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
            fa.setConsent(
                mapOf(
                    FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to st(consent.analytics),
                    FirebaseAnalytics.ConsentType.AD_STORAGE to st(consent.adStorage),
                    FirebaseAnalytics.ConsentType.AD_USER_DATA to st(consent.adUserData),
                    FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                ),
            )
            fa.setAnalyticsCollectionEnabled(consent.analytics)
            val crash = FirebaseCrashlytics.getInstance()
            crash.setCrashlyticsCollectionEnabled(consent.analytics)
            if (!consent.analytics) crash.deleteUnsentReports()
        }

        override fun log(name: String, params: Map<String, Any>) {
            val b = Bundle()
            for ((k, v) in params) when (v) {
                is Long -> b.putLong(k, v)
                is Double -> b.putDouble(k, v)
                else -> b.putString(k, v.toString())
            }
            fa.logEvent(name, b)
        }
    })
}

class AndroidPlatform(private val context: Context) : PlatformServices {
    override val versionName: String = versionLabel(BuildConfig.VERSION_NAME, BuildConfig.GIT_HASH)

    override fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(Intent.createChooser(send, null))
    }

    override fun openUrl(url: String) = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /** Stránky se kreslí přímo do plátna PdfDocument – text i výkres zůstanou vektorové. */
    override fun sharePdf(doc: PdfDoc) {
        val pdf = PdfDocument()
        val file = try {
            doc.pages.indices.forEach { i ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PdfDoc.WIDTH.toInt(), PdfDoc.HEIGHT.toInt(), i + 1).create())
                doc.drawPage(i, Canvas(page.canvas), 1f)
                pdf.finishPage(page)
            }
            // v cache drží jen poslední export
            val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            File(dir, doc.fileName).also { f -> f.outputStream().use(pdf::writeTo) }
        } catch (e: Exception) {
            Log.w("Statika", "PDF se nepodařilo vytvořit", e)
            return
        } finally {
            pdf.close()
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, doc.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // ClipData: náhled v systémovém výběru a předání oprávnění ke čtení i přes chooser
        send.clipData = ClipData.newRawUri(doc.title, uri)
        start(Intent.createChooser(send, doc.title))
    }

    private fun start(intent: Intent) {
        val a = CurrentActivity.activity
        try {
            if (a != null) a.startActivity(intent) else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }
}
