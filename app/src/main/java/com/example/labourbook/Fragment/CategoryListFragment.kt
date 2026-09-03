package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.Adapter.LabourCategoryAdapter
import com.example.labourbook.R
import com.example.labourbook.network.LabourCategoryRepository
import kotlinx.coroutines.launch

class CategoryListFragment : Fragment() {

    private lateinit var categoryAdapter: LabourCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_category_list,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val recyclerCategoryList =
            view.findViewById<RecyclerView>(R.id.recyclerCategoryList)

        recyclerCategoryList.layoutManager =
            GridLayoutManager(requireContext(), 2)

        categoryAdapter = LabourCategoryAdapter(
            categoryList = emptyList(),
            layoutResId = R.layout.item_labour_category_grid
        ) { category ->
            val bundle = Bundle().apply {
                putString("selectedCategoryId", category.Id)
                putString("selectedCategoryName", category.Name)
            }
            findNavController().navigate(
                R.id.action_categoryList_to_workerList,
                bundle
            )
        }

        recyclerCategoryList.adapter = categoryAdapter

        val etSearchCategory =
            view.findViewById<EditText>(R.id.etSearchCategory)

        etSearchCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                categoryAdapter.filterList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadLabourCategories()
    }

    private fun loadLabourCategories() {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString("access_token", null)

        val instanceUrl =
            sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please connect Salesforce first",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = LabourCategoryRepository.getLabourCategories(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken
                )

                if (response != null && response.records.isNotEmpty()) {
                    categoryAdapter.updateList(response.records)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No categories found in Salesforce",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error loading categories: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
