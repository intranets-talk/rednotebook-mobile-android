package com.example.rednotebook.ui.editor

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rednotebook.data.network.ApiClient
import com.example.rednotebook.data.repository.EntryRepository
import com.example.rednotebook.databinding.FragmentEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.rednotebook.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.DateFormatSymbols

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: EntryRepository

    private var year = 0; private var month = 0; private var day = 0
    private var isEditing = false
    private var currentText = ""

    // Photo picker launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

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
        binding.btnInsertImage.setOnClickListener { pickImage.launch("image/*") }

        loadContent()
    }

    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val entry = repo.getEntry(year, month, day)
                currentText = entry?.text ?: ""
                if (isAdded) populateViews(currentText)
            } catch (_: Exception) {
                if (isAdded) populateViews("")
            }
        }
    }

    private fun populateViews(text: String) {
        currentText = text
        EntryRenderer.render(requireContext(), text, binding.contentContainer)
        binding.etContent.setText(text)
    }

    private fun setEditMode(editing: Boolean) {
        isEditing = editing
        if (editing) {
            binding.etContent.setText(currentText)
            binding.scrollView.visibility = View.GONE
            binding.etContent.visibility  = View.VISIBLE
            binding.etContent.requestFocus()
        } else {
            currentText = binding.etContent.text.toString()
            EntryRenderer.render(requireContext(), currentText, binding.contentContainer)
            binding.etContent.visibility  = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }
        binding.btnEditTop.setImageResource(
            if (editing) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit)
        binding.editToolbar.visibility = if (editing) View.VISIBLE else View.GONE
    }

    private fun uploadImage(uri: Uri) {
        if (!ApiClient.isConfigured()) {
            Snackbar.make(binding.root, "API not configured — cannot upload image", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.uploadProgress.visibility = View.VISIBLE
        binding.btnInsertImage.isEnabled  = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val date = "%04d-%02d-%02d".format(year, month, day)

                // Read image bytes from URI
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.readBytes()
                } ?: throw Exception("Could not read image")

                // Detect MIME type
                val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png"  -> "png"
                    "image/gif"  -> "gif"
                    "image/webp" -> "webp"
                    else         -> "jpg"
                }

                // Build multipart request
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file", "image.$ext", requestBody
                )

                // Upload
                val response = ApiClient.api.uploadAttachment(date, part)

                // Build the full URL for embedding
                val baseUrl = ApiClient.getSavedUrl(requireContext()).trimEnd('/')
                val imageUrlWithoutExt = "$baseUrl${response.url}".removeSuffix(".$ext")
                val imageTag = "\n[\"\"$imageUrlWithoutExt\"\".$ext]\n"

                // Insert at cursor position or at end
                if (isAdded) {
                    val editText = binding.etContent
                    val cursor   = editText.selectionEnd.takeIf { it >= 0 } ?: editText.text.length
                    editText.text.insert(cursor, imageTag)
                    Snackbar.make(binding.root, "Image inserted!", Snackbar.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                if (isAdded) Snackbar.make(
                    binding.root, "Upload failed: ${e.message}", Snackbar.LENGTH_LONG
                ).show()
            } finally {
                if (isAdded) {
                    binding.uploadProgress.visibility = View.GONE
                    binding.btnInsertImage.isEnabled  = true
                }
            }
        }
    }

    private fun saveEntry() {
        val text = binding.etContent.text.toString()
        binding.btnEditTop.isEnabled = false
        binding.btnSave.isEnabled    = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.saveEntry(year, month, day, text)
                if (!isAdded) return@launch
                currentText = text
                EntryRenderer.render(requireContext(), text, binding.contentContainer)
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
