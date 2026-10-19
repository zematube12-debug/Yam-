package com.yampicture.rental.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yampicture.rental.R
import com.yampicture.rental.data.AppDatabase
import com.yampicture.rental.logic.AlertState
import com.yampicture.rental.logic.Billing
import com.yampicture.rental.logic.DateUtil
import com.yampicture.rental.logic.RentalAlerts
import com.yampicture.rental.logic.Reports
import com.yampicture.rental.logic.dailyTotal
import com.yampicture.rental.logic.total
import com.yampicture.rental.util.Fmt
import com.yampicture.rental.util.Prefs
import com.yampicture.rental.util.localizedContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        AlertEngine.run(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "rental_alerts",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun runNow(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "rental_alerts_now",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AlertWorker>().build(),
            )
        }
    }
}

/** Looks at every rental that is still out and sends whichever notifications are due (each exactly once). */
object AlertEngine {
    suspend fun run(appContext: Context) {
        val db = AppDatabase.get(appContext)
        val prefs = Prefs(appContext)
        val settings = prefs.load()
        val ctx = appContext.localizedContext(settings.language)
        val fmt = Fmt(settings.language, settings.ethiopianDates)
        Notifier.ensureChannels(ctx)

        val now = System.currentTimeMillis()
        val grace = settings.graceMs
        val lead = settings.leadMs

        for (f in db.rentals().getActive()) {
            val r = f.rental
            if (r.startAt > now) continue

            val old = AlertState(r.dueNotified, r.warnedIndex, r.addedNotifiedIndex)
            val d = RentalAlerts.evaluate(r.dueAt, now, grace, lead, old)
            if (d.newState != old) {
                db.rentals().setAlertState(r.id, d.newState.dueNotified, d.newState.warnedIndex, d.newState.addedIndex)
            }

            val customer = f.customer.name
            val items = f.items.joinToString(", ") { it.label(settings.isAmharic) }
            val route = "rental/${r.id}"

            if (d.notifyDue) {
                Notifier.post(
                    ctx,
                    Notifier.notificationId(r.id, Notifier.KIND_DUE),
                    Notifier.CH_ALERTS,
                    ctx.getString(R.string.n_due_title, customer),
                    ctx.getString(R.string.n_due_text, items, fmt.dateTime(r.dueAt)),
                    route,
                )
            }
            if (d.warnIndex > 0) {
                val at = Billing.extraDayThreshold(r.dueAt, grace, d.warnIndex)
                Notifier.post(
                    ctx,
                    Notifier.notificationId(r.id, Notifier.KIND_WARN),
                    Notifier.CH_ALERTS,
                    ctx.getString(R.string.n_warn_title, customer),
                    ctx.getString(
                        R.string.n_warn_text,
                        items,
                        ctx.getString(R.string.money_fmt, fmt.num(f.dailyTotal)),
                        fmt.dateTime(at),
                    ),
                    route,
                )
            }
            if (d.addedIndex > 0) {
                Notifier.post(
                    ctx,
                    Notifier.notificationId(r.id, Notifier.KIND_ADDED),
                    Notifier.CH_ALERTS,
                    ctx.getString(R.string.n_added_title, customer),
                    ctx.getString(
                        R.string.n_added_text,
                        d.addedIndex,
                        ctx.getString(R.string.money_fmt, fmt.num(f.dailyTotal * d.addedIndex)),
                        ctx.getString(R.string.money_fmt, fmt.num(f.total(now, grace))),
                    ),
                    route,
                )
            }
        }

        maybeSendMonthlyReport(ctx, db, prefs, fmt, now, grace)
    }

    /** On the last evening of every month: a ready-made summary notification that opens the report. */
    private suspend fun maybeSendMonthlyReport(
        ctx: Context,
        db: AppDatabase,
        prefs: Prefs,
        fmt: Fmt,
        now: Long,
        grace: Long,
    ) {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        val lastDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isLastEvening = c.get(Calendar.DAY_OF_MONTH) == lastDay && c.get(Calendar.HOUR_OF_DAY) >= 17
        val monthIndex = DateUtil.monthIndexOf(now)
        if (!isLastEvening || prefs.lastReportMonth == monthIndex) return

        val report = Reports.build(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            db.rentals().getAll(),
            db.ledger().getAll(),
            now,
            grace,
        )
        fun money(n: Long) = ctx.getString(R.string.money_fmt, fmt.num(n))
        Notifier.post(
            ctx,
            9_000_000 + monthIndex % 1000,
            Notifier.CH_REPORT,
            ctx.getString(R.string.n_report_title, fmt.month(report.year, report.month)),
            ctx.getString(R.string.n_report_text, money(report.income), money(report.expense), money(report.net)),
            "reports",
        )
        prefs.lastReportMonth = monthIndex
    }
}
