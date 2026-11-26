package com.udyamcare.kharcha

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Txn(
    val id: Long,
    val amountPaise: Long,
    val type: TxType,
    val party: String,
    val category: String, // Cat.key, "" while pending
    val note: String,
    val time: Long,
    val status: Int, // 0 = pending, 1 = saved, 2 = skipped
)

class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "kharcha.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE txn (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount INTEGER NOT NULL,
                type INTEGER NOT NULL,
                party TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT '',
                note TEXT NOT NULL DEFAULT '',
                time INTEGER NOT NULL,
                status INTEGER NOT NULL DEFAULT 0,
                dedupe TEXT NOT NULL UNIQUE,
                body TEXT NOT NULL DEFAULT ''
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS txn")
        onCreate(db)
    }

    // ----- writes -----

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

    fun insertManual(type: TxType, paise: Long, party: String, cat: String, note: String, time: Long): Long {
        val v = ContentValues().apply {
            put("amount", paise)
            put("type", type.ordinal)
            put("party", party)
            put("category", cat)
            put("note", note)
            put("time", time)
            put("status", 1)
            put("dedupe", "m:${System.nanoTime()}")
        }
        return writableDatabase.insert("txn", null, v)
    }

    fun save(id: Long, party: String, category: String) {
        val v = ContentValues().apply {
            put("party", party)
            put("category", category)
            put("status", 1)
        }
        writableDatabase.update("txn", v, "id=?", arrayOf(id.toString()))
    }

    fun update(id: Long, paise: Long, party: String, category: String, note: String) {
        val v = ContentValues().apply {
            put("amount", paise)
            put("party", party)
            put("category", category)
            put("note", note)
            put("status", 1)
        }
        writableDatabase.update("txn", v, "id=?", arrayOf(id.toString()))
    }

    fun reassignCategory(oldKey: String, newKey: String) {
        val v = ContentValues().apply { put("category", newKey) }
        writableDatabase.update("txn", v, "category=?", arrayOf(oldKey))
    }

    fun skip(id: Long) {
        val v = ContentValues().apply { put("status", 2) }
        writableDatabase.update("txn", v, "id=?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("txn", "id=?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        writableDatabase.delete("txn", null, null)
    }

    // ----- reads -----

    fun get(id: Long): Txn? =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.toTxn() else null
        }

    fun nextPending(): Txn? =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE status=0 ORDER BY time DESC LIMIT 1", null)
            .use { if (it.moveToFirst()) it.toTxn() else null }

    fun pendingCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM txn WHERE status=0", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /** filter: 0 = all, 1 = money out, 2 = money in. */
    fun list(from: Long, to: Long, filter: Int, q: String): List<Txn> {
        val sb = StringBuilder("SELECT $COLS FROM txn WHERE status<>2 AND time>=? AND time<?")
        val args = mutableListOf(from.toString(), to.toString())
        if (filter == 1) sb.append(" AND type=0") else if (filter == 2) sb.append(" AND type=1")
        if (q.isNotBlank()) {
            sb.append(" AND (party LIKE ? OR note LIKE ?)")
            args += "%${q.trim()}%"; args += "%${q.trim()}%"
        }
        sb.append(" ORDER BY time DESC LIMIT 500")
        return readableDatabase.rawQuery(sb.toString(), args.toTypedArray())
            .use { c -> buildList { while (c.moveToNext()) add(c.toTxn()) } }
    }

    fun all(): List<Txn> =
        readableDatabase.rawQuery("SELECT $COLS FROM txn WHERE status=1 ORDER BY time DESC", null)
            .use { c -> buildList { while (c.moveToNext()) add(c.toTxn()) } }

    /** (money out, money in) in paise, saved entries only. */
    fun totals(from: Long, to: Long): Pair<Long, Long> {
        var out = 0L
        var inn = 0L
        readableDatabase.rawQuery(
            "SELECT type, SUM(amount) FROM txn WHERE status=1 AND time>=? AND time<? GROUP BY type",
            arrayOf(from.toString(), to.toString())
        ).use {
            while (it.moveToNext()) {
                if (it.getInt(0) == TxType.DEBIT.ordinal) out = it.getLong(1) else inn = it.getLong(1)
            }
        }
        return out to inn
    }

    fun byCategory(from: Long, to: Long, type: TxType): List<Pair<String, Long>> =
        readableDatabase.rawQuery(
            "SELECT category, SUM(amount) FROM txn WHERE status=1 AND type=? AND time>=? AND time<? " +
                "GROUP BY category ORDER BY SUM(amount) DESC",
            arrayOf(type.ordinal.toString(), from.toString(), to.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) } }

    fun topParties(from: Long, to: Long, type: TxType, limit: Int): List<Pair<String, Long>> =
        readableDatabase.rawQuery(
            "SELECT party, SUM(amount) FROM txn WHERE status=1 AND type=? AND party<>'' AND time>=? AND time<? " +
                "GROUP BY lower(party) ORDER BY SUM(amount) DESC LIMIT $limit",
            arrayOf(type.ordinal.toString(), from.toString(), to.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) } }

    fun partyNames(): List<String> =
        readableDatabase.rawQuery(
            "SELECT party FROM txn WHERE party<>'' GROUP BY lower(party) ORDER BY COUNT(*) DESC LIMIT 200", null
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    /** The category used the last time money moved with this person (to suggest it again). */
    fun lastCategory(party: String, type: TxType): String? {
        if (party.isBlank()) return null
        return readableDatabase.rawQuery(
            "SELECT category FROM txn WHERE status=1 AND category<>'' AND type=? AND lower(party)=lower(?) " +
                "ORDER BY time DESC LIMIT 1",
            arrayOf(type.ordinal.toString(), party.trim())
        ).use { if (it.moveToFirst()) it.getString(0) else null }
    }

    fun byParty(party: String): List<Txn> =
        readableDatabase.rawQuery(
            "SELECT $COLS FROM txn WHERE status=1 AND lower(party)=lower(?) ORDER BY time DESC LIMIT 100",
            arrayOf(party)
        ).use { c -> buildList { while (c.moveToNext()) add(c.toTxn()) } }

    /**
     * Net udhaar per person. Positive = they owe me, negative = I owe them.
     * (lent - got back) - (borrowed - repaid)
     */
    fun udhaar(): List<Pair<String, Long>> {
        val names = HashMap<String, String>()
        val net = HashMap<String, Long>()
        readableDatabase.rawQuery(
            "SELECT party, category, SUM(amount) FROM txn WHERE status=1 AND party<>'' " +
                "AND category IN ('lent','back','borrow','repaid') GROUP BY lower(party), category", null
        ).use {
            while (it.moveToNext()) {
                val p = it.getString(0)
                val k = p.lowercase()
                names.putIfAbsent(k, p)
                val v = it.getLong(2)
                val delta = when (it.getString(1)) {
                    "lent" -> v
                    "back" -> -v
                    "borrow" -> -v
                    else -> v // repaid
                }
                net[k] = (net[k] ?: 0L) + delta
            }
        }
        return net.filter { it.value != 0L }.map { names[it.key]!! to it.value }.sortedByDescending { kotlin.math.abs(it.second) }
    }

    private fun Cursor.toTxn() = Txn(
        id = getLong(0), amountPaise = getLong(1), type = TxType.values()[getInt(2)],
        party = getString(3), category = getString(4), note = getString(5),
        time = getLong(6), status = getInt(7),
    )

    private companion object {
        const val COLS = "id, amount, type, party, category, note, time, status"
    }
}
