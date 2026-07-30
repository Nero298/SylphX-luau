package com.sylphx.luau

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.sylphx.luau.databinding.FragmentSettingsBinding
import java.io.File

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handlePickedImage(uri) }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.uploadBackgroundButton.setOnClickListener { launchImagePicker() }
        binding.clearBackgroundButton.setOnClickListener { clearBackground() }

        val currentTheme = ThemeManager.getSelectedTheme(requireContext())
        val adapter = ThemeAdapter(
            themes = AppTheme.ALL,
            selectedId = currentTheme.id
        ) { theme ->
            ThemeManager.setSelectedTheme(requireContext(), theme)
            (activity as? MainActivity)?.applyBackground()
        }

        binding.themeGrid.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.themeGrid.adapter = adapter

        updateBackgroundStatusLabel()
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        }
        imagePicker.launch(intent)
    }

    private fun handlePickedImage(uri: Uri) {
        try {
            val extension = requireContext().contentResolver.getType(uri)?.let { mime ->
                if (mime.contains("gif")) "gif" else "img"
            } ?: "img"
            val outFile = File(requireContext().filesDir, "custom_background_$extension")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            ThemeManager.setCustomBackgroundPath(requireContext(), outFile.absolutePath)
            (activity as? MainActivity)?.applyBackground()
            updateBackgroundStatusLabel()
            Toast.makeText(requireContext(), "Background image applied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearBackground() {
        ThemeManager.clearCustomBackground(requireContext())
        (activity as? MainActivity)?.applyBackground()
        updateBackgroundStatusLabel()
        Toast.makeText(requireContext(), "Background image cleared, using default theme", Toast.LENGTH_SHORT).show()
    }

    private fun updateBackgroundStatusLabel() {
        val path = ThemeManager.getCustomBackgroundPath(requireContext())
        binding.backgroundStatusLabel.text = if (path != null) {
            "Using custom background image"
        } else {
            "No custom background set — using animated theme"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
