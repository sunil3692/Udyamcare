package com.udyamcare.kharcha

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The popup: shows the payment and asks who it was + category. One tap on a category saves it. */
class PopupActivity : Activity() {

    private lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        render(intent.getLongExtra("id", -1))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent.getLongExtra("id", -1))
    }

    private fun render(id: Long) {
        val t = db.get(id)
        if (t == null || t.status != 0) {
            finish(); return
        }
        Notifier.cancel(this, id)

        val debit = t.type == TxType.DEBIT
        val pad = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = (if (debit) "− " else "+ ") + formatRupees(t.amountPaise)
            textSize = 30f
            setTextColor(if (debit) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = if (debit) "पैसे किसको गए? / Paid to?" else "पैसे किससे आए? / Received from?"
            textSize = 15f
            gravity = Gravity.CENTER
        })

        val party = EditText(this).apply {
            setText(t.party)
            hint = if (debit) "नाम / Name" else "नाम / Name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
        }
        root.addView(party, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "किस काम का? एक टैप में सेव / Tap a category to save"
            textSize = 13f
            setPadding(0, dp(8), 0, dp(4))
        })

        val grid = GridLayout(this).apply { columnCount = 2 }
        categoriesFor(this, t.type).forEach { label ->
            val b = Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener {
                    db.save(id, party.text.toString().trim(), label)
                    next()
                }
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply { width = 0 }
            grid.addView(b, lp)
        }
        root.addView(grid)

        root.addView(Button(this).apply {
            text = "छोड़ो / Skip"
            isAllCaps = false
            setOnClickListener { db.skip(id); next() }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    /** After saving, show the next unanswered payment if there is one, else close. */
    private fun next() {
        val n = db.nextPending()
        if (n != null) render(n.id) else finish()
    }

    companion object {
        fun intent(context: Context, id: Long): Intent =
            Intent(context, PopupActivity::class.java)
                .putExtra("id", id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
