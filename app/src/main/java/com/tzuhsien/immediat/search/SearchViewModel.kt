package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.Repository
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

//    lateinit var ytInfoNote: Note

    var userSpotifyAuthToken: String? = null

    var spotifySingleResultId: String? = null
    var ytSingleResultId: String? = null

    private val _ytVideoData = MutableLiveData<YouTubeResult?>(null)
    val ytVideoData: LiveData<YouTubeResult?>
        get() = _ytVideoData

    private val _spotifyEpisodeData = MutableLiveData<EpisodeResult?>(null)
    val spotifyEpisodeData: LiveData<EpisodeResult?>
        get() = _spotifyEpisodeData

    private val _youtubeSearchResult = MutableLiveData<YouTubeSearchResult>(null)
    val youTubeSearchResult: LiveData<YouTubeSearchResult>
        get() = _youtubeSearchResult

    private val _searchResultList = MutableLiveData<List<ItemX>>()
    val searchResultList: LiveData<List<ItemX>>
        get() = _searchResultList

    private val _ytTrendingList = MutableLiveData<List<Item>>()
    val ytTrendingList: LiveData<List<Item>>
        get() = _ytTrendingList


    val uiState = SearchUiState(
        onItemClick = { item ->
            navigateToYoutubeNote(item.id.videoId)
        },
        onTrendingVideoClick = { item ->
            navigateToYoutubeNote(item.id)
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
        } else if (query.contains(spotifyShareLink) || query.contains(spotifyUri)){
            // Spotify

            Timber.d("Spotify link")
            val sourceId = query.extractSpotifySourceId()
            if (sourceId.isNotEmpty()) {
                if (sourceId.contains("episode:")) {
                    getEpisodeInfoById(sourceId.substringAfter("episode:"))
                } else if (sourceId.contains("track:")) {
                    getTrackInfoById(sourceId.substringAfter("track"))
                }
            }
        } else {
            // TODO: search on spotify
            //  use coroutine { two child coroutine } wait for both to responses to continue

            searchOnYouTube(query)
        }

    }

    private fun getEpisodeInfoById(id: String) {

        if (null != userSpotifyAuthToken) {
            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                val result = repository.getSpotifyEpisodeInfo(id, userSpotifyAuthToken!!)

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

        _searchResultList.value = list
    }

    fun navigateToYoutubeNote(videoId: String) {
        _navigateToYoutubeNote.value = videoId
    }

    fun navigateToSpotifyNote(spotifyId: String) {
        _navigateToSpotifyNote.value = spotifyId
    }

    fun doneNavigateToTakeNote() {
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

}

data class SearchUiState(
    val onItemClick: (ItemX) -> Unit,
    val onTrendingVideoClick: (Item) -> Unit
)