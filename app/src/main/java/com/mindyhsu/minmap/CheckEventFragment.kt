package com.mindyhsu.minmap

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mindyhsu.minmap.databinding.FragmentCheckEventBinding

class CheckEventFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentCheckEventBinding
    private lateinit var viewModel: CheckEventViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCheckEventBinding.inflate(inflater, container, false)

        val eventDetail = CheckEventFragmentArgs.fromBundle(requireArguments()).eventDetail
        viewModel = CheckEventViewModel(eventDetail)

        binding.checkMeetTime.text = getString(R.string.meeting_time_at, viewModel.eventTime.toString())
        binding.checkMeetLocation.text = getString(R.string.meeting_point_at, viewModel.eventLocation)

        binding.checkEventButton.setOnClickListener {
            findNavController().navigate(MapFragmentDirections.navigateToMapFragment(true))
        }

        return binding.root
    }
}