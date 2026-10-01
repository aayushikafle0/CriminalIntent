package com.example.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class DatePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = requireArguments().getLong("date")

        return DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day)
                }

                parentFragmentManager.setFragmentResult(
                    "date_selected",
                    bundleOf("date" to selectedDate.timeInMillis)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        fun newInstance(date: Long): DatePickerFragment {
            return DatePickerFragment().apply {
                arguments = bundleOf("date" to date)
            }
        }
    }
}