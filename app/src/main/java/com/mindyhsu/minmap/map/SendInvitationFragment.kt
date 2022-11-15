package com.mindyhsu.minmap.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.databinding.FragmentSendInvitationBinding
import com.mindyhsu.minmap.ext.getVmFactory

class SendInvitationFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSendInvitationBinding
    private val viewModel by viewModels<SendInvitationViewModel> { getVmFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSendInvitationBinding.inflate(inflater, container, false)
        val adapter = SendInvitationAdapter()
        binding.sendInvitationRecyclerView.adapter = adapter

        viewModel.userList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        return binding.root
    }
}