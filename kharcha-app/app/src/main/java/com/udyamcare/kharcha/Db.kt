package com.udyamcare.kharcha

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

data class Txn(
    val id: Long,
    val amountPaise: Long,
    val type: TxType,
    val party: String,
    val category: String,
    val time: Long,
    val status: Int, // 0 = pending, 1 = saved, 2 = skipped
)

class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "kharcha.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE txn (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount INTEGER NOT NULL,
                type INTEGER NOT NULL,
                party TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT '',
                time INTEGER NOT NULL,
                status INTEGER NOT NULL DEFAULT 0,
                dedupe TEXT NOT NULL UNIQUE,
                body TEXT NOT NULL DEFAULT ''
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /** Returns the new row id, or -1 if this SMS was already recorded. */
    fun insertIfNew(p: ParsedTx, body: String, time: Long, status: Int = 0): Long {
        val key = p.ref?.let { "ref:$it" } ?: "h:${body.hashCode()}"
        val v = ContentValues().apply {
            put("amount", p.amountPaise)
            put("type", p.type.ordinal)
            put("party", p.party)
            put("time", time)
            put("status", status)
            put("dedupe", key)
            put("body", body)
        }
        return writableDatabase.insertWithOnConflict("txn", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun get(id: Long): Txn? =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.toTxn() else null
        }

    fun save(id: Long, party: String, category: String) {
        val v = ContentValues().apply {
            put("party", party)
            put("category", category)
            put("status", 1)
        }
        writableDatabase.update("txn", v, "id=?", arrayOf(id.toString()))
    }

    fun skip(id: Long) {
        val v = ContentValues().apply { put("status", 2) }
        writableDatabase.update("txn", v, "id=?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("txn", "id=?", arrayOf(id.toString()))
    }

    fun nextPending(): Txn? =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE status=0 ORDER BY time DESC LIMIT 1", null)
            .use { if (it.moveToFirst()) it.toTxn() else null }

    fun recent(limit: Int = 200): List<Txn> =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE status<>2 ORDER BY time DESC LIMIT $limit", null)
            .use { c -> buildList { while (c.moveToNext()) add(c.toTxn()) } }

    /** Total (debit, credit) in paise for the current calendar month, counting only saved entries. */
    fun monthTotals(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var debit = 0L
        var credit = 0L
        readableDatabase.rawQuery(
            "SELECT type, SUM(amount) FROM txn WHERE status=1 AND time>=? GROUP BY type",
            arrayOf(cal.timeInMillis.toString())
        ).use {
            while (it.moveToNext()) {
                if (it.getInt(0) == TxType.DEBIT.ordinal) debit = it.getLong(1) else credit = it.getLong(1)
            }
        }
        return debit to credit
    }

    private fun Cursor.toTxn() = Txn(
        id = getLong(0), amountPaise = getLong(1), type = TxType.values()[getInt(2)],
        party = getString(3), category = getString(4), time = getLong(5), status = getInt(6),
    )

    private companion object {
        const val COLS = "id, amount, type, party, category, time, status"
    }
}
