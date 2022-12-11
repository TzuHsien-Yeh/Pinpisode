package com.tzuhsien.pinpisode.signin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.FragmentSignInBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.signin.SignInViewModel.Companion.GOOGLE_SIGN_IN
import timber.log.Timber


class SignInFragment : Fragment() {

    private val viewModel by viewModels<SignInViewModel> { getVmFactory() }
    private lateinit var binding: FragmentSignInBinding
//    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        viewModel.source = SignInFragmentArgs.fromBundle(requireArguments()).source
        viewModel.sourceId = SignInFragmentArgs.fromBundle(requireArguments()).sourceId
        Timber.d("source: ${viewModel.source}, sourceId: ${viewModel.sourceId}")

        binding = FragmentSignInBinding.inflate(layoutInflater)

        Glide.with(binding.appIcon)
            .load(R.raw.pinpisode_logo_with_text)
            .into(binding.appIcon)

        binding.btnSignIn.setOnClickListener { signIn() }

        viewModel.msg.observe(viewLifecycleOwner) {
            it?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }

        viewModel.navigateUp.observe(viewLifecycleOwner) {
            Timber.d("viewModel.navigateUp: $it")
            if (it) {
                findNavController().navigate(NavGraphDirections.actionGlobalNoteListFragment())
                viewModel.doneNavigation()
            }
        }

        viewModel.navigateToYtNote.observe(viewLifecycleOwner) {
            Timber.d("navigateToYtNote: $it")
            it?.let {
                findNavController().navigate(NavGraphDirections.actionGlobalYouTubeNoteFragment(
                    videoIdKey = it))
                viewModel.doneNavigation()
            }
        }

        viewModel.navigateToSpNote.observe(viewLifecycleOwner) {
            Timber.d("navigateToSpNote: $it")
            it?.let {
                findNavController().navigate(NavGraphDirections.actionGlobalSpotifyNoteFragment(
                    sourceIdKey = it))
                viewModel.doneNavigation()
            }
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

    private fun signIn() {
        val signInIntent = viewModel.googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == GOOGLE_SIGN_IN) {
            val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            viewModel.handleSignInResult(completedTask)
        }
    }

}