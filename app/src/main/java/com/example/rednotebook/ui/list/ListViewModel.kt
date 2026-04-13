package com.example.rednotebook.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.data.repository.EntryRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class ListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntryRepository(app)

    val year    = MutableLiveData(Calendar.getInstance().get(Calendar.YEAR))
    val month   = MutableLiveData(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val entries = MutableLiveData<List<EntryEntity>>(emptyList())
    val error   = MutableLiveData<String?>()

    var minYear = 1990; private set
    var maxYear = Calendar.getInstance().get(Calendar.YEAR); private set

    init {
        loadMonth()
        viewModelScope.launch { loadYearRange() }
    }

    fun onResume() { loadMonth() }

    fun loadMonth() {
        viewModelScope.launch {
            try {
                val list = repo.getMonthEntries(year.value!!, month.value!!)
                entries.value = list.sortedBy { it.day }
            } catch (e: Exception) {
                error.value = e.message
            }
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
    }

    fun goToPreviousMonth() {
        val m = month.value!!; val y = year.value!!
        if (m == 1) { month.value = 12; year.value = y - 1 } else month.value = m - 1
        loadMonth()
    }

    fun goToNextMonth() {
        val m = month.value!!; val y = year.value!!
        if (m == 12) { month.value = 1; year.value = y + 1 } else month.value = m + 1
        loadMonth()
    }

    fun clearError() { error.value = null }
}
