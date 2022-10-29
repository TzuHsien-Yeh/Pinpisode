package com.tzuhsien.immediat.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.immediat.data.Repository
import com.tzuhsien.immediat.takenote.TakeNoteViewModel

@Suppress("UNCHECKED_CAST")
class YoutubeVideoViewModelFactory(
//    private val repository: Repository,
    private val videoId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(TakeNoteViewModel::class.java) ->
                    TakeNoteViewModel(videoId)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
