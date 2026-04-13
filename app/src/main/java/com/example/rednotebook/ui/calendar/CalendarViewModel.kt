package com.example.rednotebook.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.data.repository.EntryRepository
import com.example.rednotebook.sync.SyncWorker
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntryRepository(app)

    val year    = MutableLiveData(Calendar.getInstance().get(Calendar.YEAR))
    val month   = MutableLiveData(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val entries = MutableLiveData<Map<Int, EntryEntity>>(emptyMap())
    val error   = MutableLiveData<String?>()

    var minYear = 1990; private set
    var maxYear = Calendar.getInstance().get(Calendar.YEAR); private set

    init {
        loadMonth()
        viewModelScope.launch {
            loadYearRange()          // from Room — instant
            repo.initialSyncIfNeeded()
            loadMonthInternal()      // reload after sync
            loadYearRange()          // reload range after sync
        }
    }

    /** Called every time CalendarFragment resumes — picks up saves from EditorFragment. */
    fun onResume() {
        loadMonth()
    }

    fun loadMonth() {
        viewModelScope.launch { loadMonthInternal() }
    }

    private suspend fun loadMonthInternal() {
        try {
            val list = repo.getMonthEntries(year.value!!, month.value!!)
            entries.value = list.associateBy { it.day }
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    fun refreshCurrentMonth() {
        viewModelScope.launch {
            try {
                repo.refreshMonthInBackground(year.value!!, month.value!!)
                loadMonthInternal()
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadYearRange() {
        try {
            val months = repo.getAvailableMonths()
            if (months.isNotEmpty()) {
                minYear = months.minOf { it.year }
                maxYear = maxOf(months.maxOf { it.year },
                    Calendar.getInstance().get(Calendar.YEAR))
            }
        } catch (_: Exception) {}
    }

    fun setYearMonth(y: Int, m: Int) {
        year.value  = y
        month.value = m
        loadMonth()
        refreshCurrentMonth()
    }

    fun clearError() { error.value = null }
}
