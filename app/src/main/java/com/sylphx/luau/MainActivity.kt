package com.sylphx.luau

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingTabSwitch: Runnable? = null

    /** Left-to-right order of the bottom-nav tabs, used to pick the slide direction. */
    private val tabOrder = listOf(
        ToolMode.DETECT, ToolMode.DEOBFUSCATE, ToolMode.OBFUSCATE, ToolMode.BEAUTIFY, ToolMode.SETTINGS
    )

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

    /** Small "magnetic pop" bounce on the tapped bottom-nav icon, then switches tabs. */
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
        val previousMode = currentMode
        currentMode = mode

        binding.tabDetect.isSelected = mode == ToolMode.DETECT
        binding.tabDeobfuscate.isSelected = mode == ToolMode.DEOBFUSCATE
        binding.tabObfuscate.isSelected = mode == ToolMode.OBFUSCATE
        binding.tabBeautify.isSelected = mode == ToolMode.BEAUTIFY
        binding.tabSettings.isSelected = mode == ToolMode.SETTINGS

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

        pendingTabSwitch?.let { mainHandler.removeCallbacks(it) }

        val swap = Runnable {
            val fragment: Fragment = if (mode == ToolMode.SETTINGS) {
                SettingsFragment()
            } else {
                ToolFragment.newInstance(mode)
            }

            val transaction = supportFragmentManager.beginTransaction()
            if (animateTab) {
                val movingForward = tabOrder.indexOf(mode) > tabOrder.indexOf(previousMode)
                if (movingForward) {
                    transaction.setCustomAnimations(
                        R.anim.fragment_slide_in_right,
                        R.anim.fragment_slide_out_left
                    )
                } else {
                    transaction.setCustomAnimations(
                        R.anim.fragment_slide_in_left,
                        R.anim.fragment_slide_out_right
                    )
                }
            }
            transaction.replace(R.id.fragmentContainer, fragment).commit()
        }
        pendingTabSwitch = swap

        // Tab selection state updates instantly; the actual content swap is
        // deliberately held for ~1s so switching tabs feels like a real
        // transition rather than an instant flash.
        if (animateTab) {
            mainHandler.postDelayed(swap, 1000L)
        } else {
            swap.run()
        }
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

    override fun onDestroy() {
        pendingTabSwitch?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }
}
