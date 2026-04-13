package com.example.rednotebook.ui.editor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rednotebook.data.network.ApiClient
import com.example.rednotebook.data.repository.EntryRepository
import com.example.rednotebook.databinding.FragmentEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.rednotebook.sync.SyncWorker
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: EntryRepository

    private var year = 0; private var month = 0; private var day = 0
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        year  = arguments?.getInt("year",  0) ?: 0
        month = arguments?.getInt("month", 0) ?: 0
        day   = arguments?.getInt("day",   0) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = EntryRepository(requireContext())

        if (year == 0 || month == 0 || day == 0) { findNavController().navigateUp(); return }

        binding.tvDate.text = "$day ${DateFormatSymbols().months[month - 1]} $year"
        setEditMode(false)

        binding.btnBack.setOnClickListener   { findNavController().navigateUp() }
        binding.btnEditTop.setOnClickListener { if (isEditing) saveEntry() else setEditMode(true) }
        binding.btnSave.setOnClickListener   { saveEntry() }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        loadContent()
    }

    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val entry = repo.getEntry(year, month, day)
                if (isAdded) populateViews(entry?.text ?: "")
            } catch (_: Exception) {
                if (isAdded) populateViews("")
            }
        }
    }

    private fun populateViews(text: String) {
        binding.tvContent.text = text
        binding.etContent.setText(text)
    }

    private fun setEditMode(editing: Boolean) {
        isEditing = editing
        if (editing) {
            binding.etContent.setText(binding.tvContent.text)
            binding.scrollView.visibility = View.GONE
            binding.etContent.visibility  = View.VISIBLE
            binding.etContent.requestFocus()
        } else {
            binding.tvContent.text        = binding.etContent.text
            binding.etContent.visibility  = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }
        binding.btnEditTop.setImageResource(
            if (editing) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit)
        binding.editToolbar.visibility = if (editing) View.VISIBLE else View.GONE
    }

    private fun saveEntry() {
        val text = binding.etContent.text.toString()
        // Disable buttons immediately to prevent double-tap and back-crash
        binding.btnEditTop.isEnabled = false
        binding.btnSave.isEnabled    = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.saveEntry(year, month, day, text)

                if (!isAdded) return@launch  // fragment already gone — do nothing

                binding.tvContent.text = text
                SyncWorker.enqueue(requireContext())
                val msg = if (ApiClient.isConfigured()) "Saved!"
                          else "Saved locally — will sync when online"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                setEditMode(false)
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Snackbar.make(binding.root, "Save failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                if (isAdded) {
                    binding.btnEditTop.isEnabled = true
                    binding.btnSave.isEnabled    = true
                }
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete entry?")
            .setMessage("This will delete the entry for this day.")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        repo.deleteEntry(year, month, day)
                        SyncWorker.enqueue(requireContext())
                        if (isAdded) findNavController().navigateUp()
                    } catch (e: Exception) {
                        if (isAdded) Snackbar.make(
                            binding.root, "Delete failed: ${e.message}", Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
