package com.tzuhsien.pinpisode.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.tzuhsien.pinpisode.MyApplication.Companion.applicationContext
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.FragmentProfileBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.glide
import com.tzuhsien.pinpisode.loading.BUNDLE_KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.REQUEST_KEY_DISMISS
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

        viewModel.updateLocalUserId()
        Timber.d("${UserManager.userId},${UserManager.userName},${UserManager.userEmail},${UserManager.userPic}")
        binding.textUserName.text = UserManager.userName
        binding.textUserEmail.text = UserManager.userEmail
        binding.imgProfilePic.glide(UserManager.userPic)

        binding.logOut.setOnClickListener {
            GoogleSignIn.getClient(applicationContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            UserManager.userId = null
            findNavController().navigate(NavGraphDirections.actionGlobalSignInFragment())
            Timber.d("User logged out: ${UserManager.userId}")
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