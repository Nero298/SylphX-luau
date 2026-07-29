package com.sylphx.luau

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.sylphx.luau.databinding.FragmentToolBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ToolFragment : Fragment(R.layout.fragment_tool) {

    private var _binding: FragmentToolBinding? = null
    private val binding get() = _binding!!

    private lateinit var mode: ToolMode
    private var selectedPreset: String = "RobloxExecutor"
    private var selectedDeobfEngine: LeakDApi.Endpoint = LeakDApi.Endpoint.MOONSEC

    private val obfuscatePresets = listOf(
        "RobloxExecutor", "RobloxStudio", "Lua51", "Lua52", "Lua53", "Lua54"
    )

    private val deobfEngines = listOf(
        "Moonsec" to LeakDApi.Endpoint.MOONSEC,
        "Prometheus" to LeakDApi.Endpoint.PROMETHEUS,
        "IronBrew2" to LeakDApi.Endpoint.IRONBREW2,
        "IronVeil" to LeakDApi.Endpoint.IRONVEIL
    )

    private val urlFetchClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Accept any text-like file: .lua, .txt, or anything else the user picks — we just read it as text.
    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { readPickedFile(it) }
        }
    }

    companion object {
        private const val ARG_MODE = "arg_mode"

        fun newInstance(mode: ToolMode): ToolFragment {
            val f = ToolFragment()
            f.arguments = Bundle().apply { putString(ARG_MODE, mode.name) }
            return f
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentToolBinding.bind(view)

        mode = ToolMode.valueOf(requireArguments().getString(ARG_MODE)!!)

        setupChipsForMode()
        setupInputHint()
        setupDetectButtonVisibility()
        setupUrlFetch()
        setupPanelFocusGlow()
        setupRunButtonPulse()

        binding.pickFileButton.setOnClickListener { launchFilePicker() }
        binding.runButton.setOnClickListener { runTool() }
        binding.copyButton.setOnClickListener { copyResult() }
        binding.saveButton.setOnClickListener { showSaveDialog() }
        binding.detectButton.setOnClickListener { runDetectOnly() }
        binding.clearInputButton.setOnClickListener { clearInput() }
        binding.clearOutputButton.setOnClickListener { clearOutput() }

        listOf(
            binding.pickFileButton, binding.copyButton, binding.saveButton,
            binding.detectButton, binding.clearInputButton, binding.clearOutputButton
        ).forEach { applyPressScale(it) }
    }

    /** Scale-down-on-press feedback for toolbar buttons, on top of their ripple background. */
    @Suppress("ClickableViewAccessibility")
    private fun applyPressScale(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    /** Input/Output panels get an expanding cyan border glow while the code field inside is focused. */
    private fun setupPanelFocusGlow() {
        binding.codeInput.setOnFocusChangeListener { _, hasFocus ->
            binding.inputPanelFrame.isActivated = hasFocus
        }
    }

    private var runPulseAnimator: ValueAnimator? = null

    /** Subtle breathing glow on the Run button so it never looks static, even when idle. */
    private fun setupRunButtonPulse() {
        runPulseAnimator = ValueAnimator.ofFloat(0.85f, 1f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                binding.runButton.alpha = it.animatedValue as Float
            }
            start()
        }
    }

    private fun setupInputHint() {
        binding.codeInput.hint = when (mode) {
            ToolMode.DETECT -> "Dán code muốn nhận diện loại obfuscator..."
            ToolMode.DEOBFUSCATE -> "Dán code đã bị obfuscate vào đây..."
            ToolMode.OBFUSCATE -> "Dán code Lua gốc muốn obfuscate..."
            ToolMode.BEAUTIFY -> "Dán code Lua muốn làm đẹp (format lại)..."
            ToolMode.SETTINGS -> ""
        }
    }

    /** Detect button is a standalone check, only shown on the Obfuscate tab. */
    private fun setupDetectButtonVisibility() {
        val showDetect = mode == ToolMode.OBFUSCATE
        binding.detectButton.visibility = if (showDetect) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupChipsForMode() {
        when (mode) {
            ToolMode.OBFUSCATE -> {
                binding.chipScrollView.visibility = android.view.View.VISIBLE
                binding.presetChipGroup.removeAllViews()
                obfuscatePresets.forEachIndexed { index, preset ->
                    val chip = Chip(requireContext()).apply {
                        text = preset
                        isCheckable = true
                        isChecked = index == 0
                        setOnClickListener { selectedPreset = preset }
                    }
                    binding.presetChipGroup.addView(chip)
                }
            }
            ToolMode.DEOBFUSCATE -> {
                binding.chipScrollView.visibility = android.view.View.VISIBLE
                binding.presetChipGroup.removeAllViews()
                deobfEngines.forEachIndexed { index, (label, endpoint) ->
                    val chip = Chip(requireContext()).apply {
                        text = label
                        isCheckable = true
                        isChecked = index == 0
                        setOnClickListener { selectedDeobfEngine = endpoint }
                    }
                    binding.presetChipGroup.addView(chip)
                }
            }
            else -> {
                binding.chipScrollView.visibility = android.view.View.GONE
            }
        }
    }

    /** URL field: user pastes a raw-code URL and hits Enter to fetch its contents into the input box. */
    private fun setupUrlFetch() {
        binding.urlInput.setOnEditorActionListener { _, actionId, event ->
            val isEnterPress = actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isEnterPress) {
                fetchFromUrl()
                true
            } else {
                false
            }
        }
    }

    private fun fetchFromUrl() {
        val url = binding.urlInput.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) return

        binding.statusLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
        binding.statusLabel.text = "Đang tải nội dung từ URL..."

        viewLifecycleOwner.lifecycleScope.launch {
            val fetched = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(url).get().build()
                    urlFetchClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.string() else null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (fetched != null) {
                binding.codeInput.setText(fetched)
                showPickedFileLabel("Đã nạp code từ URL")
                binding.statusLabel.text = ""
            } else {
                binding.statusLabel.setTextColor(resources.getColor(R.color.error_red, null))
                binding.statusLabel.text = "Không tải được nội dung từ URL này."
            }
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // Accept .lua, .txt and any other plain-text-ish file the user wants to pick.
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream", "*/*"))
        }
        filePicker.launch(intent)
    }

    private fun readPickedFile(uri: Uri) {
        try {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "script.lua"
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().readText()
                binding.codeInput.setText(text)
            }
            showPickedFileLabel("Đã nạp file: $name")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không đọc được file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Reveals the floating file/URL status chip with a quick fade-in. */
    private fun showPickedFileLabel(text: String) {
        binding.pickedFileLabel.text = text
        if (binding.pickedFileLabel.visibility != View.VISIBLE) {
            binding.pickedFileLabel.alpha = 0f
            binding.pickedFileLabel.visibility = View.VISIBLE
            binding.pickedFileLabel.animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun currentCode(): String = binding.codeInput.text?.toString()?.trim().orEmpty()

    private fun clearInput() {
        binding.codeInput.setText("")
        binding.urlInput.setText("")
        binding.pickedFileLabel.text = ""
        binding.pickedFileLabel.visibility = View.GONE
    }

    private fun clearOutput() {
        binding.resultOutput.setText("")
        binding.statusLabel.text = ""
        binding.outputSizeLabel.text = ""
        binding.outputSizeLabel.visibility = View.GONE
    }

    private fun runTool() {
        val code = currentCode()
        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có code để xử lý", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        binding.statusLabel.text = ""
        binding.resultOutput.setText("")

        viewLifecycleOwner.lifecycleScope.launch {
            val result = when (mode) {
                ToolMode.DETECT -> LeakDApi.sendFile(LeakDApi.Endpoint.DETECT, code)
                ToolMode.DEOBFUSCATE -> {
                    val raw = LeakDApi.sendFile(selectedDeobfEngine, code)
                    if (raw.success) raw.copy(message = WatermarkUtil.replaceFirstLineWatermark(raw.message)) else raw
                }
                ToolMode.OBFUSCATE -> {
                    val raw = LeakDApi.sendFile(LeakDApi.Endpoint.OBFUSCATE, code, preset = selectedPreset)
                    if (raw.success) raw.copy(message = WatermarkUtil.withObfuscateCredit(raw.message)) else raw
                }
                ToolMode.BEAUTIFY -> {
                    val raw = LeakDApi.sendFile(LeakDApi.Endpoint.BEAUTIFY, code)
                    if (raw.success) raw.copy(message = WatermarkUtil.withBeautifyCredit(raw.message)) else raw
                }
                ToolMode.SETTINGS -> LeakDApi.ApiResult(success = false, message = "N/A")
            }

            setLoading(false)
            renderResult(result)
        }
    }

    /** Obfuscate tab: standalone "Detect" button that just calls /detect and shows the raw result. */
    private fun runDetectOnly() {
        val code = currentCode()
        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có code để nhận diện", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        binding.statusLabel.text = ""

        viewLifecycleOwner.lifecycleScope.launch {
            val result = LeakDApi.sendFile(LeakDApi.Endpoint.DETECT, code)
            setLoading(false)
            if (result.success) {
                binding.statusLabel.setTextColor(resources.getColor(R.color.accent_cyan, null))
                binding.statusLabel.text = "Phát hiện: ${result.message} — ${result.extra}"
            } else {
                binding.statusLabel.setTextColor(resources.getColor(R.color.error_red, null))
                binding.statusLabel.text = "Lỗi: ${result.message}"
            }
        }
    }

    private fun renderResult(result: LeakDApi.ApiResult) {
        if (result.success) {
            binding.resultOutput.setText(result.message)
            binding.statusLabel.setTextColor(resources.getColor(R.color.accent_lime, null))
            binding.statusLabel.text = when (mode) {
                ToolMode.DETECT -> "Phát hiện: ${result.message} — ${result.extra}"
                else -> result.extra ?: "Xong."
            }
            val sizeKb = result.message.toByteArray().size / 1024.0
            binding.outputSizeLabel.text = "Output: %.2f KB".format(sizeKb)
            binding.outputSizeLabel.alpha = 0f
            binding.outputSizeLabel.visibility = View.VISIBLE
            binding.outputSizeLabel.animate().alpha(1f).setDuration(200).start()
        } else {
            binding.statusLabel.setTextColor(resources.getColor(R.color.error_red, null))
            val presetHint = result.presets?.joinToString(", ")
            binding.statusLabel.text = if (presetHint != null) {
                "Lỗi: ${result.message} (${presetHint})"
            } else {
                "Lỗi: ${result.message}"
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.runButton.isEnabled = !loading
        binding.detectButton.isEnabled = !loading

        // Run button "morph": shrink text away, reveal a centered spinner in its place.
        if (loading) {
            runPulseAnimator?.pause()
            binding.runButton.alpha = 1f
            binding.runButton.animate().scaleX(0.94f).scaleY(0.94f).setDuration(120).start()
            binding.progressBar.visibility = View.VISIBLE
            binding.runButton.text = ""
        } else {
            binding.runButton.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            binding.progressBar.visibility = View.GONE
            binding.runButton.text = "Chạy"
            runPulseAnimator?.resume()
        }

        // High-tech animated skeleton fills the Output panel while a request is in flight.
        binding.outputLoadingSkeleton.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.outputLoadingSkeleton.alpha = 0f
            binding.outputLoadingSkeleton.animate().alpha(1f).setDuration(180).start()
            animateSkeletonShimmer()
        } else {
            skeletonShimmerAnimator?.cancel()
        }
    }

    private var skeletonShimmerAnimator: ValueAnimator? = null

    private fun animateSkeletonShimmer() {
        skeletonShimmerAnimator?.cancel()
        skeletonShimmerAnimator = ValueAnimator.ofFloat(0.4f, 1f).apply {
            duration = 700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { binding.outputLoadingSkeleton.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun copyResult() {
        val text = binding.resultOutput.text?.toString().orEmpty()
        if (text.isEmpty()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SylPhX Luau result", text))
        Toast.makeText(requireContext(), "Đã copy", Toast.LENGTH_SHORT).show()
    }

    /** Shows a dialog to let the user name the output file before saving, defaulting per-mode. */
    private fun showSaveDialog() {
        val text = binding.resultOutput.text?.toString().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có kết quả để lưu", Toast.LENGTH_SHORT).show()
            return
        }

        val defaultName = when (mode) {
            ToolMode.DEOBFUSCATE -> "SylphXluauDeobfuscated.lua"
            ToolMode.OBFUSCATE -> "SylphXluauObfuscated.lua"
            ToolMode.BEAUTIFY -> "SylphXluauBeautified.lua"
            ToolMode.DETECT -> "SylphXluauDetectResult.txt"
            ToolMode.SETTINGS -> "SylphXluau.txt"
        }

        val input = EditText(requireContext()).apply {
            setText(defaultName)
            setSelection(defaultName.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Lưu file")
            .setMessage("Tên file để lưu:")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val fileName = input.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: defaultName
                saveResult(fileName, text)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveResult(fileName: String, text: String) {
        try {
            val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { it.write(text.toByteArray()) }
            Toast.makeText(requireContext(), "Đã lưu: ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi lưu file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        runPulseAnimator?.cancel()
        skeletonShimmerAnimator?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
