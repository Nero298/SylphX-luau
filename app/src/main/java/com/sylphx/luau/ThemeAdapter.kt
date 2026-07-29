package com.sylphx.luau

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sylphx.luau.databinding.ItemThemeBinding

class ThemeAdapter(
    private val themes: List<AppTheme>,
    private var selectedId: String,
    private val onThemeSelected: (AppTheme) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(val binding: ItemThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.binding.themePreview.setTheme(theme)
        holder.binding.themeName.text = theme.displayName

        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(theme.dotColor)
        }
        holder.binding.themeDot.background = dot

        val isSelected = theme.id == selectedId
        holder.binding.themeCard.strokeWidth = if (isSelected) 4 else 0
        holder.binding.themeCard.strokeColor = theme.dotColor
        holder.binding.themeCard.cardElevation = if (isSelected) 6f else 0f

        holder.binding.root.setOnClickListener { view ->
            val previousSelected = selectedId
            selectedId = theme.id
            onThemeSelected(theme)
            notifyItemChanged(themes.indexOfFirst { it.id == previousSelected })
            notifyItemChanged(position)

            view.animate()
                .scaleX(0.93f).scaleY(0.93f)
                .setDuration(90)
                .withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                .start()
        }
    }

    override fun getItemCount(): Int = themes.size
}
