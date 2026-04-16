package com.example.rednotebook.ui.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rednotebook.R
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.databinding.FragmentListBinding
import com.example.rednotebook.databinding.ItemEntryBinding
import java.text.DateFormatSymbols
import java.util.Calendar

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    // Own ViewModel — not shared with CalendarFragment
    private val viewModel: ListViewModel by viewModels()

    private lateinit var adapter: EntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EntryAdapter { entry ->
            findNavController().navigate(
                R.id.action_list_to_editor,
                bundleOf("year" to entry.year, "month" to entry.month, "day" to entry.day)
            )
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnPrev.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.btnNext.setOnClickListener { viewModel.goToNextMonth() }
        binding.tvMonthYear.setOnClickListener { showYearPicker() }
        binding.tvMonthYear.background =
            requireContext().obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
            ).getDrawable(0)

        binding.fabToday.setOnClickListener {
            val now = Calendar.getInstance()
            viewModel.setYearMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        }

        binding.fabAdd.setOnClickListener {
            // Navigate to editor for today's day number in the currently viewed month
            val year  = viewModel.year.value  ?: return@setOnClickListener
            val month = viewModel.month.value ?: return@setOnClickListener
            // Use today's day if we're in the current month, otherwise day 1
            val now = Calendar.getInstance()
            val day = now.get(Calendar.DAY_OF_MONTH)
            findNavController().navigate(
                R.id.action_list_to_editor,
                bundleOf("year" to year, "month" to month, "day" to day)
            )
        }

        viewModel.year.observe(viewLifecycleOwner)  { updateHeader() }
        viewModel.month.observe(viewLifecycleOwner) { updateHeader() }
        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            val isEmpty = entries.isEmpty()
            binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.fabAdd.visibility    = View.VISIBLE
        }
    }

    private fun updateHeader() {
        val year  = viewModel.year.value  ?: return
        val month = viewModel.month.value ?: return
        binding.tvMonthYear.text = "${DateFormatSymbols().months[month - 1]} $year"

        // Show FAB only when not on the current month
        val now = Calendar.getInstance()
        val isCurrentMonth = year == now.get(Calendar.YEAR) &&
                month == now.get(Calendar.MONTH) + 1
        binding.fabToday.visibility = if (isCurrentMonth) View.GONE else View.VISIBLE
    }

    private fun showYearPicker() {
        val currentYear = viewModel.year.value ?: Calendar.getInstance().get(Calendar.YEAR)
        val picker = NumberPicker(requireContext()).apply {
            minValue = viewModel.minYear
            maxValue = viewModel.maxYear
            value    = currentYear.coerceIn(viewModel.minYear, viewModel.maxYear)
            wrapSelectorWheel = false
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Jump to year")
            .setView(picker)
            .setPositiveButton("Go") { _, _ ->
                viewModel.setYearMonth(picker.value, viewModel.month.value ?: 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class EntryAdapter(
    private val onClick: (EntryEntity) -> Unit
) : RecyclerView.Adapter<EntryAdapter.VH>() {

    private var items: List<EntryEntity> = emptyList()

    fun submitList(list: List<EntryEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.binding.tvDay.text     = entry.day.toString()
        holder.binding.tvDate.text    = entry.date
        holder.binding.tvSnippet.text = entry.text.trim().take(120)
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    override fun getItemCount() = items.size
}
