package com.example.labourbook.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.R
import com.example.labourbook.model.LabourCategoryModel

class LabourCategoryAdapter(
    private var categoryList: List<LabourCategoryModel>,
    private val layoutResId: Int = R.layout.item_labour_category,
    private val onCategoryClick: (LabourCategoryModel) -> Unit
) : RecyclerView.Adapter<LabourCategoryAdapter.CategoryViewHolder>() {

    private var fullList: List<LabourCategoryModel> = categoryList

    class CategoryViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val tvCategoryIcon: TextView =
            itemView.findViewById(R.id.tvCategoryIcon)

        val tvCategoryName: TextView =
            itemView.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                layoutResId,
                parent,
                false
            )

        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {

        val category = categoryList[position]

        holder.tvCategoryName.text = category.Name
        holder.tvCategoryIcon.text = getCategoryEmoji(category.Name)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    fun updateList(
        newList: List<LabourCategoryModel>
    ) {
        fullList = newList
        categoryList = newList
        notifyDataSetChanged()
    }

    fun filterList(query: String) {
        categoryList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter {
                it.Name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    private fun getCategoryEmoji(categoryName: String?): String {
        if (categoryName.isNullOrBlank()) return "👷"
        val name = categoryName.lowercase()
        return when {
            name.contains("plumb") -> "🪠"
            name.contains("electr") -> "⚡"
            name.contains("paint") -> "🎨"
            name.contains("carpent") -> "🪚"
            name.contains("clean") -> "🧹"
            name.contains("mason") || name.contains("construct") || name.contains("build") -> "🧱"
            name.contains("gard") || name.contains("lawn") -> "🌱"
            name.contains("mechan") || name.contains("auto") -> "🔧"
            name.contains("weld") -> "👨‍🏭"
            name.contains("driver") -> "🚗"
            else -> "👷"
        }
    }
}