package com.example.labourbook.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.model.WorkerModel
import com.example.labourbook.R

class WorkerAdapter(
    private val workerList: List<WorkerModel>,
    private val onBookClick: (WorkerModel) -> Unit
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar: TextView = itemView.findViewById(R.id.tvWorkerAvatar)
        val tvRating: TextView = itemView.findViewById(R.id.tvWorkerRating)
        val tvName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvSpecialty: TextView = itemView.findViewById(R.id.tvWorkerSpecialty)
        val tvRate: TextView = itemView.findViewById(R.id.tvWorkerRate)
        val tvLocation: TextView? = itemView.findViewById(R.id.tvWorkerLocation)
        val btnBook: Button = itemView.findViewById(R.id.btnBookWorker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_worker, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val worker = workerList[position]
        holder.tvAvatar.text = worker.avatarEmoji
        holder.tvRating.text = worker.rating
        holder.tvName.text = worker.name
        holder.tvSpecialty.text = worker.specialty
        holder.tvRate.text = worker.rate
        holder.tvLocation?.text = worker.address

        holder.btnBook.setOnClickListener {
            onBookClick(worker)
        }
    }

    override fun getItemCount(): Int = workerList.size
}