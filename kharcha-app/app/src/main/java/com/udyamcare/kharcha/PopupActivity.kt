package com.udyamcare.kharcha

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.Toast

/** The popup: shows the payment and asks who it was + category. One tap on a category saves it. */
class PopupActivity : Activity() {

    private lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L.load(this)
        db = Db(this)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        val accent = if (debit) C.RED else C.GREEN
        val remaining = db.pendingCount() - 1

        val box = vertical().apply { background = rounded(C.CARD, 24) }

        // Coloured header
        val header = vertical().apply {
            background = rounded(accent, 24)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            gravity = Gravity.CENTER
        }
        header.addView(label(
            if (debit) tr("↑ पैसे गए", "↑ Money paid") else tr("↓ पैसे आए", "↓ Money received"),
            14f, 0xCCFFFFFF.toInt(), true
        ))
        header.addView(label(formatRupees(t.amountPaise), 34f, 0xFFFFFFFF.toInt(), true).apply { gravity = Gravity.CENTER })
        if (remaining > 0) {
            header.addView(label(tr("और $remaining पेमेंट बाकी हैं", "$remaining more waiting"), 12f, 0xCCFFFFFF.toInt()))
        }
        box.addView(header, lp(MATCH, WRAP))

        val body = vertical().apply { setPadding(dp(16), dp(14), dp(16), dp(16)) }

        body.addView(label(
            if (debit) tr("किसको दिए?", "Who did you pay?") else tr("किससे आए?", "Who paid you?"),
            13f, C.SUB, true
        ))
        val shown = Alias.get(this, t.party) ?: t.party
        val party = AutoCompleteTextView(this).apply {
            setText(shown)
            hint = tr("नाम लिखो", "Type a name")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            threshold = 1
            setAdapter(ArrayAdapter(this@PopupActivity, android.R.layout.simple_dropdown_item_1line, db.partyNames()))
        }
        body.addView(party, lp(MATCH, WRAP, b = dp(6)))

        // Suggestion from history
        val last = db.lastCategory(shown, t.type)?.let { Cat.byKey(it) }
        if (last != null) {
            body.addView(chip(tr("पिछली बार: ", "Same as last time: ") + last.label, C.GREEN_BG, C.GREEN) {
                persist(t, party.text.toString().trim(), last.key); done()
            }, lp(WRAP, WRAP, b = dp(6)))
        }

        body.addView(label(tr("किस काम का? टैप करते ही सेव", "Category — tap to save"), 13f, C.SUB, true), lp(WRAP, WRAP, t = dp(4)))
        body.addView(categoryChips(Cat.forType(t.type), null, onAdd = {
            showAddCategoryDialog(t.type) { c ->
                persist(t, party.text.toString().trim(), c.key); done()
            }
        }) { cat ->
            persist(t, party.text.toString().trim(), cat.key); done()
        })

        body.addView(chip(tr("बाद में / छोड़ो", "Later / Skip"), C.CHIP, C.SUB) {
            db.skip(id); next()
        }, lp(WRAP, WRAP, t = dp(10)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        box.addView(body)

        val outer = FrameLayout(this).apply { setPadding(dp(10), dp(10), dp(10), dp(10)) }
        outer.addView(box)
        setContentView(ScrollView(this).apply { addView(outer) })
    }

    private fun persist(t: Txn, typed: String, catKey: String) {
        Alias.put(this, t.party, typed)
        db.save(t.id, typed, catKey)
    }

    private fun done() {
        Toast.makeText(this, tr("✔ सेव हो गया", "✔ Saved"), Toast.LENGTH_SHORT).show()
        next()
    }

    /** Show the next unanswered payment if there is one, else close. */
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
