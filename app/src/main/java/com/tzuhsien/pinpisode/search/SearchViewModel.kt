package com.tzuhsien.pinpisode.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.Constants
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.extractYoutubeVideoId
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util.getString
import kotlinx.coroutines.*
import timber.log.Timber


class SearchViewModel(private val repository: Repository) : ViewModel() {

    // Search by pasting url
    var spotifySingleResultId: String? = null
    var ytSingleResultId: String? = null

    private val _ytVideoData = MutableLiveData<YouTubeResult?>(null)
    val ytVideoData: LiveData<YouTubeResult?>
        get() = _ytVideoData

    private val _spotifyEpisodeData = MutableLiveData<SpotifyItem?>(null)
    val spotifyEpisodeData: LiveData<SpotifyItem?>
        get() = _spotifyEpisodeData

    // Search by keywords
    private val _searchQuery = MutableLiveData<String?>(null)
    val searchQuery: LiveData<String?>
        get() = _searchQuery

    // Recommendations
    private val _ytTrendingList = MutableLiveData<List<Item>>()
    val ytTrendingList: LiveData<List<Item>>
        get() = _ytTrendingList

    private val _spotifyLatestEpisodesList = MutableLiveData<List<SpotifyItem>>()
    val spotifyLatestEpisodesList: LiveData<List<SpotifyItem>>
        get() = _spotifyLatestEpisodesList

    // Deal with Spotify auth issue or no show saved
    private val _spotifyMsg = MutableLiveData<String>(null)
    val spotifyMsg: LiveData<String>
        get() = _spotifyMsg

    private val _isAuthRequired = MutableLiveData<Boolean?>(null)
    val isAuthRequired: LiveData<Boolean?>
        get() = _isAuthRequired

    private val _showSpotifyAuthView = MutableLiveData<Boolean>()
    val showSpotifyAuthView: LiveData<Boolean>
        get() = _showSpotifyAuthView

    val uiState = SearchUiState(
        onYoutubeItemClick = { item ->
            navigateToYoutubeNote(item.id.videoId)
        },
        onTrendingVideoClick = { item ->
            navigateToYoutubeNote(item.id)
        },
        onSpotifyLatestContentClick = { item ->
            navigateToSpotifyNote(item.uri.extractSpotifySourceId())
        }
    )

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _showMsg = MutableLiveData<String?>(null)
    val showMsg: LiveData<String?>
        get() = _showMsg

    private val _navigateToYoutubeNote = MutableLiveData<String?>()
    val navigateToYoutubeNote: LiveData<String?>
        get() = _navigateToYoutubeNote

    private val _navigateToSpotifyNote = MutableLiveData<String?>()
    val navigateToSpotifyNote: LiveData<String?>
        get() = _navigateToSpotifyNote

    init {
        getYoutubeTrendingVideos()
    }

    fun getSpotifyAuthToken(): String? {
        return repository.getSpotifyAuthToken()
    }

    private fun getYoutubeTrendingVideos() {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = withContext(Dispatchers.IO) {
                repository.getTrendingVideosOnYouTube()
            }

            when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    putToItemList(result.data)
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                }
                else -> {
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    fun getSpotifySavedShowLatestEpisodes() {
        Timber.d("getSpotifySavedShowLatestEpisodes")
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            getSpotifyAuthToken()?.let { token ->

                val result = withContext(Dispatchers.IO) {
                    repository.getUserSavedShows(token)
                }

                when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        if (result.data.items.isEmpty()) {
                            _spotifyMsg.value = getString(R.string.sp_msg_have_not_saved_any_show)
                        } else {
                            getShowEpisodesById(result.data.items)
                        }
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        _status.value = LoadApiStatus.ERROR

                        _spotifyMsg.value = result.error
                        Timber.d("getUserSavedShows is Result.Fail [msg]: ${result.error}")
                    }
                    is Result.SpotifyAuthError -> {
                        _status.value = LoadApiStatus.DONE
                        _showSpotifyAuthView.value = true
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        _status.value = LoadApiStatus.ERROR

                        Timber.d("getSpotifySavedShowLatestEpisodes is Result.Error: ${result.exception}")
                    }
                    else -> {
                        _error.value = getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }

    private fun getShowEpisodesById(showItems: List<ShowItem>) {
        Timber.d("getShowEpisodesById $showItems ")
        coroutineScope.launch {

            val list = mutableListOf<SpotifyItem>()

            for (item in showItems) {
                val episodeResult = getSpotifyAuthToken()?.let {
                    withContext(Dispatchers.IO) {
                        repository.getShowEpisodes(
                            showId = item.show.id,
                            authToken = it
                        )
                    }
                }

                when (episodeResult) {
                    is Result.Success -> {
                        val latestEpisode = episodeResult.data.items[0]
                        latestEpisode.show = item.show
                        list.add(latestEpisode)
                    }
                    is Result.Fail -> {
                        _error.value = episodeResult.error
                        _status.value = LoadApiStatus.ERROR
                        _spotifyMsg.value = episodeResult.error
                        Timber.d("getUserSavedShows is Result.Fail [msg]: ${episodeResult.error}")
                    }
                    is Result.SpotifyAuthError -> {
                        _status.value = LoadApiStatus.DONE
                        _showSpotifyAuthView.value = true
                    }
                    is Result.Error -> {
                        _error.value = episodeResult.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                    }
                    else -> {
                        _error.value = getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }

            _spotifyLatestEpisodesList.value = list
        }
    }

    private fun putToItemList(data: YouTubeResult) {
        val list = mutableListOf<Item>()
        for (item in data.items) {
            list.add(item)
        }

        _ytTrendingList.value = list
    }

    fun findMediaSource(query: String) {
        // Check if the query is a YouTube url
        if (query.contains(Constants.YOUTUBE_WATCH_URL) || query.contains(Constants.YOUTUBE_SHARE_LINK)) {

            val videoId = query.extractYoutubeVideoId()
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
            }
            _searchQuery.value = null

        } else if (query.contains(Constants.SPOTIFY_SHARE_LINK) || query.contains(Constants.SPOTIFY_URI)) {
            // If the query is a  Spotify link, request auth token to proceed to search
            _isAuthRequired.value = null == getSpotifyAuthToken()

            val sourceId = query.extractSpotifySourceId()
            if (sourceId.isNotEmpty()) {
                if (sourceId.contains(Constants.SPOTIFY_URI_EPISODE)) {
                    getEpisodeInfoById(sourceId.substringAfter(Constants.SPOTIFY_URI_EPISODE))
                } else {
                    _showMsg.value = getString(R.string.episode_not_found)
                }
            }

            _searchQuery.value = null

        } else {
            _searchQuery.value = query
        }
    }

    private fun getEpisodeInfoById(id: String) {

        getSpotifyAuthToken()?.let { token ->

            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                val result = withContext(Dispatchers.IO) {
                    repository.getSpotifyEpisodeInfo(id, token)
                }

                _spotifyEpisodeData.value = when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        result.data
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        _status.value = LoadApiStatus.ERROR

                        // Show msg if http exception or source not found (result list is empty)
                        _showMsg.value = result.error
                        null
                    }
                    is Result.SpotifyAuthError -> {
                        _status.value = LoadApiStatus.DONE
                        _isAuthRequired.value = result.expired
                        null
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    else -> {
                        _error.value = getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                }
            }
        }
    }

    private fun getYoutubeVideoInfoById(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = withContext(Dispatchers.IO) {
                repository.getYouTubeVideoInfoById(videoId)
            }

            _ytVideoData.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("video data: ${result.data}")

                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR

                    // Show msg if video not found (result list is empty)
                    _showMsg.value = result.error
                    null
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }
    }

    fun navigateToYoutubeNote(videoId: String) {
        _navigateToYoutubeNote.value = videoId
    }

    fun navigateToSpotifyNote(spotifyId: String) {
        _navigateToSpotifyNote.value = spotifyId
    }

    fun doneNavigation() {
        _navigateToYoutubeNote.value = null
        _navigateToSpotifyNote.value = null
    }

    fun resetMsg() {
        _showMsg.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun saveSpotifyAuthToken(authToken: String) {
        repository.setSpotifyAuthToken(authToken)
        _isAuthRequired.value = false
    }

    fun requestSpotifyAuthToken() {
        _isAuthRequired.value = true
    }

    fun doneRequestSpotifyAuthToken() {
        _isAuthRequired.value = false
    }
}

data class SearchUiState(
    val onYoutubeItemClick: (ItemX) -> Unit,
    val onTrendingVideoClick: (Item) -> Unit,
    val onSpotifyLatestContentClick: (SpotifyItem) -> Unit,
)