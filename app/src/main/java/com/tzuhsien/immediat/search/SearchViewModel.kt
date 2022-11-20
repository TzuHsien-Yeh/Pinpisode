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

    var userSpotifyAuthToken: String? = null

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
    private val _searchQuery = MutableLiveData<String>(null)
    val searchQuery: LiveData<String>
        get() = _searchQuery

    private val _youtubeSearchResult = MutableLiveData<YouTubeSearchResult>(null)
    val youTubeSearchResult: LiveData<YouTubeSearchResult>
        get() = _youtubeSearchResult

    private val _ytSearchResultList = MutableLiveData<List<ItemX>>()
    val ytSearchResultList: LiveData<List<ItemX>>
        get() = _ytSearchResultList

    private val _spotifySearchResult = MutableLiveData<SpotifySearchResult>(null)
    val spSearchResult: LiveData<SpotifySearchResult>
         get() = _spotifySearchResult



    private val _ytTrendingList = MutableLiveData<List<Item>>()
    val ytTrendingList: LiveData<List<Item>>
        get() = _ytTrendingList

    private val _spotifyLatestEpisodesList = MutableLiveData(listOf(SpotifyItem(), SpotifyItem()))
    val spotifyLatestEpisodesList: LiveData<List<SpotifyItem>>
        get() = _spotifyLatestEpisodesList


    private val _isAuthRequired = MutableLiveData(false)
    val isAuthRequired: LiveData<Boolean>
        get() = _isAuthRequired

    val uiState = SearchUiState(
        onYoutubeItemClick = { item ->
            navigateToYoutubeNote(item.id.videoId)
        },
        onTrendingVideoClick = { item ->
            navigateToYoutubeNote(item.id)
        },
        onSpotifyLatestContentClick = { item ->
            navigateToSpotifyNote(item.id)
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
        if (UserManager.userSpotifyAuthToken.isNotEmpty()) {
            getSpotifySavedShowLatestEpisodes()
        }
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
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
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

    fun getSpotifySavedShowLatestEpisodes() {

        Timber.d("getSpotifySavedShowLatestEpisodes")
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getUserSavedShows(UserManager.userSpotifyAuthToken)

            when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    getShowEpisodesById(result.data.items)
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
                        list.add(episodeResult.data.items[0])
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
        Timber.d("_ytTrendingList.value = $list")
    }

    fun findMediaSource(query: String) {

        Timber.d("findMediaSource")

        // Check if the query is a YouTube url
        if (query.contains(youtubeWatchUrl) || query.contains(youtubeShareLink)) {
            val videoId = query.extractYoutubeVideoId()
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
            }
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
        } else {
            _searchQuery.value = query
//            // search on spotify use coroutine { two child coroutine } wait for both to responses to continue
//            if (UserManager.userSpotifyAuthToken.isNotEmpty()) {
//                searchOnSpotify(query)
//            } else {
//                _isAuthRequired.value = true
//            }
//            searchOnYouTube(query)
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
        } else {

            // TODO: PROMPT TO AUTH
        }

    }

    private fun getTrackInfoById(id: String) {

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

    private fun searchOnYouTube(query: String) {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.searchOnYouTube(query)

            _youtubeSearchResult.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("youtubeSearchResults: ${result.data}")

                    putYtResultToItemList(result.data)
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
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

    private fun putYtResultToItemList(ytResult: YouTubeSearchResult) {
        val list = mutableListOf<ItemX>()
        for (item in ytResult.items) {
            list.add(item)
        }

        _ytSearchResultList.value = list
    }

    private fun searchOnSpotify(query: String) {

        Timber.d("searchOnSpotify")
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.searchOnSpotify(query, authToken = UserManager.userSpotifyAuthToken)

            _spotifySearchResult.value =
                when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("spotifySearchResults: ${result.data}")

//                    putResultToItemList(result.data)
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
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

}

data class SearchUiState(
    val onYoutubeItemClick: (ItemX) -> Unit,
    val onTrendingVideoClick: (Item) -> Unit,
    val onSpotifyLatestContentClick: (SpotifyItem) -> Unit,
)