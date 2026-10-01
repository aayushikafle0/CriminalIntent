package com.example.criminalintent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.databinding.ListItemCrimeBinding
import java.text.DateFormat
import java.util.Date

class CrimeListAdapter(
    private val onCrimeClicked: (String) -> Unit
) : ListAdapter<Crime, CrimeListAdapter.CrimeHolder>(CrimeDiffCallback()) {

    init {
        stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CrimeHolder {
        val binding = ListItemCrimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CrimeHolder(binding)
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CrimeHolder(
        private val binding: ListItemCrimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(crime: Crime) {
            binding.crimeTitle.text =
                crime.title.ifBlank { "Untitled crime" }

            binding.crimeDate.text =
                DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(Date(crime.date))

            binding.crimeSolved.isVisible = crime.isSolved

            binding.root.setOnClickListener {
                onCrimeClicked(crime.id)
            }
        }
    }
}

private class CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {

    override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
        return oldItem == newItem
    }
}