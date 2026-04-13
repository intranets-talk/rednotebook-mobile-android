package com.example.rednotebook.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.data.repository.EntryRepository
import com.example.rednotebook.sync.SyncWorker
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntryRepository(app)

    private val _year  = MutableLiveData(Calendar.getInstance().get(Calendar.YEAR))
    private val _month = MutableLiveData(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val year:  LiveData<Int> = _year
    val month: LiveData<Int> = _month

    private val _entries = MutableLiveData<Map<Int, EntryEntity>>(emptyMap())
    val entries: LiveData<Map<Int, EntryEntity>> = _entries

    private val _error   = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    init {
        // On startup: show local data immediately, then do initial sync in background
        loadMonth()
        viewModelScope.launch {
            repo.initialSyncIfNeeded()
            // Reload after sync so fresh data appears without user action
            loadMonthSilent()
        }
    }

    /** Load from Room instantly — no network, no spinner. */
    fun loadMonth() {
        viewModelScope.launch {
            try {
                val list = repo.getMonthEntries(_year.value!!, _month.value!!)
                _entries.value = list.associateBy { it.day }
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
            }
        }
    }

    /** Silently refresh current month from API in background, then update UI. */
    fun refreshCurrentMonth() {
        viewModelScope.launch {
            try {
                repo.refreshMonthInBackground(_year.value!!, _month.value!!)
                val list = repo.getMonthEntries(_year.value!!, _month.value!!)
                _entries.value = list.associateBy { it.day }
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadMonthSilent() {
        try {
            val list = repo.getMonthEntries(_year.value!!, _month.value!!)
            _entries.value = list.associateBy { it.day }
        } catch (_: Exception) {}
    }

    fun saveEntry(year: Int, month: Int, day: Int, text: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repo.saveEntry(year, month, day, text)
                if (_year.value == year && _month.value == month) loadMonth()
                SyncWorker.enqueue(getApplication())
                onDone(true)
            } catch (e: Exception) {
                _error.value = "Save failed: ${e.message}"
                onDone(false)
            }
        }
    }

    fun deleteEntry(year: Int, month: Int, day: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteEntry(year, month, day)
                if (_year.value == year && _month.value == month) loadMonth()
                SyncWorker.enqueue(getApplication())
                onDone(true)
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
                onDone(false)
            }
        }
    }

    fun goToPreviousMonth() {
        val m = _month.value!!; val y = _year.value!!
        if (m == 1) { _month.value = 12; _year.value = y - 1 } else _month.value = m - 1
        loadMonth()
        refreshCurrentMonth()
    }

    fun goToNextMonth() {
        val m = _month.value!!; val y = _year.value!!
        if (m == 12) { _month.value = 1; _year.value = y + 1 } else _month.value = m + 1
        loadMonth()
        refreshCurrentMonth()
    }

    fun setYearMonth(year: Int, month: Int) {
        _year.value = year
        _month.value = month
        loadMonth()
        refreshCurrentMonth()
    }

    fun clearError() { _error.value = null }
}
