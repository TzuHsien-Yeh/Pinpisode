package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.ext.addYouTubeNoteData
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.network.YouTubeApi
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchViewModel(private val repository: Repository) : ViewModel() {

    private val _ytVideoData = MutableLiveData<YouTubeResult>()
    val ytVideoData: LiveData<YouTubeResult>
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

    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    private val _navigateToTakeNote = MutableLiveData<String?>()
    val navigateToTakeNote: LiveData<String?>
        get() = _navigateToTakeNote


    val youtubeWatchUrl = "youtube.com/watch?v="
    val youtubeShareLink = "youtu.be/"

    fun findMediaSource(query: String) {

        // Check if the query is a YouTube url
        if (query.contains(youtubeWatchUrl) || query.contains(youtubeShareLink)) {
            val videoId = extractYoutubeVideoId(query)
            if (videoId.isNotEmpty()) {
                getYoutubeVideoInfoById(videoId)
                // TODO: prepare to call YT search api
            }
        } else {

            // TODO: if (query is a spotify url) { get spotify resource id } else

        }

//        if (!videoId.isNullOrEmpty()) {
//
//            coroutineScope.launch {
//                val result = YouTubeApi.retrofitService.getVideoInfo(
//
//                )
//
//                if (result.pageInfo.totalResults == 0) {
//                    _toastMsg.value = "Invalid link: Video not found"
//                } else
//                    addYouTubeNoteData(result)
//                _navigateToTakeNote.value = result.items[0].id
//            }
//        }

    }

    private fun getYoutubeVideoInfoById(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getYouTubeVideoInfoById(videoId)

            _ytVideoData.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("post returned data: ${result.data}")
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

    private fun extractYoutubeVideoId(query: String): String {
        return if (youtubeWatchUrl in query) {
            query
                .substringAfter(youtubeWatchUrl)
                .substringBefore("&", query.substringAfter(youtubeWatchUrl))
        } else {
            query.substringAfter(youtubeShareLink)
        }
    }

    fun showToastCompleted() {
        _toastMsg.value = null
    }

    fun doneNavigateToTakeNote() {
        _navigateToTakeNote.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}