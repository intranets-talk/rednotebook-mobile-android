package com.example.rednotebook.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.rednotebook.data.local.AppDatabase
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.data.local.MonthRef
import com.example.rednotebook.data.local.OpType
import com.example.rednotebook.data.local.PendingOpEntity
import com.example.rednotebook.data.model.Entry
import com.example.rednotebook.data.model.EntryBody
import com.example.rednotebook.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EntryRepository(private val context: Context) {

    private val db        = AppDatabase.getInstance(context)
    private val entryDao  = db.entryDao()
    private val pendingDao = db.pendingOpDao()

    private val prefs = context.getSharedPreferences("rednotebook_prefs", Context.MODE_PRIVATE)

    // ── Network check ─────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun canUseNetwork() = ApiClient.isConfigured() && isNetworkAvailable()

    // ── Full sync (called once on first connect) ───────────────────────────

    suspend fun initialSyncIfNeeded() = withContext(Dispatchers.IO) {
        if (!canUseNetwork()) return@withContext
        val alreadySynced = prefs.getBoolean("initial_sync_done", false)
        if (alreadySynced) return@withContext

        try {
            val months = ApiClient.api.getMonths()
            for (m in months) {
                try {
                    val entries = ApiClient.api.getMonthEntries(m.year, m.month)
                    entryDao.deleteMonth(m.year, m.month)
                    entryDao.upsertAll(entries.map { it.toEntity() })
                } catch (_: Exception) {}
            }
            prefs.edit().putBoolean("initial_sync_done", true).apply()
        } catch (_: Exception) {}
    }

    /** Force a full re-sync (e.g. after saving new API URL in Settings). */
    fun resetInitialSync() {
        prefs.edit().putBoolean("initial_sync_done", false).apply()
    }

    // ── Background refresh for a single month ─────────────────────────────

    suspend fun refreshMonthInBackground(year: Int, month: Int) = withContext(Dispatchers.IO) {
        if (!canUseNetwork()) return@withContext
        try {
            val remote = ApiClient.api.getMonthEntries(year, month)
            entryDao.deleteMonth(year, month)
            entryDao.upsertAll(remote.map { it.toEntity() })
        } catch (_: Exception) {}
    }

    // ── Read — always Room first, instant ─────────────────────────────────

    suspend fun getMonthEntries(year: Int, month: Int): List<EntryEntity> =
        withContext(Dispatchers.IO) {
            entryDao.getMonthEntries(year, month)
        }

    suspend fun getEntry(year: Int, month: Int, day: Int): EntryEntity? =
        withContext(Dispatchers.IO) {
            entryDao.getEntry(year, month, day)
        }

    suspend fun getAvailableMonths(): List<MonthRef> =
        withContext(Dispatchers.IO) {
            entryDao.getAvailableMonths()
        }

    suspend fun search(query: String): List<EntryEntity> =
        withContext(Dispatchers.IO) {
            entryDao.search(query)
        }

    suspend fun cacheEntry(year: Int, month: Int, day: Int, text: String, date: String) =
        withContext(Dispatchers.IO) {
            entryDao.upsert(
                EntryEntity(year = year, month = month, day = day,
                    text = text, date = date, isSynced = true)
            )
        }

    // ── Write ─────────────────────────────────────────────────────────────

    suspend fun saveEntry(year: Int, month: Int, day: Int, text: String) =
        withContext(Dispatchers.IO) {
            val date   = "%04d-%02d-%02d".format(year, month, day)
            val entity = EntryEntity(year = year, month = month, day = day,
                text = text, date = date, isSynced = false)
            entryDao.upsert(entity)

            if (canUseNetwork()) {
                try {
                    ApiClient.api.saveEntry(year, month, day, EntryBody(text))
                    entryDao.upsert(entity.copy(isSynced = true))
                    pendingDao.deleteForDay(year, month, day)
                    return@withContext
                } catch (_: Exception) {}
            }
            queueOp(OpType.UPSERT, year, month, day, text)
        }

    suspend fun deleteEntry(year: Int, month: Int, day: Int) =
        withContext(Dispatchers.IO) {
            entryDao.delete(year, month, day)

            if (canUseNetwork()) {
                try {
                    ApiClient.api.deleteEntry(year, month, day)
                    pendingDao.deleteForDay(year, month, day)
                    return@withContext
                } catch (_: Exception) {}
            }
            queueOp(OpType.DELETE, year, month, day)
        }

    // ── Sync queue ────────────────────────────────────────────────────────

    suspend fun hasPendingOps() = withContext(Dispatchers.IO) { pendingDao.count() > 0 }

    suspend fun syncPendingOps() = withContext(Dispatchers.IO) {
        if (!canUseNetwork()) return@withContext
        for (op in pendingDao.getAll()) {
            try {
                when (op.opType) {
                    OpType.UPSERT -> {
                        ApiClient.api.saveEntry(op.year, op.month, op.day, EntryBody(op.text))
                        entryDao.getEntry(op.year, op.month, op.day)?.let {
                            entryDao.upsert(it.copy(isSynced = true))
                        }
                    }
                    OpType.DELETE -> ApiClient.api.deleteEntry(op.year, op.month, op.day)
                }
                pendingDao.delete(op)
            } catch (_: Exception) { break }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private suspend fun queueOp(type: OpType, year: Int, month: Int, day: Int, text: String = "") {
        pendingDao.deleteForDay(year, month, day)
        pendingDao.insert(PendingOpEntity(opType = type, year = year,
            month = month, day = day, text = text))
    }

    private fun Entry.toEntity() = EntryEntity(year = year, month = month, day = day,
        text = text, date = date, isSynced = true)
}
