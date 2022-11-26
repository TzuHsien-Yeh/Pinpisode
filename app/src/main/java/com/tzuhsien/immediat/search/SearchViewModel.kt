package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.ext.extractSpotifySourceId
import com.tzuhsien.immediat.ext.extractYoutubeVideoId
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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


    private val _ytTrendingList = MutableLiveData<List<Item>>()
    val ytTrendingList: LiveData<List<Item>>
        get() = _ytTrendingList

    private val _spotifyLatestEpisodesList = MutableLiveData<List<SpotifyItem>>()
    val spotifyLatestEpisodesList: LiveData<List<SpotifyItem>>
        get() = _spotifyLatestEpisodesList

    private val _spotifyMsg = MutableLiveData<String>(null)
    val spotifyMsg: LiveData<String>
        get() = _spotifyMsg

    private val _isAuthRequired = MutableLiveData<Boolean?>(null)
    val isAuthRequired: LiveData<Boolean?>
        get() = _isAuthRequired

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

    // status: The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    // error: The internal MutableLiveData that stores the error of the most recent request
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

    private val youtubeWatchUrl = "youtube.com/watch?v="
    private val youtubeShareLink = "youtu.be/"

    private val spotifyShareLink = "https://open.spotify.com/"
    private val spotifyUri = "spotify:"

    init {
        getYoutubeTrendingVideos()
    }

    private fun getYoutubeTrendingVideos() {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getTrendingVideosOnYouTube()

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
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    fun getSpotifySavedShowLatestEpisodes() {
        Timber.d("getSpotifySavedShowLatestEpisodes")
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getUserSavedShows(UserManager.userSpotifyAuthToken)

            when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    if (result.data.items.isEmpty()) {
                        _spotifyMsg.value = "You haven't saved any show yet"
                    } else {
                        getShowEpisodesById(result.data.items)
                    }
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    Timber.d("getSpotifySavedShowLatestEpisodes is Result.Error: ${result.exception.toString()}")
                }
                else -> {
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    private fun getShowEpisodesById(showItems: List<ShowItem>) {
        Timber.d("getShowEpisodesById $showItems ")
        coroutineScope.launch {

            val list = mutableListOf<SpotifyItem>()

            for (item in showItems) {
                val episodeResult = repository.getShowEpisodes(
                    showId = item.show.id,
                    authToken = UserManager.userSpotifyAuthToken
                )

                when (episodeResult) {
                    is Result.Success -> {
                        val latestEpisode = episodeResult.data.items[0]
                        latestEpisode.show = item.show
                        list.add(latestEpisode)
                    }
                    is Result.Fail -> {
                        _error.value = episodeResult.error
                        _status.value = LoadApiStatus.ERROR
                    }
                    is Result.Error -> {
                        _error.value = episodeResult.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                    }
                    else -> {
                        _error.value = Util.getString(R.string.unknown_error)
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
        if (query.contains(youtubeWatchUrl) || query.contains(youtubeShareLink)) {

            val videoId = query.extractYoutubeVideoId()
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
            }
            _searchQuery.value = null

        } else if (query.contains(spotifyShareLink) || query.contains(spotifyUri)) {
            // If the query is a  Spotify link, request auth token to proceed to search
            _isAuthRequired.value = UserManager.userSpotifyAuthToken.isEmpty()

            val sourceId = query.extractSpotifySourceId()
            if (sourceId.isNotEmpty()) {
                if (sourceId.contains("episode:")) {
                    getEpisodeInfoById(sourceId.substringAfter("episode:"))
                } else if (sourceId.contains("track:")) {
                    getTrackInfoById(sourceId.substringAfter("track"))
                }
            }

            _searchQuery.value = null

        } else {
            _searchQuery.value = query
        }
    }

    private fun getEpisodeInfoById(id: String) {

        if (UserManager.userSpotifyAuthToken.isNotEmpty()) {
            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                val result =
                    repository.getSpotifyEpisodeInfo(id, UserManager.userSpotifyAuthToken)

                _spotifyEpisodeData.value = when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        result.data
                    }
                    is Result.Fail -> {
                        _error.value = result.error
                        _status.value = LoadApiStatus.ERROR

                        // Show msg if source not found (result list is empty)
                        _showMsg.value = result.error
                        null
                    }
                    is Result.Error -> {
                        _error.value = result.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                    else -> {
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                }
            }
        }
    }

    private fun getTrackInfoById(id: String) {
        // TODO: get track info
    }

    private fun getYoutubeVideoInfoById(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getYouTubeVideoInfoById(videoId)

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
                    _error.value = Util.getString(R.string.unknown_error)
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

    fun doneRequestSpotifyAuthToken() {
        _isAuthRequired.value = false
    }

    fun requestSpotifyAuthToken() {
        _isAuthRequired.value = true
    }

}

data class SearchUiState(
    val onYoutubeItemClick: (ItemX) -> Unit,
    val onTrendingVideoClick: (Item) -> Unit,
    val onSpotifyLatestContentClick: (SpotifyItem) -> Unit,
)