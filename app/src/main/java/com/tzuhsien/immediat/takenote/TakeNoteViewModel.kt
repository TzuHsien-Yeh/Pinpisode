package com.tzuhsien.immediat.takenote

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber

class TakeNoteViewModel(val videoId: String) : ViewModel() {

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        Timber.d("videoId Passed in TakeNote: $videoId")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}