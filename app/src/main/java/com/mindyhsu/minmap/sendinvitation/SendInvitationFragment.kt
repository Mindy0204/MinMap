package com.mindyhsu.minmap.sendinvitation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.databinding.FragmentSendInvitationBinding
import com.mindyhsu.minmap.ext.getVmFactory

class SendInvitationFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSendInvitationBinding
    private val viewModel by viewModels<SendInvitationViewModel> {
        getVmFactory(
            SendInvitationFragmentArgs.fromBundle(requireArguments()).eventLocation,
            SendInvitationFragmentArgs.fromBundle(requireArguments()).eventLocationName
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSendInvitationBinding.inflate(inflater, container, false)
        val adapter = SendInvitationAdapter(viewModel.sendInvitationUiState)
        binding.sendInvitationRecyclerView.adapter = adapter

        viewModel.userList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.sendInvitationButton.setOnClickListener {
            viewModel.sendEvent()
        }

        viewModel.isInvitationSuccess.observe(viewLifecycleOwner) {
            if (!it) {
                Toast.makeText(context, getString(R.string.send_invitation_fail), Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigateUp()
            }
        }

        viewModel.status.observe(viewLifecycleOwner) {
            Toast.makeText(context, getString(R.string.internet_not_connected), Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}