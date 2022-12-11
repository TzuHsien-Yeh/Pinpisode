package com.tzuhsien.pinpisode.search.result

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.tzuhsien.pinpisode.Constants.AUTH_CODE
import com.tzuhsien.pinpisode.Constants.CLIENT_ID
import com.tzuhsien.pinpisode.Constants.PARAM_CODE
import com.tzuhsien.pinpisode.Constants.PARAM_CODE_CHALLENGE
import com.tzuhsien.pinpisode.Constants.PARAM_CODE_CHALLENGE_METHOD
import com.tzuhsien.pinpisode.Constants.PARAM_CODE_VERIFIER
import com.tzuhsien.pinpisode.Constants.PARAM_GRANT_TYPE
import com.tzuhsien.pinpisode.Constants.REDIRECT_URI
import com.tzuhsien.pinpisode.Constants.S256
import com.tzuhsien.pinpisode.Constants.SCOPE_LIBRARY_READ
import com.tzuhsien.pinpisode.Constants.SCOPE_READ_PLAYBACK_POSITION
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.databinding.FragmentSearchResultBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util.CODE_VERIFIER
import com.tzuhsien.pinpisode.util.Util.getCodeChallenge
import timber.log.Timber


class SearchResultFragment : Fragment() {

    companion object {
        const val SOURCE_KEY = "sourceKey"
        const val REQUEST_KEYWORD_1 = "requestKey1"
        const val REQUEST_KEYWORD_2 = "requestKey2"
        const val KEY_QUERY = "query"
    }

    private val viewModel by viewModels<SearchResultViewModel> { getVmFactory() }
    private lateinit var binding: FragmentSearchResultBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSearchResultBinding.inflate(layoutInflater)

        viewModel.setViewPagerSource(requireArguments().getSerializable(SOURCE_KEY) as Source)

        if (viewModel.source == Source.YOUTUBE) {
            // Receive query string from Search Fragment
            requireActivity().supportFragmentManager.setFragmentResultListener(
                REQUEST_KEYWORD_1,
                this
            ) { _, bundle ->
                if (null != bundle.getString(KEY_QUERY)) {
                    viewModel.searchOnYouTube(bundle.getString(KEY_QUERY))
                } else {
                    viewModel.emptySearchResultLists()
                }
                Timber.d("setFragmentResultListener: ${bundle.getString(KEY_QUERY)}")
            }
        } else {
            // Receive query string from Search Fragment
            requireActivity().supportFragmentManager.setFragmentResultListener(
                REQUEST_KEYWORD_2,
                this
            ) { _, bundle ->
                if (null != bundle.getString(KEY_QUERY)) {
                    viewModel.searchOnSpotify(bundle.getString(KEY_QUERY))
                    viewModel.queryKeyword = bundle.getString(KEY_QUERY)
                } else {
                    viewModel.emptySearchResultLists()
                }
                Timber.d("setFragmentResultListener: ${bundle.getString(KEY_QUERY)}")
            }
        }

        binding.recyclerviewYtSearchResult.visibility =
            if (viewModel.source == Source.YOUTUBE) View.VISIBLE else View.GONE
        binding.recyclerviewSpSearchResult.visibility =
            if (viewModel.source == Source.SPOTIFY) View.VISIBLE else View.GONE

        val resultAdapter = YtSearchResultAdapter(viewModel.uiState)
        binding.recyclerviewYtSearchResult.adapter = resultAdapter
        viewModel.ytSearchResultList.observe(viewLifecycleOwner) {
            resultAdapter.submitList(it)
        }

        val spResultAdapter = SpotifySearchResultAdapter(viewModel.uiState)
        binding.recyclerviewSpSearchResult.adapter = spResultAdapter
        viewModel.spSearchResultList.observe(viewLifecycleOwner) {
            spResultAdapter.submitList(it)
        }

        if (viewModel.source == Source.SPOTIFY && null == viewModel.getSpotifyAuthToken()) {
            binding.btnSpotifyAuth.visibility = View.VISIBLE
        }

        binding.btnSpotifyAuth.setOnClickListener {
            showLoginActivityCode.launch(getLoginActivityCodeIntent())
            if (null != viewModel.getSpotifyAuthToken()) {
                viewModel.doneRequestSpotifyAuthToken()
            }
        }
        viewModel.needSpotifyAuth.observe(viewLifecycleOwner) {
            Timber.d("viewModel.needSpotifyAuth.observe: $it")
            binding.btnSpotifyAuth.visibility = if (it) View.VISIBLE else View.GONE

            if (!it) {
                viewModel.searchOnSpotify(viewModel.queryKeyword)
            }
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner) {
            it?.let {
                findNavController().navigate(
                    NavGraphDirections.actionGlobalYouTubeNoteFragment(
                        videoIdKey = it
                    )
                )
                viewModel.doneNavigation()
            }
        }

        viewModel.navigateToSpotifyNote.observe(viewLifecycleOwner) {
            it?.let {
                Timber.d("viewModel.navigateToSpotifyNote.observe: $it")
                findNavController().navigate(
                    NavGraphDirections.actionGlobalSpotifyNoteFragment(
                        sourceIdKey = it
                    )
                )
                viewModel.doneNavigation()
            }
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

    /**
     *  Spotify Auth flow
     * */
    private fun getLoginActivityCodeIntent(): Intent =
        AuthorizationClient.createLoginActivityIntent(
            activity,
            AuthorizationRequest.Builder(CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                REDIRECT_URI)
                .setScopes(
                    arrayOf(
                        SCOPE_READ_PLAYBACK_POSITION,
                        SCOPE_LIBRARY_READ,
                    )
                )
                .setCustomParam(PARAM_CODE_CHALLENGE_METHOD, S256)
                .setCustomParam(PARAM_CODE_CHALLENGE, getCodeChallenge(CODE_VERIFIER))
                .build()
        )

    private val showLoginActivityCode = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        val authorizationResponse =
            AuthorizationClient.getResponse(result.resultCode, result.data)

        when (authorizationResponse.type) {
            AuthorizationResponse.Type.CODE -> {
                // Here You will get the authorization code which you
                // can get with authorizationResponse.code

                showLoginActivityToken.launch(getLoginActivityTokenIntent(authorizationResponse.code))
            }
            AuthorizationResponse.Type.ERROR -> {
                Timber.d("AuthorizationResponse.Type.ERROR")
            }

            else -> {} // Probably interruption
        }
    }

    private fun getLoginActivityTokenIntent(code: String): Intent =
        AuthorizationClient.createLoginActivityIntent(
            activity,
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(
                    arrayOf(
                        SCOPE_READ_PLAYBACK_POSITION,
                        SCOPE_LIBRARY_READ,
                    )
                )
                .setCustomParam(PARAM_GRANT_TYPE, AUTH_CODE)
                .setCustomParam(PARAM_CODE, code)
                .setCustomParam(PARAM_CODE_VERIFIER, CODE_VERIFIER)
                .build()
        )

    private val showLoginActivityToken = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        val authorizationResponse =
            AuthorizationClient.getResponse(result.resultCode, result.data)

        when (authorizationResponse.type) {
            AuthorizationResponse.Type.TOKEN -> {
                // Here You can get access to the authorization token
                // with authorizationResponse.token

                Timber.d("showLoginActivityToken authorizationResponse.expiresIn: ${authorizationResponse.expiresIn}")
                Timber.d("authorizationResponse.accessToken = ${authorizationResponse.accessToken}")
                viewModel.saveSpotifyAuthToken(authorizationResponse.accessToken)
                viewModel.doneRequestSpotifyAuthToken()
            }
            AuthorizationResponse.Type.ERROR -> {
                Timber.d("showLoginActivityToken : AuthorizationResponse.Type.ERROR")
            }
            else -> {} // Probably interruption
        }
    }

}