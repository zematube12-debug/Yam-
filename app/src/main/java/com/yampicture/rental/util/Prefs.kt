package com.yampicture.rental.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

data class Settings(
    val language: String = "am",
    /** Free lateness (hours) after the due time. More than this = one extra day is billed. */
    val graceHours: Int = 12,
    /** How many hours BEFORE an extra day is added the warning notification is sent. */
    val leadHours: Int = 3,
    val ethiopianDates: Boolean = true,
    val shopName: String = "YAM PICTURE",
    val shopPhone: String = "",
) {
    val graceMs: Long get() = graceHours * 3_600_000L
    val leadMs: Long get() = leadHours * 3_600_000L
    val isAmharic: Boolean get() = language == "am"
}

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("yam_prefs", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            language = sp.getString("lang", d.language) ?: d.language,
            graceHours = sp.getInt("grace", d.graceHours),
            leadHours = sp.getInt("lead", d.leadHours),
            ethiopianDates = sp.getBoolean("eth", d.ethiopianDates),
            shopName = sp.getString("shop_name", d.shopName) ?: d.shopName,
            shopPhone = sp.getString("shop_phone", d.shopPhone) ?: d.shopPhone,
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putString("lang", s.language)
            .putInt("grace", s.graceHours)
            .putInt("lead", s.leadHours)
            .putBoolean("eth", s.ethiopianDates)
            .putString("shop_name", s.shopName)
            .putString("shop_phone", s.shopPhone)
            .apply()
    }

    /** Month (year*12+month) for which the "monthly report is ready" notification was already sent. */
    var lastReportMonth: Int
        get() = sp.getInt("last_report_month", -1)
        set(v) {
            sp.edit().putInt("last_report_month", v).apply()
        }
}

/** A context whose string resources follow the in-app language instead of the phone language. */
class LocalizedContext(base: Context, lang: String) : ContextWrapper(base) {
    private val localizedResources: Resources = run {
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(Locale(lang))
        base.createConfigurationContext(cfg).resources
    }

    override fun getResources(): Resources = localizedResources
}

fun Context.localizedContext(lang: String): Context = LocalizedContext(this, lang)
