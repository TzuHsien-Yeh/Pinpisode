package com.tzuhsien.immediat.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.youtubenote.YouTubeNoteViewModel

@Suppress("UNCHECKED_CAST")
class YoutubeNoteViewModelFactory(
    private val repository: Repository,
    private val noteId: String,
    private val videoId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(YouTubeNoteViewModel::class.java) ->
                    YouTubeNoteViewModel(repository, noteId, videoId)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
