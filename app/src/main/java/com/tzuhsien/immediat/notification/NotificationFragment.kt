package com.tzuhsien.immediat.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tzuhsien.immediat.databinding.FragmentNotificationBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.loading.LoadingDialogDirections
import com.tzuhsien.immediat.network.LoadApiStatus

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

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when(it) {
                LoadApiStatus.LOADING -> {
                    findNavController().navigate(LoadingDialogDirections.actionGlobalLoadingDialog())
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to true))
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to false))
                }
            }
        }

        return binding.root
    }


}