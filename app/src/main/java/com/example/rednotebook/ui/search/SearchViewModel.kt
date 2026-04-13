package com.example.rednotebook.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.data.repository.EntryRepository
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntryRepository(app)

    val query   = MutableLiveData<String>("")
    val results = MutableLiveData<List<EntryEntity>>(emptyList())
    val error   = MutableLiveData<String?>(null)
    val loading = MutableLiveData<Boolean>(false)

    fun search(q: String) {
        if (q.isBlank()) return
        query.value   = q
        loading.value = true
        error.value   = null
        viewModelScope.launch {
            try {
                val found = repo.search(q)
                results.value = found
                if (found.isEmpty()) error.value = "No results found."
            } catch (e: Exception) {
                error.value   = "Search failed: ${e.message}"
                results.value = emptyList()
            } finally {
                loading.value = false
            }
        }
    }
}
