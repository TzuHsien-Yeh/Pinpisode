package com.tzuhsien.pinpisode.profile

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
import com.tzuhsien.pinpisode.databinding.FragmentProfileBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.glide
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import timber.log.Timber

class ProfileFragment : Fragment() {

    private val viewModel by viewModels<ProfileViewModel> { getVmFactory() }
    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        binding.textUserName.text = viewModel.getCurrentUser()?.name
        binding.textUserEmail.text = viewModel.getCurrentUser()?.email
        binding.imgProfilePic.glide(viewModel.getCurrentUser()?.pic)

        binding.logOut.setOnClickListener {
            viewModel.signOut()
            viewModel.updateUser()
            findNavController().navigate(NavGraphDirections.actionGlobalSignInFragment())
            Timber.d("User logged out: ${viewModel.getCurrentUser()}")
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(NavGraphDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_DISMISS,
                        bundleOf(KEY_DONE_LOADING to true))
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_DISMISS,
                        bundleOf(KEY_DONE_LOADING to false))
                }
            }
        }

        return binding.root
    }

}