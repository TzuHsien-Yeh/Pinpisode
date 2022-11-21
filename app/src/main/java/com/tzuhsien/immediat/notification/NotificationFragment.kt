package com.tzuhsien.immediat.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tzuhsien.immediat.databinding.FragmentNotificationBinding
import com.tzuhsien.immediat.ext.getVmFactory

class NotificationFragment : Fragment() {
    private val viewModel by viewModels<NotificationViewModel> {
        getVmFactory()
    }
    private lateinit var binding: FragmentNotificationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentNotificationBinding.inflate(layoutInflater)

        viewModel.invitationList.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.getInvitersInfo(it)
            } else {
                viewModel.emptyFullInvitationData()
            }
        }

        val adapter = InvitationAdapter(viewModel.uiState)
        binding.recyclerviewInvitation.adapter = adapter
        viewModel.fullInvitationData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }


}