package cz.usharik.statika

import android.app.Application
import cz.usharik.statika.platform.AdMobSdk
import cz.usharik.statika.platform.AndroidPlatform
import cz.usharik.statika.platform.AppGraph
import cz.usharik.statika.platform.CurrentActivity
import cz.usharik.statika.platform.PlayBillingSdk
import cz.usharik.statika.platform.PrefsStore
import cz.usharik.statika.platform.SharedAds
import cz.usharik.statika.platform.SharedBilling
import cz.usharik.statika.platform.firebaseAnalytics
import cz.usharik.statika.platform.migrateLegacyAdFree

class StatikaApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(CurrentActivity)
        val store = PrefsStore(this)
        migrateLegacyAdFree(this, store)
        val analytics = firebaseAnalytics(this)
        val billing = SharedBilling(PlayBillingSdk(this), store, analytics)
        graph = AppGraph(
            store = store,
            analytics = analytics,
            ads = SharedAds(AdMobSdk(this), billing, analytics),
            billing = billing,
            platform = AndroidPlatform(this),
        )
    }
}
