package com.tzuhsien.immediat.takenote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.data.model.VideoNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber

class TakeNoteViewModel(val videoId: String) : ViewModel() {

    private val _timeNoteList = MutableLiveData<List<VideoNote>>()
    val timeNoteList: LiveData<List<VideoNote>>
        get() = _timeNoteList

    val testTime = 10.3

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        Timber.d("videoId Passed in TakeNote: $videoId")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun takeTimeStamp(second: Float) {
        val sec = second.toLong()

        Timber.d("timestamp: $sec")
        //TODO: add time data to the note of the videoId

        coroutineScope
    }
}