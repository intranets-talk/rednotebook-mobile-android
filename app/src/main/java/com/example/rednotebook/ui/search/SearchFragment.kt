package com.example.rednotebook.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rednotebook.R
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.databinding.FragmentSearchBinding
import com.example.rednotebook.databinding.ItemSearchResultBinding

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchAdapter { entry ->
            findNavController().navigate(
                R.id.action_search_to_editor,
                bundleOf("year" to entry.year, "month" to entry.month, "day" to entry.day)
            )
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val savedQuery = searchViewModel.query.value ?: ""
        binding.etSearch.setText(savedQuery)
        binding.btnClear.visibility = if (savedQuery.isNotEmpty()) View.VISIBLE else View.GONE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClear.visibility =
                    if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        })

        binding.btnClear.setOnClickListener {
            binding.etSearch.setText("")
            searchViewModel.query.value   = ""
            searchViewModel.results.value = emptyList()
            binding.emptyView.visibility  = View.GONE
        }

        searchViewModel.results.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            binding.emptyView.visibility =
                if (results.isEmpty()
                    && searchViewModel.query.value?.isNotBlank() == true
                    && searchViewModel.loading.value == false)
                    View.VISIBLE else View.GONE
        }

        searchViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        searchViewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                binding.emptyView.text = err
                binding.emptyView.visibility = View.VISIBLE
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
        binding.btnSearch.setOnClickListener { doSearch() }
    }

    private fun doSearch() {
        val q = binding.etSearch.text.toString().trim()
        if (q.isEmpty()) return
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        searchViewModel.search(q)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SearchAdapter(
    private val onClick: (EntryEntity) -> Unit
) : RecyclerView.Adapter<SearchAdapter.VH>() {

    private var items: List<EntryEntity> = emptyList()

    fun submitList(list: List<EntryEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSearchResultBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.binding.tvDate.text    = e.date
        holder.binding.tvSnippet.text = e.text.trim().take(120)
        holder.itemView.setOnClickListener { onClick(e) }
    }

    override fun getItemCount() = items.size
}
