package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Item
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.ext.extractYoutubeVideoId
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber


class SearchViewModel(private val repository: Repository) : ViewModel() {

    lateinit var ytInfoNote: Note

    private val _ytVideoData = MutableLiveData<YouTubeResult?>(null)
    val ytVideoData: LiveData<YouTubeResult?>
        get() = _ytVideoData

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

    val youtubeWatchUrl = "youtube.com/watch?v="
    val youtubeShareLink = "youtu.be/"

    fun findMediaSource(query: String) {

        // Check if the query is a YouTube url
        if (query.contains(youtubeWatchUrl) || query.contains(youtubeShareLink)) {
            val videoId = query.extractYoutubeVideoId()
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
                // TODO: else -> prepare to call YT search api
            }
        } else {

            // TODO: if (query is a spotify url) { get spotify resource id } else

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

    fun setYoutubeNoteData(videoItem: Item) {
        ytInfoNote = Note(
            sourceId = videoItem.id,
            source = Source.YOUTUBE.source,
            ownerId = UserManager.userId,
            authors = listOf(UserManager.userId),
            tags = listOf(Source.YOUTUBE.source),
            thumbnail = videoItem.snippet.thumbnails.high.url,
            title = videoItem.snippet.title,
            duration = videoItem.contentDetails.duration
        )
    }

    fun navigateToYoutubeNote() {
        _navigateToYoutubeNote.value = ytInfoNote.sourceId
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