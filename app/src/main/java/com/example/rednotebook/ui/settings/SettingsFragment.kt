package com.example.rednotebook.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rednotebook.data.network.ApiClient
import com.example.rednotebook.databinding.FragmentSettingsBinding
import com.example.rednotebook.data.repository.EntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "rednotebook_prefs"
        private const val KEY_DARK_MODE = "dark_mode"

        fun isDarkMode(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false)
        }

        fun applyTheme(context: android.content.Context) {
            val darkMode = isDarkMode(context)
            AppCompatDelegate.setDefaultNightMode(
                if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore saved URL
        val saved = ApiClient.getSavedUrl(requireContext())
        binding.etApiUrl.setText(saved)

        // Restore dark mode toggle state
        binding.switchDarkMode.isChecked = isDarkMode(requireContext())

        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DARK_MODE, isChecked).apply()

            // Apply immediately — recreates the activity
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Save & test connection
        binding.btnSave.setOnClickListener {
            val raw = binding.etApiUrl.text.toString().trim()
            if (raw.isBlank()) {
                showStatus("Please enter a URL.", success = false)
                return@setOnClickListener
            }
            val url = if (raw.endsWith("/")) raw else "$raw/"
            binding.btnSave.isEnabled = false
            showStatus("Checking connection…", success = null)

            viewLifecycleOwner.lifecycleScope.launch {
                val reachable = testUrl(url)
                if (reachable) {
                    ApiClient.saveUrl(requireContext(), url)
                    ApiClient.buildApi(url)
                    // Reset sync flag so all entries are re-fetched from the new server
                    EntryRepository(requireContext()).resetInitialSync()
                    showStatus("✓ Connected and saved!", success = true)
                } else {
                    showStatus(
                        "✗ Could not reach that URL. Check the address and that your server is running.",
                        success = false
                    )
                }
                binding.btnSave.isEnabled = true
            }
        }
    }

    private suspend fun testUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${url}months").openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout    = 5000
            connection.requestMethod  = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    private fun showStatus(message: String, success: Boolean?) {
        binding.tvStatus.text = message
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.setTextColor(
            resources.getColor(
                when (success) {
                    true  -> android.R.color.holo_green_light
                    false -> android.R.color.holo_red_light
                    null  -> android.R.color.darker_gray
                }, null
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
