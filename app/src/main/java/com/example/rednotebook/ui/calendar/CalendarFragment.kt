package com.example.rednotebook.ui.calendar

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.rednotebook.R
import com.example.rednotebook.data.local.EntryEntity
import com.example.rednotebook.databinding.FragmentCalendarBinding
import java.text.DateFormatSymbols
import java.util.Calendar

class SinglePageRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val maxFling = (width * 0.6f).toInt()
        return super.fling(velocityX.coerceIn(-maxFling, maxFling), velocityY)
    }
}

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // Own ViewModel — not shared with ListFragment
    private val viewModel: CalendarViewModel by viewModels()

    private lateinit var adapter: MonthAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private val OFFSET      = 5000
    private val EPOCH_YEAR  = 2000
    private val EPOCH_MONTH = 1

    private fun posToYearMonth(pos: Int): Pair<Int, Int> {
        val absMonth = (EPOCH_YEAR * 12 + EPOCH_MONTH - 1) + (pos - OFFSET)
        return Pair(absMonth / 12, absMonth % 12 + 1)
    }

    private fun yearMonthToPos(year: Int, month: Int): Int {
        val epochAbs = EPOCH_YEAR * 12 + (EPOCH_MONTH - 1)
        return OFFSET + (year * 12 + (month - 1) - epochAbs)
    }

    private var isScrollingProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MonthAdapter()
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarPager.layoutManager = layoutManager
        binding.calendarPager.adapter = adapter
        binding.calendarPager.clipChildren = false
        binding.calendarPager.clipToPadding = false
        LinearSnapHelper().attachToRecyclerView(binding.calendarPager)

        // Scroll to current ViewModel month (not necessarily today)
        val initPos = yearMonthToPos(viewModel.year.value!!, viewModel.month.value!!)
        layoutManager.scrollToPosition(initPos)

        binding.calendarPager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isScrollingProgrammatically) {
                    val snapView = LinearSnapHelper().findSnapView(layoutManager) ?: return
                    val pos = layoutManager.getPosition(snapView)
                    if (pos == RecyclerView.NO_ID.toInt()) return
                    val (year, month) = posToYearMonth(pos)
                    viewModel.setYearMonth(year, month)
                    updateHeader(year, month)
                }
            }
        })

        viewModel.entries.observe(viewLifecycleOwner) { adapter.notifyDataSetChanged() }

        binding.btnPrev.setOnClickListener { scrollByMonths(-1) }
        binding.btnNext.setOnClickListener { scrollByMonths(+1) }
        binding.btnToday.setOnClickListener { jumpToToday() }
        binding.tvMonthYear.setOnClickListener { showYearPicker() }

        updateHeader(viewModel.year.value!!, viewModel.month.value!!)
    }

    private fun scrollByMonths(delta: Int) {
        binding.calendarPager.smoothScrollToPosition(
            layoutManager.findFirstVisibleItemPosition() + delta)
    }

    private fun jumpTo(year: Int, month: Int) {
        val target  = yearMonthToPos(year, month)
        val current = layoutManager.findFirstVisibleItemPosition()
        isScrollingProgrammatically = true
        if (Math.abs(target - current) > 3) {
            layoutManager.scrollToPosition(target)
            binding.calendarPager.post {
                isScrollingProgrammatically = false
                viewModel.setYearMonth(year, month)
                updateHeader(year, month)
                adapter.notifyDataSetChanged()
            }
        } else {
            isScrollingProgrammatically = false
            binding.calendarPager.smoothScrollToPosition(target)
        }
    }

    private fun jumpToToday() {
        val now = Calendar.getInstance()
        jumpTo(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    private fun updateHeader(year: Int, month: Int) {
        binding.tvMonthYear.text = "${DateFormatSymbols().months[month - 1]} $year"
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
                jumpTo(picker.value, viewModel.month.value ?: 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class MonthAdapter : RecyclerView.Adapter<MonthAdapter.VH>() {
        inner class VH(val container: LinearLayout) : RecyclerView.ViewHolder(container)
        override fun getItemCount() = OFFSET * 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }
            return VH(container)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (year, month) = posToYearMonth(position)
            val entries = if (viewModel.year.value == year && viewModel.month.value == month)
                viewModel.entries.value ?: emptyMap()
            else emptyMap()
            buildMonthGrid(holder.container, year, month, entries)
        }
    }

    private fun buildMonthGrid(
        container: LinearLayout, year: Int, month: Int, entries: Map<Int, EntryEntity>
    ) {
        container.removeAllViews()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDow    = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today       = Calendar.getInstance()
        var day = 1; var leadingBlanks = firstDow

        while (day <= daysInMonth) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52))
            }
            for (col in 0..6) {
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                when {
                    leadingBlanks > 0 -> {
                        row.addView(View(requireContext()).apply { layoutParams = p }); leadingBlanks--
                    }
                    day > daysInMonth -> row.addView(View(requireContext()).apply { layoutParams = p })
                    else -> {
                        val d        = day
                        val hasEntry = entries.containsKey(d)
                        val isToday  = today.get(Calendar.YEAR) == year
                                && today.get(Calendar.MONTH) + 1 == month
                                && today.get(Calendar.DAY_OF_MONTH) == d
                        val cell = TextView(requireContext()).apply {
                            layoutParams = p
                            text         = d.toString()
                            gravity      = Gravity.CENTER
                            textSize     = 16f
                            background   = when {
                                isToday  -> ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_today_circle)
                                hasEntry -> ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_circle)
                                else     -> null
                            }
                            if (isToday || hasEntry) setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.white))
                            setOnClickListener {
                                findNavController().navigate(
                                    R.id.action_calendar_to_editor,
                                    bundleOf("year" to year, "month" to month, "day" to d))
                            }
                        }
                        row.addView(cell); day++
                    }
                }
            }
            container.addView(row)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        // Reload entries in case an edit was made in EditorFragment
        viewModel.onResume()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
