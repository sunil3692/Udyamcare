package com.udyamcare.kharcha

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

object C {
    val PRIMARY = 0xFF0F766E.toInt()
    val DARK = 0xFF0B4F4A.toInt()
    val BG = 0xFFF3F5F7.toInt()
    val CARD = 0xFFFFFFFF.toInt()
    val TEXT = 0xFF111827.toInt()
    val SUB = 0xFF6B7280.toInt()
    val LINE = 0xFFE5E7EB.toInt()
    val RED = 0xFFDC2626.toInt()
    val GREEN = 0xFF16A34A.toInt()
    val AMBER = 0xFFB45309.toInt()
    val RED_BG = 0xFFFEE2E2.toInt()
    val GREEN_BG = 0xFFDCFCE7.toInt()
    val AMBER_BG = 0xFFFEF3C7.toInt()
    val CHIP = 0xFFEEF2F5.toInt()
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.rounded(color: Int, rDp: Int, strokeColor: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(rDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

fun Context.label(text: CharSequence, sp: Float = 14f, color: Int = C.TEXT, bold: Boolean = false): TextView =
    TextView(this).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

fun Context.vertical(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
fun Context.horizontal(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun Context.card(): LinearLayout = vertical().apply {
    background = rounded(C.CARD, 16)
    elevation = dp(2).toFloat()
    setPadding(dp(16), dp(14), dp(16), dp(14))
}

fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

fun Context.space(hDp: Int): View = View(this).apply { layoutParams = lp(MATCH, dp(hDp)) }

/** Small rounded tappable chip/button. */
fun Context.chip(text: String, bg: Int, fg: Int, onClick: (() -> Unit)? = null): TextView =
    label(text, 14f, fg, true).apply {
        gravity = Gravity.CENTER
        background = rounded(bg, 20)
        setPadding(dp(14), dp(9), dp(14), dp(9))
        if (onClick != null) setOnClickListener { onClick() }
    }

fun Context.primaryButton(text: String, onClick: () -> Unit): TextView =
    chip(text, C.PRIMARY, 0xFFFFFFFF.toInt(), onClick).apply {
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = rounded(C.PRIMARY, 24)
    }

/** Grid of category chips. The tag of each child is its Cat. */
fun Context.categoryChips(cats: List<Cat>, selected: Cat?, onAdd: (() -> Unit)? = null, onPick: (Cat) -> Unit): GridLayout {
    val grid = GridLayout(this).apply { columnCount = 3 }
    cats.forEach { c ->
        val on = c == selected
        val tv = label("${c.emoji}\n${c.name2}", 12f, if (on) 0xFFFFFFFF.toInt() else C.TEXT, on).apply {
            gravity = Gravity.CENTER
            background = rounded(if (on) c.color else C.CHIP, 14)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            tag = c
            setOnClickListener { onPick(c) }
        }
        val p = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
            .apply { width = 0; setMargins(dp(3), dp(3), dp(3), dp(3)) }
        grid.addView(tv, p)
    }
    if (onAdd != null) {
        val add = label("➕\n" + tr("नई", "New"), 12f, C.PRIMARY, true).apply {
            gravity = Gravity.CENTER
            background = rounded(C.GREEN_BG, 14)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            setOnClickListener { onAdd() }
        }
        val p = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f))
            .apply { width = 0; setMargins(dp(3), dp(3), dp(3), dp(3)) }
        grid.addView(add, p)
    }
    return grid
}

/** Dialog to create the user's own category. Needs an Activity context. */
fun Context.showAddCategoryDialog(type: TxType, onCreated: (Cat) -> Unit) {
    val emojis = listOf("🏷️", "☕", "🍔", "⛽", "📱", "🎓", "👕", "🎁", "🔧", "🐄", "🌾", "🚗", "💡", "🏦", "💵", "📚", "🧴", "🎬")
    var sel = emojis[0]
    val box = vertical().apply { setPadding(dp(20), dp(16), dp(20), dp(8)) }
    box.addView(label(
        tr("नई category", "New category") + if (type == TxType.DEBIT) tr(" (खर्च)", " (spent)") else tr(" (आमदनी)", " (earned)"),
        18f, C.TEXT, true
    ))
    val name = android.widget.EditText(this).apply {
        hint = tr("नाम, जैसे: दूध, पेट्रोल, मज़दूरी", "Name, e.g. Milk, Petrol, Labour")
        setSingleLine()
        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }
    box.addView(name, lp(MATCH, WRAP, t = dp(8)))
    box.addView(label(tr("इमोजी चुनो", "Pick an icon"), 13f, C.SUB, true), lp(WRAP, WRAP, t = dp(10), b = dp(4)))
    val grid = GridLayout(this).apply { columnCount = 6 }
    fun paint() {
        grid.removeAllViews()
        emojis.forEach { e ->
            val on = e == sel
            grid.addView(label(e, 22f).apply {
                gravity = Gravity.CENTER
                background = rounded(if (on) C.GREEN_BG else C.CHIP, 12)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { sel = e; paint() }
            }, GridLayout.LayoutParams().apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
        }
    }
    paint()
    box.addView(grid)

    val dialog = android.app.AlertDialog.Builder(this)
        .setView(box)
        .setPositiveButton(tr("जोड़ो", "Add"), null)
        .setNegativeButton(tr("रद्द", "Cancel"), null)
        .create()
    dialog.show()
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val n = name.text.toString().trim()
        if (n.isEmpty()) {
            name.error = tr("नाम लिखो", "Enter a name"); return@setOnClickListener
        }
        val c = Cat.addCustom(this, n, sel, type)
        dialog.dismiss()
        onCreated(c)
    }
}

// ---------------- Charts ----------------

class DonutView(ctx: Context) : View(ctx) {
    var slices: List<Pair<Int, Long>> = emptyList()
    var centerTop = ""
    var centerBottom = ""

    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h)
        val stroke = size * 0.17f
        val rect = RectF(
            (w - size) / 2 + stroke / 2, (h - size) / 2 + stroke / 2,
            (w + size) / 2 - stroke / 2, (h + size) / 2 - stroke / 2
        )
        arc.strokeWidth = stroke
        val total = slices.sumOf { it.second }
        if (total <= 0L) {
            arc.color = C.LINE
            c.drawArc(rect, 0f, 360f, false, arc)
        } else {
            var start = -90f
            for ((color, v) in slices) {
                val sweep = 360f * v / total
                arc.color = color
                c.drawArc(rect, start, maxOf(sweep - 1.5f, 0.5f), false, arc)
                start += sweep
            }
        }
        txt.color = C.SUB
        txt.textSize = context.dp(12).toFloat()
        txt.typeface = Typeface.DEFAULT
        c.drawText(centerTop, w / 2, h / 2 - context.dp(4), txt)
        txt.color = C.TEXT
        txt.textSize = context.dp(19).toFloat()
        txt.typeface = Typeface.DEFAULT_BOLD
        c.drawText(centerBottom, w / 2, h / 2 + context.dp(20), txt)
    }
}

class BarsView(ctx: Context) : View(ctx) {
    /** label, out, in */
    var months: List<Triple<String, Long, Long>> = emptyList()

    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    override fun onDraw(c: Canvas) {
        if (months.isEmpty()) return
        val padB = context.dp(22).toFloat()
        val padT = context.dp(16).toFloat()
        val chartH = height - padB - padT
        val max = months.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1L).toFloat()
        val slot = width / months.size.toFloat()
        val bw = slot * 0.24f
        val r = context.dp(4).toFloat()
        txt.textSize = context.dp(10).toFloat()
        months.forEachIndexed { i, (label, out, inn) ->
            val cx = slot * i + slot / 2
            val base = padT + chartH
            val oh = chartH * out / max
            val ih = chartH * inn / max
            bar.color = C.RED
            c.drawRoundRect(RectF(cx - bw - 2, base - oh, cx - 2, base), r, r, bar)
            bar.color = C.GREEN
            c.drawRoundRect(RectF(cx + 2, base - ih, cx + bw + 2, base), r, r, bar)
            txt.color = C.SUB
            c.drawText(label, cx, height - context.dp(6).toFloat(), txt)
            if (out > 0) c.drawText(compactRupees(out), cx - bw / 2 - 2, base - oh - context.dp(3), txt)
            if (inn > 0) c.drawText(compactRupees(inn), cx + bw / 2 + 2, base - ih - context.dp(3), txt)
        }
    }
}
