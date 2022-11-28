package com.tzuhsien.pinpisode.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.tzuhsien.pinpisode.MyApplication.Companion.applicationContext
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.FragmentProfileBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.signin.SignInFragmentDirections
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
        Glide.with(binding.imgProfilePic)
            .load(UserManager.userPic)
            .into(binding.imgProfilePic)

        binding.logOut.setOnClickListener {
            GoogleSignIn.getClient(applicationContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            UserManager.userId = null
            findNavController().navigate(SignInFragmentDirections.actionGlobalSignInFragment())
            Timber.d("User logged out: ${UserManager.userId}")
        }

        return binding.root
    }

}