package com.example.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.criminalintent.databinding.FragmentCrimeListBinding
import kotlinx.coroutines.launch

class CrimeListFragment : Fragment() {

    private var _binding: FragmentCrimeListBinding? = null
    private val viewModel: CrimeListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCrimeListBinding.inflate(
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

        val crimeAdapter = CrimeListAdapter { crimeId ->
            openCrime(crimeId)
        }

        binding.crimeRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        binding.crimeRecyclerView.adapter = crimeAdapter

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater
                ) {
                    menu.add(Menu.NONE, 1, Menu.NONE, "Add")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }

                override fun onMenuItemSelected(
                    menuItem: MenuItem
                ): Boolean {
                    if (menuItem.itemId != 1) return false

                    viewModel.addCrime { crimeId ->
                        if (isAdded) {
                            openCrime(crimeId)
                        }
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.crimes.collect { crimes ->
                    crimeAdapter.submitList(crimes)
                    binding.emptyText.isVisible = crimes.isEmpty()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "CriminalIntent"
    }

    private fun openCrime(crimeId: String) {
        parentFragmentManager.setFragmentResult(
            "open_crime",
            bundleOf("crime_id" to crimeId)
        )
    }

    override fun onDestroyView() {
        _binding?.crimeRecyclerView?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}