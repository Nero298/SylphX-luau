package com.sylphx.luau

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sylphx.luau.databinding.ActivityMainBinding

enum class ToolMode { DETECT, DEOBFUSCATE, OBFUSCATE, BEAUTIFY, SETTINGS }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentMode: ToolMode = ToolMode.DETECT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyBackground()

        binding.tabDetect.setOnClickListener { onTabClicked(it, ToolMode.DETECT) }
        binding.tabDeobfuscate.setOnClickListener { onTabClicked(it, ToolMode.DEOBFUSCATE) }
        binding.tabObfuscate.setOnClickListener { onTabClicked(it, ToolMode.OBFUSCATE) }
        binding.tabBeautify.setOnClickListener { onTabClicked(it, ToolMode.BEAUTIFY) }
        binding.tabSettings.setOnClickListener { onTabClicked(it, ToolMode.SETTINGS) }

        if (savedInstanceState == null) {
            selectTab(ToolMode.DETECT, animateTab = false)
        }
    }

    /** Small "magnetic pop" bounce on the tapped sidebar icon, then switches tabs. */
    private fun onTabClicked(view: View, mode: ToolMode) {
        if (mode == currentMode) return
        view.animate()
            .scaleX(1.12f).scaleY(1.12f)
            .setInterpolator(OvershootInterpolator())
            .setDuration(140)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            .start()
        selectTab(mode, animateTab = true)
    }

    private fun selectTab(mode: ToolMode, animateTab: Boolean) {
        currentMode = mode

        binding.tabDetect.isSelected = mode == ToolMode.DETECT
        binding.tabDeobfuscate.isSelected = mode == ToolMode.DEOBFUSCATE
        binding.tabObfuscate.isSelected = mode == ToolMode.OBFUSCATE
        binding.tabBeautify.isSelected = mode == ToolMode.BEAUTIFY
        binding.tabSettings.isSelected = mode == ToolMode.SETTINGS

        binding.glowDetect.isSelected = mode == ToolMode.DETECT
        binding.glowDeobfuscate.isSelected = mode == ToolMode.DEOBFUSCATE
        binding.glowObfuscate.isSelected = mode == ToolMode.OBFUSCATE
        binding.glowBeautify.isSelected = mode == ToolMode.BEAUTIFY
        binding.glowSettings.isSelected = mode == ToolMode.SETTINGS

        binding.iconDetect.isSelected = mode == ToolMode.DETECT
        binding.iconDeobfuscate.isSelected = mode == ToolMode.DEOBFUSCATE
        binding.iconObfuscate.isSelected = mode == ToolMode.OBFUSCATE
        binding.iconBeautify.isSelected = mode == ToolMode.BEAUTIFY
        binding.iconSettings.isSelected = mode == ToolMode.SETTINGS

        binding.labelDetect.isSelected = mode == ToolMode.DETECT
        binding.labelDeobfuscate.isSelected = mode == ToolMode.DEOBFUSCATE
        binding.labelObfuscate.isSelected = mode == ToolMode.OBFUSCATE
        binding.labelBeautify.isSelected = mode == ToolMode.BEAUTIFY
        binding.labelSettings.isSelected = mode == ToolMode.SETTINGS

        binding.currentTabLabel.text = when (mode) {
            ToolMode.DETECT -> "Detect"
            ToolMode.DEOBFUSCATE -> "Deobfuscate"
            ToolMode.OBFUSCATE -> "Obfuscate"
            ToolMode.BEAUTIFY -> "Beautify"
            ToolMode.SETTINGS -> "Settings"
        }

        val fragment: Fragment = if (mode == ToolMode.SETTINGS) {
            SettingsFragment()
        } else {
            ToolFragment.newInstance(mode)
        }

        val transaction = supportFragmentManager.beginTransaction()
        if (animateTab) {
            transaction.setCustomAnimations(
                R.anim.fragment_fade_slide_in,
                R.anim.fragment_fade_slide_out
            )
        }
        transaction.replace(R.id.fragmentContainer, fragment).commit()
    }

    override fun onResume() {
        super.onResume()
        applyBackground()
    }

    /**
     * Re-applies whichever background the user picked in Settings: either a
     * custom uploaded image/GIF (with a dark scrim so text stays legible),
     * or the animated gradient of the selected theme.
     */
    fun applyBackground() {
        val customPath = ThemeManager.getCustomBackgroundPath(this)
        if (customPath != null) {
            binding.animatedBackground.visibility = View.GONE
            binding.customBackgroundImage.visibility = View.VISIBLE
            Glide.with(this).load(java.io.File(customPath)).into(binding.customBackgroundImage)
        } else {
            binding.customBackgroundImage.visibility = View.GONE
            binding.animatedBackground.visibility = View.VISIBLE
            binding.animatedBackground.setTheme(ThemeManager.getSelectedTheme(this))
        }
    }
}
