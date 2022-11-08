package com.mindyhsu.minmap.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mindyhsu.minmap.databinding.FragmentNavigationSuccessBinding
import com.mindyhsu.minmap.ext.getVmFactory

class NavigationSuccessFragment : DialogFragment() {
    private val viewModel by viewModels<NavigationSuccessViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNavigationSuccessBinding.inflate(inflater, container, false)

        binding.successDialogButton.setOnClickListener {
            viewModel.finishEvent()
            findNavController().navigateUp()
        }

        return binding.root
    }
}