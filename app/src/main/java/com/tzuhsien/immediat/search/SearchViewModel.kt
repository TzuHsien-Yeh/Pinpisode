package com.tzuhsien.immediat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.ImMediAtApplication
import com.tzuhsien.immediat.network.YouTubeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    fun getYouTubeVideoInfoById(query: String) {
        val youtubeWatchUrl = "youtube.com/watch?v="
        val youtubeShareLink = "youtu.be/"
        var videoId = ""
        val testId = "H9aDOOU8gcQ"

        if (youtubeWatchUrl in query) {
            videoId = query
                .substringAfter(youtubeWatchUrl)
                .substringBefore("&", query.substringAfter(youtubeWatchUrl))
        } else {
            if (youtubeShareLink in query) {
                videoId = query.substringAfter("youtu.be/")
            } else {
                // TODO: call YT search api
            }
        }
        if (!videoId.isNullOrEmpty()) {
            coroutineScope.launch {
                val result = YouTubeApi.retrofitService.getVideoInfo(
                    id = videoId,
                    part = ImMediAtApplication.YOUTUBE_PARAM_PART
                )

                if (result.pageInfo.totalResults == 0) {
                    _toastMsg.value = "Invalid link: Video not found"
                }
            }
        }

    }

    fun showToastCompleted() {
        _toastMsg.value = null
    }
}