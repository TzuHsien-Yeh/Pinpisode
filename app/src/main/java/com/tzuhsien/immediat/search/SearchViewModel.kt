package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.ItemX
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.model.YouTubeSearchResult
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

    var ytSingleResultId: String? = null

    private val _ytVideoData = MutableLiveData<YouTubeResult?>(null)
    val ytVideoData: LiveData<YouTubeResult?>
        get() = _ytVideoData

    private val _youtubeSearchResult = MutableLiveData<YouTubeSearchResult>(null)
    val youTubeSearchResult: LiveData<YouTubeSearchResult>
        get() = _youtubeSearchResult

    private val _searchResultList = MutableLiveData<List<ItemX>>()
    val searchResultList: LiveData<List<ItemX>>
        get() = _searchResultList

    val uiState = SearchUiState(
        onItemClick = { item ->
            navigateToYoutubeNote(item.id.videoId)
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

    private val youtubeWatchUrl = "youtube.com/watch?v="
    private val youtubeShareLink = "youtu.be/"

    private val spotifyShareLink = "https://open.spotify.com/"
    private val spotifyUri = "spotify:"

    fun findMediaSource(query: String) {

        // Check if the query is a YouTube url
        if (query.contains(youtubeWatchUrl) || query.contains(youtubeShareLink)) {
            val videoId = query.extractYoutubeVideoId()
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
            }
        } else if (query.contains(spotifyShareLink) || query.contains(spotifyUri)){
            val sourceId = query.extractSpotifySourceId()
            if (sourceId.isNotEmpty()) {
                // TODO:  get spotify info by source id + endpoint type?
            }
        } else {
            // TODO: search on spotify
            //  use coroutine { two child coroutine } wait for both to responses to continue

            searchOnYouTube(query)
        }

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

//    fun setYoutubeNoteData(videoItem: Item) {
//        ytInfoNote = Note(
//            sourceId = videoItem.id,
//            source = Source.YOUTUBE.source,
//            ownerId = UserManager.userId!!,
//            authors = listOf(UserManager.userId!!),
//            tags = listOf(Source.YOUTUBE.source),
//            thumbnail = videoItem.snippet.thumbnails.high.url,
//            title = videoItem.snippet.title,
//            duration = videoItem.contentDetails.duration
//        )
//    }

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

    fun doneNavigateToTakeNote() {
        _navigateToYoutubeNote.value = null
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
    val onItemClick: (ItemX) -> Unit
)