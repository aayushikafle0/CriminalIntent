package com.example.criminalintent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.criminalintent.databinding.FragmentCrimeDetailBinding
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val viewModel: CrimeDetailViewModel by viewModels()
    private var rendering = false
    private var displayedPhoto: String? = null

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val name = viewModel.pendingPhotoName
        if (name != null) {
            val file = photoFile(name)
            if (success && file.length() > 0) {
                viewModel.setPhoto(name)
            } else {
                file.delete()
            }
        }
        viewModel.pendingPhotoName = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCrimeDetailBinding.inflate(
            inflater, container, false
        )
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = requireNotNull(_binding)

        binding.crimeTitle.doAfterTextChanged { text ->
            if (!rendering) {
                viewModel.setTitle(text.toString())
            }
        }

        binding.crimeSolved.setOnCheckedChangeListener { _, checked ->
            if (!rendering) {
                viewModel.setSolved(checked)
            }
        }

        childFragmentManager.setFragmentResultListener(
            "date_selected",
            viewLifecycleOwner
        ) { _, result ->
            viewModel.setDate(result.getLong("date"))
        }

        binding.crimeDate.setOnClickListener {
            val crime = viewModel.uiState.value.crime
            if (crime != null &&
                !childFragmentManager.isStateSaved &&
                childFragmentManager.findFragmentByTag("date_picker") == null
            ) {
                DatePickerFragment.newInstance(crime.date)
                    .show(childFragmentManager, "date_picker")
            }
        }

        binding.takePhoto.setOnClickListener {
            launchCamera()
        }

        binding.sendReport.setOnClickListener {
            sendReport()
        }

        binding.saveCrime.setOnClickListener {
            viewModel.saveCrime()
        }

        binding.deleteCrime.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete crime?")
                .setMessage("This will remove this crime from your list.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCrime()
                }
                .show()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            val state = viewModel.uiState.value
            if (!state.busy) {
                if (state.crime == null) {
                    parentFragmentManager.popBackStack()
                } else {
                    viewModel.saveCrime()
                }
            }
        }

        render(viewModel.uiState.value)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.RESUMED
            ) {
                viewModel.uiState.collect { state ->
                    render(state)

                    state.error?.let {
                        Toast.makeText(
                            requireContext(), it, Toast.LENGTH_LONG
                        ).show()
                    }

                    if (state.finished &&
                        !parentFragmentManager.isStateSaved
                    ) {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "Crime details"
    }

    private fun render(state: CrimeDetailUiState) {
        val binding = _binding ?: return
        val enabled = state.crime != null &&
                !state.busy && !state.finished

        listOf(
            binding.crimeTitle,
            binding.crimeDate,
            binding.crimeSolved,
            binding.takePhoto,
            binding.sendReport,
            binding.saveCrime,
            binding.deleteCrime
        ).forEach {
            it.isEnabled = enabled
        }

        val crime = state.crime ?: return

        rendering = true
        if (binding.crimeTitle.text.toString() != crime.title) {
            binding.crimeTitle.setText(crime.title)
        }
        binding.crimeSolved.isChecked = crime.isSolved
        binding.crimeDate.text =
            DateFormat.getDateInstance(DateFormat.LONG)
                .format(Date(crime.date))
        rendering = false

        if (displayedPhoto != crime.photoFileName) {
            displayedPhoto = crime.photoFileName
            showPhoto(crime.photoFileName)
        }
    }

    private fun photoFile(name: String): File {
        val directory = File(requireContext().filesDir, "photos")
        directory.mkdirs()
        return File(directory, name)
    }

    private fun launchCamera() {
        val name = "crime_${UUID.randomUUID()}.jpg"
        val file = photoFile(name)

        try {
            file.createNewFile()
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            viewModel.pendingPhotoName = name
            takePhoto.launch(uri)
        } catch (exception: Exception) {
            viewModel.pendingPhotoName = null
            file.delete()
            Toast.makeText(
                requireContext(),
                "Unable to open the camera on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showPhoto(name: String?) {
        val binding = _binding ?: return
        binding.crimePhoto.setImageDrawable(null)
        if (name == null) return

        val file = photoFile(name)
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.path, bounds)

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1024 ||
                    bounds.outHeight / sampleSize > 1024
                ) {
                    sampleSize *= 2
                }

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeFile(file.path, options)
            }

            if (_binding === binding && displayedPhoto == name) {
                binding.crimePhoto.setImageBitmap(bitmap)
            }
        }
    }

    private fun sendReport() {
        val crime = viewModel.uiState.value.crime ?: return
        val date = DateFormat.getDateInstance(DateFormat.LONG)
            .format(Date(crime.date))
        val status = if (crime.isSolved) "Solved" else "Unsolved"

        val report = """
            Crime report
            Title: ${crime.title.ifBlank { "Untitled crime" }}
            Date: $date
            Status: $status
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Crime report")
            putExtra(Intent.EXTRA_TEXT, report)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send crime report"))
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "No sharing app is available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        displayedPhoto = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(crimeId: String): CrimeDetailFragment {
            return CrimeDetailFragment().apply {
                arguments = bundleOf("crime_id" to crimeId)
            }
        }
    }
}