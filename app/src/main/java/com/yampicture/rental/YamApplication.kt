package com.yampicture.rental

import android.app.Application
import com.yampicture.rental.notify.AlertWorker
import com.yampicture.rental.notify.Notifier
import com.yampicture.rental.util.Prefs
import com.yampicture.rental.util.localizedContext

class YamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val lang = Prefs(this).load().language
        Notifier.ensureChannels(localizedContext(lang))
        AlertWorker.schedule(this)
    }
}
