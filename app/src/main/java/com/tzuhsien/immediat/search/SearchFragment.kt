package com.tzuhsien.immediat.search

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.databinding.FragmentSearchBinding
import com.tzuhsien.immediat.ext.extractSpotifySourceId
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.utcToLocalTime
import com.tzuhsien.immediat.search.result.ViewPagerAdapter
import com.tzuhsien.immediat.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel by viewModels<SearchViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSearchBinding.inflate(layoutInflater)

        binding.searchviewSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Timber.d("onQueryTextSubmit: QUERY = $query")
                    query?.let {
                        viewModel.findMediaSource(query)
                    }
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    viewModel.resetMsg()
                    return false
                }
            }
        )


        viewModel.showMsg.observe(viewLifecycleOwner, Observer {
            if (null != it) {
                binding.textResourceNotFound.text = it
                binding.textResourceNotFound.visibility = View.VISIBLE

                Timber.d("not available")
            } else {
                binding.textResourceNotFound.visibility = View.GONE
            }
        })

        binding.cardSingleVideoResult.visibility = View.GONE
        binding.textSearchResult.visibility = View.GONE

        /**
         * Search result of url (specified sourceId)
         * */
        viewModel.ytVideoData.observe(viewLifecycleOwner, Observer {
           it?.let {
               if (it.items.isNotEmpty()) {

                   val item = it.items[0]

                   binding.textSearchResult.visibility = View.VISIBLE
                   binding.cardSingleVideoResult.visibility = View.VISIBLE
                   binding.textTitle.text = item.snippet.title
                   Glide.with(this)
                       .load(item.snippet.thumbnails.high.url)
                       .into(binding.imgThumbnail)
                   binding.textChannelName.text = item.snippet.channelTitle
                   binding.textPublishedTime.text = item.snippet.publishedAt.utcToLocalTime()
                   viewModel.ytSingleResultId = item.id // including video sourceId

                   binding.icYoutube.visibility = View.GONE
                   binding.textTrendingOnYoutube.visibility = View.GONE
                   binding.recyclerviewYtTrending.visibility = View.GONE
               } else {
                   binding.cardSingleVideoResult.visibility = View.GONE
               }
           }
        })
        binding.cardSingleVideoResult.setOnClickListener {
            viewModel.navigateToYoutubeNote(viewModel.ytSingleResultId!!)
        }

        // Spotify episode url search
        viewModel.spotifyEpisodeData.observe(viewLifecycleOwner) {
            it?.let {
                binding.textSearchResult.visibility = View.VISIBLE
                binding.cardSingleSpotifyResult.visibility = View.VISIBLE
                binding.textSpotifySourceTitle.text = it.name
                binding.textSpotifyShow.text = it.show?.name
                binding.textSpotifyPublisher.text = it.show?.publisher
                Glide.with(binding.imgSpotifySource)
                    .load(it.images[0].url)
                    .into(binding.imgSpotifySource)

                viewModel.spotifySingleResultId = it.uri.extractSpotifySourceId()

                binding.icYoutube.visibility = View.GONE
                binding.textTrendingOnYoutube.visibility = View.GONE
                binding.recyclerviewYtTrending.visibility = View.GONE
                binding.textNewOnSpotify.visibility = View.GONE
                binding.recyclerviewSpLatestContent.visibility = View.GONE
                }
        }

        binding.cardSingleSpotifyResult.setOnClickListener {
            viewModel.navigateToSpotifyNote(viewModel.spotifySingleResultId!!)
        }

        /**
         * Search results of key word
         * */
        // Display search result
        setUpViewPager()
        setUpTabLayout()
        viewModel.searchQuery.observe(viewLifecycleOwner) {
            it?.let {
                Timber.d("query = $it, parentFragmentManager.setFragmentResult")
                requireActivity().supportFragmentManager.setFragmentResult("requestKey", bundleOf("query" to it))
            }

            binding.tabLayoutSearchResults.visibility = if (null != it) View.VISIBLE else View.GONE

            binding.icYoutube.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.textTrendingOnYoutube.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.recyclerviewYtTrending.visibility = if (null == it) View.VISIBLE else View.GONE

            binding.icSpotify.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.textNewOnSpotify.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.recyclerviewSpLatestContent.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.imgViewCoverSpotifyContent.visibility = if (null == it) View.VISIBLE else View.GONE
            binding.btnSpotifyAuth.visibility = if (null == it) View.VISIBLE else View.GONE
        }


        /**
         * Recommendations
         * */
        val ytTrendingAdapter = YtTrendingAdapter(viewModel.uiState)
        binding.recyclerviewYtTrending.adapter = ytTrendingAdapter
        viewModel.ytTrendingList.observe(viewLifecycleOwner) {
            ytTrendingAdapter.submitList(it)

            Timber.d("_ytTrendingList.value = $it")
        }

        // Cover the content if not authorized
        viewModel.isAuthRequired.observe(viewLifecycleOwner) {
            if (it) {
                showLoginActivityCode.launch(getLoginActivityCodeIntent())
                viewModel.doneRequestSpotifyAuthToken()
            }
        }

        binding.btnSpotifyAuth.setOnClickListener {
            showLoginActivityCode.launch(getLoginActivityCodeIntent())
            viewModel.doneRequestSpotifyAuthToken()
            viewModel.getSpotifySavedShowLatestEpisodes()
        }

        val spLatestContentAdapter = SpContentAdapter(viewModel.uiState)
        binding.recyclerviewSpLatestContent.adapter = spLatestContentAdapter
        viewModel.spotifyLatestEpisodesList.observe(viewLifecycleOwner) {
            Timber.d("viewModel.spotifyLatestEpisodesList.observe: $it")
            spLatestContentAdapter.submitList(it)

            binding.btnSpotifyAuth.visibility = if (it[0].id.isEmpty()) View.VISIBLE else View.GONE
            binding.imgViewCoverSpotifyContent.visibility = if (it[0].id.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerviewSpLatestContent.alpha = if (it[0].id.isEmpty()) 0.5F else 1F
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(
                    YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(
                        videoIdKey = it
                    )
                )
                viewModel.doneNavigation()
            }
        })

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

    private fun setUpTabLayout() {
        TabLayoutMediator(
            binding.tabLayoutSearchResults,
            binding.viewPagerSearchResult
        ) { tab, position ->
            when (position) {
                0 -> tab.text = Source.YOUTUBE.source
                1 -> tab.text = Source.SPOTIFY.source
            }
        }.attach()
    }

    private fun setUpViewPager() {
        val adapter = ViewPagerAdapter(this, 2)
        binding.viewPagerSearchResult.adapter = adapter
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
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.CODE, REDIRECT_URI)
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

        val authorizationResponse = AuthorizationClient.getResponse(result.resultCode, result.data)

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
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setCustomParam("grant_type", "authorization_code")
                .setCustomParam("code", code)
                .setCustomParam("code_verifier", CODE_VERIFIER)
                .build()
        )

    private val showLoginActivityToken = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        val authorizationResponse = AuthorizationClient.getResponse(result.resultCode, result.data)

        when (authorizationResponse.type) {
            AuthorizationResponse.Type.TOKEN -> {
                // Here You can get access to the authorization token
                // with authorizationResponse.token

                Timber.d("showLoginActivityToken authorizationResponse.expiresIn: ${authorizationResponse.expiresIn}")
                Timber.d("authorizationResponse.accessToken = ${authorizationResponse.accessToken}")
                UserManager.userSpotifyAuthToken = authorizationResponse.accessToken
                viewModel.getSpotifySavedShowLatestEpisodes()
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