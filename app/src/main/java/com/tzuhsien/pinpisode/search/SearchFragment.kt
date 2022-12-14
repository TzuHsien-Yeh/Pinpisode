package com.tzuhsien.pinpisode.search

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
import com.google.android.material.tabs.TabLayoutMediator
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.tzuhsien.pinpisode.Constants
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.databinding.FragmentSearchBinding
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.glide
import com.tzuhsien.pinpisode.ext.utcToLocalTime
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.search.result.SearchResultFragment.Companion.KEY_QUERY
import com.tzuhsien.pinpisode.search.result.SearchResultFragment.Companion.REQUEST_KEYWORD_1
import com.tzuhsien.pinpisode.search.result.SearchResultFragment.Companion.REQUEST_KEYWORD_2
import com.tzuhsien.pinpisode.search.result.ViewPagerAdapter
import com.tzuhsien.pinpisode.util.Util
import timber.log.Timber

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel by viewModels<SearchViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSearchBinding.inflate(layoutInflater)

        binding.searchviewSearch.setOnQueryTextListener(
            object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
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

        viewModel.showMsg.observe(viewLifecycleOwner) {
            if (null != it) {
                binding.textResourceNotFound.text = it
                binding.textResourceNotFound.visibility = View.VISIBLE
                hideRecommendationViews()
                Timber.d("not available")
            } else {
                binding.textResourceNotFound.visibility = View.GONE
            }
        }


        /**
         * Search result of url (specified sourceId)
         * */
        viewModel.ytVideoData.observe(viewLifecycleOwner) {
            it?.let {
                if (it.items.isNotEmpty()) {

                    val item = it.items[0]

                    binding.textSearchResult.visibility = View.VISIBLE
                    binding.cardSingleVideoResult.visibility = View.VISIBLE
                    binding.textTitle.text = item.snippet.title
                    binding.imgThumbnail.glide(item.snippet.thumbnails.high.url)
                    binding.textChannelName.text = item.snippet.channelTitle
                    binding.textPublishedTime.text = item.snippet.publishedAt.utcToLocalTime()
                    viewModel.ytSingleResultId = item.id // video sourceId

                    // Hide other views
                    hideRecommendationViews()
                    binding.cardSingleSpotifyResult.visibility = View.GONE
                    binding.tabLayoutSearchResults.visibility = View.GONE
                } else {
                    binding.cardSingleVideoResult.visibility = View.GONE
                }
            }
        }
        binding.cardSingleVideoResult.setOnClickListener {
            viewModel.ytSingleResultId?.let { viewModel.navigateToYoutubeNote(it) }
        }

        // Spotify episode url search
        viewModel.spotifyEpisodeData.observe(viewLifecycleOwner) {
            it?.let {
                binding.textSearchResult.visibility = View.VISIBLE
                binding.cardSingleSpotifyResult.visibility = View.VISIBLE
                binding.textSpotifySourceTitle.text = it.name
                binding.textSpotifyShow.text = it.show?.name
                binding.textSpotifyPublisher.text = it.show?.publisher
                binding.imgSpotifySource.glide(it.images[0].url)
                viewModel.spotifySingleResultId = it.uri.extractSpotifySourceId()

                // Hide other views
                hideRecommendationViews()
                binding.cardSingleVideoResult.visibility = View.GONE
                binding.tabLayoutSearchResults.visibility = View.GONE
            }
        }

        binding.cardSingleSpotifyResult.setOnClickListener {
            viewModel.spotifySingleResultId?.let { viewModel.navigateToSpotifyNote(it) }
        }

        /**
         * Search results of key words
         * */
        // Display search result
        setUpViewPager()
        setUpTabLayout()
        viewModel.searchQuery.observe(viewLifecycleOwner) {
            Timber.d("query = $it, parentFragmentManager.setFragmentResult")
            requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEYWORD_1,
                bundleOf(KEY_QUERY to it))
            requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEYWORD_2,
                bundleOf(KEY_QUERY to it))

            if (null != it) {
                hideRecommendationViews()
                binding.cardSingleVideoResult.visibility = View.GONE
                binding.cardSingleSpotifyResult.visibility = View.GONE
            }

            binding.tabLayoutSearchResults.visibility = if (null != it) View.VISIBLE else View.GONE
        }

        /**
         * Recommendations
         * */
        val ytTrendingAdapter = YtTrendingAdapter(viewModel.uiState)
        binding.recyclerviewYtTrending.adapter = ytTrendingAdapter
        viewModel.ytTrendingList.observe(viewLifecycleOwner) {
            ytTrendingAdapter.submitList(it)
        }

        viewModel.showSpotifyAuthView.observe(viewLifecycleOwner) {
            binding.viewGroupSpNotAuthorized.visibility =
                if (it) View.VISIBLE else if (viewModel.getSpotifyAuthToken().isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // Check if Spotify auth token available and if the user want to auth
        viewModel.isAuthRequired.observe(viewLifecycleOwner) {
            when (it) {
                null -> {
                    if (!viewModel.getSpotifyAuthToken().isNullOrEmpty()) {
                        viewModel.getSpotifySavedShowLatestEpisodes()
                        binding.viewGroupSpNotAuthorized.visibility = View.GONE
                    }
                }
                true -> {
                    showLoginActivityCode.launch(getLoginActivityCodeIntent())
                    viewModel.doneRequestSpotifyAuthToken()
                }
                false -> {
                    binding.viewGroupSpNotAuthorized.visibility = View.GONE
                    viewModel.getSpotifySavedShowLatestEpisodes()
                }
            }
        }

        binding.btnSpotifyAuth.setOnClickListener {
            viewModel.requestSpotifyAuthToken()
        }

        // The latest episode of user's saved show on Spotify
        val spLatestContentAdapter = SpContentAdapter(viewModel.uiState)
        binding.recyclerviewSpLatestContent.adapter = spLatestContentAdapter
        viewModel.spotifyLatestEpisodesList.observe(viewLifecycleOwner) {
            Timber.d("viewModel.spotifyLatestEpisodesList.observe: $it")
            spLatestContentAdapter.submitList(it)
        }
        viewModel.spotifyMsg.observe(viewLifecycleOwner) {
            it?.let {
                binding.textSpotifyMessage.text = it
            }
        }

        /**
         *  Navigation
         * */
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

    private fun hideRecommendationViews() {
        binding.icYoutube.visibility = View.GONE
        binding.textTrendingOnYoutube.visibility = View.GONE
        binding.recyclerviewYtTrending.visibility = View.GONE

        binding.icSpotify.visibility = View.GONE
        binding.textNewOnSpotify.visibility = View.GONE
        binding.viewGroupSpNotAuthorized.visibility = View.GONE
        binding.recyclerviewSpLatestContent.visibility = View.GONE
        binding.textSpotifyMessage.visibility = View.GONE
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
    private fun getLoginActivityCodeIntent(): Intent =
        AuthorizationClient.createLoginActivityIntent(
            activity,
            AuthorizationRequest.Builder(Constants.CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                Constants.REDIRECT_URI)
                .setScopes(
                    arrayOf(
                        Constants.SCOPE_READ_PLAYBACK_POSITION,
                        Constants.SCOPE_LIBRARY_READ,
                    )
                )
                .setCustomParam(Constants.PARAM_CODE_CHALLENGE_METHOD, Constants.S256)
                .setCustomParam(Constants.PARAM_CODE_CHALLENGE,
                    Util.getCodeChallenge(Util.CODE_VERIFIER))
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
            AuthorizationRequest.Builder(Constants.CLIENT_ID, AuthorizationResponse.Type.TOKEN,
                Constants.REDIRECT_URI)
                .setScopes(
                    arrayOf(
                        Constants.SCOPE_READ_PLAYBACK_POSITION,
                        Constants.SCOPE_LIBRARY_READ,
                    )
                )
                .setCustomParam(Constants.PARAM_GRANT_TYPE, Constants.AUTH_CODE)
                .setCustomParam(Constants.PARAM_CODE, code)
                .setCustomParam(Constants.PARAM_CODE_VERIFIER, Util.CODE_VERIFIER)
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
            }
            AuthorizationResponse.Type.ERROR -> {
                Timber.d("showLoginActivityToken : AuthorizationResponse.Type.ERROR")
            }
            else -> {} // Probably interruption
        }
    }
}