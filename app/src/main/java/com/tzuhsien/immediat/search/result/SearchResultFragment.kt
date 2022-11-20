package com.tzuhsien.immediat.search.result

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.databinding.FragmentSearchResultBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom

class SearchResultFragment : Fragment() {

    private val viewModel by viewModels<SearchResultViewModel> { getVmFactory() }
    private lateinit var binding: FragmentSearchResultBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSearchResultBinding.inflate(layoutInflater)

        viewModel.source = requireArguments().getSerializable("sourceKey") as Source
        // Receive query string from Search Fragment
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "requestKey",
            this
        ) { requestKey, bundle ->

            Timber.d("setFragmentResultListener: ${bundle.getString("query")}")
            viewModel.search(bundle.getString("query")!!)
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

        if (viewModel.source == Source.SPOTIFY && UserManager.userSpotifyAuthToken.isEmpty()) {
//            binding.textNeedSpotifyAuth.visibility = View.VISIBLE
            binding.btnSpotifyAuth.visibility = View.VISIBLE
        }

        binding.btnSpotifyAuth.setOnClickListener {
            showLoginActivityCode.launch(getLoginActivityCodeIntent())
            if (UserManager.userSpotifyAuthToken.isNotEmpty()) {
                viewModel.doneRequestSpotifyAuthToken()
            }
        }
        viewModel.needSpotifyAuth.observe(viewLifecycleOwner) {
            Timber.d("viewModel.needSpotifyAuth.observe: $it")
            binding.btnSpotifyAuth.visibility = if (it) View.VISIBLE else View.GONE

            if (!it) {
                viewModel.searchOnSpotify()
            }
        }
//
//            binding.btnSpotifyAuth.visibility =
//                if (viewModel.source == Source.SPOTIFY && it) {
//                    Timber.d("binding.btnSpotifyAuth.visibility visible")
//                    View.VISIBLE
//                } else {
//                    View.GONE
//                }
//        }


            viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner) {
                it?.let {
                    findNavController().navigate(
                        YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(
                            videoIdKey = it
                        )
                    )
                    viewModel.doneNavigation()
                }
            }

            viewModel.navigateToSpotifyNote.observe(viewLifecycleOwner) {
                it?.let {
                    findNavController().navigate(
                        SpotifyNoteFragmentDirections.actionGlobalSpotifyNoteFragment(
                            sourceIdKey = it
                        )
                    )
                    viewModel.doneNavigation()
                }
            }
        return binding.root
    }

        /**
         *  Spotify Auth flow
         * */
        companion object {
            const val CLIENT_ID = "f6095c97a1ab4a7fb88b5ac5f2ba606d"
            const val REDIRECT_URI = "pinpisode://callback"

            val CODE_VERIFIER = getCodeVerifier()

            private fun getCodeVerifier(): String {
                val secureRandom = SecureRandom()
                val code = ByteArray(64)
                secureRandom.nextBytes(code)
                return Base64.encodeToString(
                    code,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            }

            fun getCodeChallenge(verifier: String): String {
                val bytes = verifier.toByteArray()
                val messageDigest = MessageDigest.getInstance("SHA-256")
                messageDigest.update(bytes, 0, bytes.size)
                val digest = messageDigest.digest()
                return Base64.encodeToString(
                    digest,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            }
        }

        fun getLoginActivityCodeIntent(): Intent =
            AuthorizationClient.createLoginActivityIntent(
                activity,
                AuthorizationRequest.Builder(CLIENT_ID,
                    AuthorizationResponse.Type.CODE,
                    REDIRECT_URI)
                    .setScopes(
                        arrayOf(
//                        "ugc-image-upload",
//                        "user-read-playback-state",
//                        "user-modify-playback-state",
                            "user-read-currently-playing",
                            "app-remote-control",
//                        "playlist-read-private",
//                        "playlist-read-collaborative",
//                        "playlist-modify-private",
//                        "playlist-modify-public",
//                        "user-follow-modify",
                            "user-follow-read",
                            "user-read-playback-position",
//                        "user-top-read",
//                        "user-read-recently-played",
//                        "user-library-modify",
                            "user-library-read",
//                        "user-read-email",
//                        "user-read-private"
                        )
                    )
                    .setCustomParam("code_challenge_method", "S256")
                    .setCustomParam("code_challenge", getCodeChallenge(CODE_VERIFIER))
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
                // Handle the Error

                else -> {}
                // Probably interruption
            }
        }

        fun getLoginActivityTokenIntent(code: String): Intent =
            AuthorizationClient.createLoginActivityIntent(
                activity,
                AuthorizationRequest.Builder(CLIENT_ID,
                    AuthorizationResponse.Type.TOKEN,
                    REDIRECT_URI)
                    .setCustomParam("grant_type", "authorization_code")
                    .setCustomParam("code", code)
                    .setCustomParam("code_verifier", CODE_VERIFIER)
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
                    UserManager.userSpotifyAuthToken = authorizationResponse.accessToken
                    viewModel.doneRequestSpotifyAuthToken()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Timber.d("showLoginActivityToken : AuthorizationResponse.Type.ERROR")
                }
                // Handle Error
                else -> {}
                // Probably interruption
            }
        }

}