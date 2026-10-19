package com.yampicture.rental.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.yampicture.rental.R
import com.yampicture.rental.data.Cat
import com.yampicture.rental.data.LedgerEntry
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.data.Types
import com.yampicture.rental.logic.MonthReport
import com.yampicture.rental.logic.balance
import com.yampicture.rental.logic.billedDays
import com.yampicture.rental.logic.dailyTotal
import com.yampicture.rental.logic.extraDays
import com.yampicture.rental.logic.paid
import com.yampicture.rental.logic.total
import com.yampicture.rental.util.Fmt
import com.yampicture.rental.util.Settings
import java.io.File
import java.io.FileOutputStream

/** Tiny A4 text layout engine on top of [PdfDocument]. */
private class PdfWriter(private val footer: String) {
    private val doc = PdfDocument()
    private val pageW = 595
    private val pageH = 842
    private val margin = 40f
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f
    private var pageNo = 0

    private val navy = 0xFF1B2A41.toInt()
    private val gold = 0xFFC99A2E.toInt()
    private val gray = 0xFF5A6070.toInt()

    val titlePaint = paint(22f, true, navy)
    val subtitlePaint = paint(12f, false, gray)
    val headingPaint = paint(14f, true, navy)
    val bodyPaint = paint(11f, false, 0xFF16181D.toInt())
    val boldPaint = paint(11f, true, 0xFF16181D.toInt())
    val mutedPaint = paint(10f, false, gray)

    private fun paint(size: Float, bold: Boolean, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        isFakeBoldText = bold
        this.color = color
    }

    init {
        newPage()
    }

    private fun newPage() {
        finishPage()
        pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        canvas = page!!.canvas
        y = margin + 14f
    }

    private fun finishPage() {
        val p = page ?: return
        val c = canvas!!
        val paint = Paint(mutedPaint).apply { textAlign = Paint.Align.CENTER }
        c.drawText("$footer · $pageNo", pageW / 2f, pageH - 22f, paint)
        doc.finishPage(p)
        page = null
    }

    private fun ensure(h: Float) {
        if (y + h > pageH - margin - 10f) newPage()
    }

    fun space(h: Float) {
        y += h
    }

    fun text(s: String, p: Paint = bodyPaint, lineHeight: Float = p.textSize + 6f) {
        ensure(lineHeight)
        canvas!!.drawText(s, margin, y, p)
        y += lineHeight
    }

    /** Word-wrapped paragraph. */
    fun paragraph(s: String, p: Paint = bodyPaint) {
        val width = pageW - margin * 2
        var rest = s.trim()
        while (rest.isNotEmpty()) {
            val count = p.breakText(rest, true, width, null).coerceAtLeast(1)
            var cut = count
            if (count < rest.length) {
                val space = rest.lastIndexOf(' ', count)
                if (space > 0) cut = space
            }
            text(rest.substring(0, cut).trim(), p)
            rest = rest.substring(cut).trim()
        }
    }

    fun heading(s: String) {
        space(10f)
        ensure(26f)
        canvas!!.drawText(s, margin, y, headingPaint)
        y += 6f
        canvas!!.drawRect(margin, y, margin + 36f, y + 2f, Paint().apply { color = gold })
        y += 16f
    }

    fun rule() {
        ensure(10f)
        canvas!!.drawLine(margin, y - 4f, pageW - margin, y - 4f, Paint().apply { color = 0xFFD5D8DE.toInt() })
        y += 6f
    }

    /**
     * One table row. [weights] are column widths; the first column is left aligned, the rest are right
     * aligned (numbers). Cell text is clipped to its column so rows never overlap.
     */
    fun row(cells: List<String>, weights: List<Float>, bold: Boolean = false) {
        ensure(18f)
        val usable = pageW - margin * 2
        val total = weights.sum()
        val p = if (bold) boldPaint else bodyPaint
        var x = margin
        cells.forEachIndexed { i, cell ->
            val w = usable * weights[i] / total
            val fitted = fit(cell, p, w - 6f)
            if (i == 0) {
                canvas!!.drawText(fitted, x, y, p)
            } else {
                val right = Paint(p).apply { textAlign = Paint.Align.RIGHT }
                canvas!!.drawText(fitted, x + w - 2f, y, right)
            }
            x += w
        }
        y += 18f
    }

    private fun fit(s: String, p: Paint, width: Float): String {
        if (p.measureText(s) <= width) return s
        val n = p.breakText(s, true, width - p.measureText("…"), null)
        return s.substring(0, n.coerceAtLeast(0)) + "…"
    }

    fun write(file: File) {
        finishPage()
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }
}

object PdfExport {

    private fun exportsDir(ctx: Context): File {
        val dir = File(ctx.cacheDir, "exports")
        dir.mkdirs()
        return dir
    }

    fun monthlyReport(
        ctx: Context,
        settings: Settings,
        report: MonthReport,
        entries: List<LedgerEntry>,
    ): File {
        val fmt = Fmt(settings.language, settings.ethiopianDates)
        fun money(n: Long) = ctx.getString(R.string.money_fmt, fmt.num(n))
        fun category(c: String) = ctx.getString(categoryRes(c))

        val w = PdfWriter(settings.shopName)
        w.text(settings.shopName, w.titlePaint, 30f)
        w.text(ctx.getString(R.string.monthly_report) + " — " + fmt.month(report.year, report.month), w.subtitlePaint)
        w.text(ctx.getString(R.string.generated_on, fmt.dateTime(System.currentTimeMillis())), w.mutedPaint)

        w.heading(ctx.getString(R.string.summary))
        val two = listOf(3f, 2f)
        w.row(listOf(ctx.getString(R.string.income), money(report.income)), two)
        w.row(listOf(ctx.getString(R.string.expense), money(report.expense)), two)
        w.rule()
        w.row(listOf(ctx.getString(R.string.net), money(report.net)), two, bold = true)

        w.heading(ctx.getString(R.string.rentals))
        w.row(listOf(ctx.getString(R.string.rentals_count), report.rentalCount.toString()), two)
        w.row(listOf(ctx.getString(R.string.billed), money(report.billed)), two)
        w.row(listOf(ctx.getString(R.string.collected), money(report.paidOnRentals)), two)
        w.row(listOf(ctx.getString(R.string.outstanding), money(report.outstanding)), two)
        w.row(listOf(ctx.getString(R.string.late_days), report.lateDays.toString()), two)

        if (report.incomeByCategory.isNotEmpty()) {
            w.heading(ctx.getString(R.string.income_by_category))
            report.incomeByCategory.forEach { (c, v) -> w.row(listOf(category(c), money(v)), two) }
        }
        if (report.expenseByCategory.isNotEmpty()) {
            w.heading(ctx.getString(R.string.expense_by_category))
            report.expenseByCategory.forEach { (c, v) -> w.row(listOf(category(c), money(v)), two) }
        }

        if (report.equipment.isNotEmpty()) {
            w.heading(ctx.getString(R.string.equipment_performance))
            val cols = listOf(3.2f, 1.2f, 1.2f, 2.2f)
            w.row(
                listOf(
                    ctx.getString(R.string.equipment),
                    ctx.getString(R.string.times),
                    ctx.getString(R.string.days),
                    ctx.getString(R.string.revenue),
                ),
                cols,
                bold = true,
            )
            report.equipment.forEach { e ->
                val name = if (settings.isAmharic && e.nameAm.isNotBlank()) e.nameAm else e.name
                w.row(listOf(name, e.rentals.toString(), e.days.toString(), money(e.revenue)), cols)
            }
        }

        if (report.customers.isNotEmpty()) {
            w.heading(ctx.getString(R.string.top_customers))
            val cols = listOf(3.4f, 1.2f, 2.4f)
            report.customers.take(10).forEach { c ->
                w.row(listOf(c.name, c.rentals.toString(), money(c.billed)), cols)
            }
        }

        if (entries.isNotEmpty()) {
            w.heading(ctx.getString(R.string.transactions))
            val cols = listOf(1.6f, 2.4f, 2.4f, 2f)
            entries.sortedBy { it.at }.forEach { e ->
                val sign = if (e.type == Types.INCOME) "+" else "-"
                w.row(listOf(fmt.date(e.at), category(e.category), e.note.ifBlank { "—" }, sign + money(e.amount)), cols)
            }
        }

        val file = File(exportsDir(ctx), "report-%04d-%02d.pdf".format(report.year, report.month + 1))
        w.write(file)
        return file
    }

    fun receipt(ctx: Context, settings: Settings, f: RentalFull, now: Long): File {
        val fmt = Fmt(settings.language, settings.ethiopianDates)
        fun money(n: Long) = ctx.getString(R.string.money_fmt, fmt.num(n))
        val grace = settings.graceMs
        val r = f.rental

        val w = PdfWriter(settings.shopName)
        w.text(settings.shopName, w.titlePaint, 30f)
        if (settings.shopPhone.isNotBlank()) w.text(settings.shopPhone, w.subtitlePaint)
        w.text(ctx.getString(R.string.receipt_title, r.id), w.headingPaint, 24f)
        w.text(ctx.getString(R.string.generated_on, fmt.dateTime(now)), w.mutedPaint)

        w.heading(ctx.getString(R.string.customer))
        w.text(f.customer.name, w.boldPaint)
        if (f.customer.phone.isNotBlank()) w.text(f.customer.phone)

        w.heading(ctx.getString(R.string.period))
        val two = listOf(2f, 3f)
        w.row(listOf(ctx.getString(R.string.picked_up), fmt.dateTime(r.startAt)), two)
        w.row(listOf(ctx.getString(R.string.return_by), fmt.dateTime(r.dueAt)), two)
        if (r.returnedAt != null) {
            w.row(listOf(ctx.getString(R.string.returned), fmt.dateTime(r.returnedAt)), two)
        }

        w.heading(ctx.getString(R.string.equipment))
        val cols = listOf(3.2f, 1.8f, 1f, 2f)
        w.row(
            listOf(
                ctx.getString(R.string.item),
                ctx.getString(R.string.rate),
                ctx.getString(R.string.days),
                ctx.getString(R.string.amount),
            ),
            cols,
            bold = true,
        )
        val days = f.billedDays(now, grace)
        f.items.forEach { item ->
            w.row(
                listOf(item.label(settings.isAmharic), money(item.dailyRate), days.toString(), money(item.dailyRate * days)),
                cols,
            )
        }
        val extra = f.extraDays(now, grace)
        if (extra > 0) {
            w.text(ctx.getString(R.string.receipt_late_note, extra, r.days), w.mutedPaint)
        }

        w.heading(ctx.getString(R.string.payment))
        val pay = listOf(3f, 2f)
        w.row(listOf(ctx.getString(R.string.total), money(f.total(now, grace))), pay, bold = true)
        f.payments.sortedBy { it.at }.forEach { p ->
            val sign = if (p.type == Types.INCOME) "" else "-"
            val name = ctx.getString(categoryRes(p.category)) + " (" + fmt.date(p.at) + ")"
            w.row(listOf(name, sign + money(p.amount)), pay)
        }
        w.rule()
        w.row(listOf(ctx.getString(R.string.paid), money(f.paid)), pay)
        w.row(listOf(ctx.getString(R.string.balance), money(f.balance(now, grace))), pay, bold = true)

        if (r.notes.isNotBlank()) {
            w.heading(ctx.getString(R.string.notes))
            w.paragraph(r.notes)
        }
        if (r.returnNotes.isNotBlank()) {
            w.heading(ctx.getString(R.string.return_notes))
            w.paragraph(r.returnNotes)
        }

        w.space(50f)
        w.row(listOf("______________________", "______________________"), listOf(1f, 1f))
        w.row(listOf(ctx.getString(R.string.signature_studio), ctx.getString(R.string.signature_customer)), listOf(1f, 1f))

        val file = File(exportsDir(ctx), "receipt-${r.id}.pdf")
        w.write(file)
        return file
    }

    fun share(ctx: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, chooserTitle))
    }

    private fun categoryRes(cat: String): Int = when (cat) {
        Cat.DEPOSIT -> R.string.cat_deposit
        Cat.PAYMENT -> R.string.cat_payment
        Cat.SHOOT -> R.string.cat_shoot
        Cat.OTHER_INCOME -> R.string.cat_other_income
        Cat.REPAIR -> R.string.cat_repair
        Cat.PURCHASE -> R.string.cat_purchase
        Cat.TRANSPORT -> R.string.cat_transport
        Cat.SALARY -> R.string.cat_salary
        Cat.BILLS -> R.string.cat_bills
        Cat.MARKETING -> R.string.cat_marketing
        Cat.REFUND -> R.string.cat_refund
        else -> R.string.cat_other_expense
    }
}
