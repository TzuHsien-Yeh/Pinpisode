package com.tzuhsien.pinpisode.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.FragmentNotificationBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.loading.BUNDLE_KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.REQUEST_KEY_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus

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
                viewModel.emptyAllInvitationData()
            }
        }

        val adapter = InvitationAdapter(viewModel.uiState)
        binding.recyclerviewInvitation.adapter = adapter
        viewModel.invitationsWithInviterInfo.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when(it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(NavGraphDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEY_DISMISS,
                        bundleOf(BUNDLE_KEY_DONE_LOADING to true))
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEY_DISMISS,
                        bundleOf(BUNDLE_KEY_DONE_LOADING to false))
                }
            }
        }

        return binding.root
    }
}