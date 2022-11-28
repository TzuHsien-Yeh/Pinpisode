package com.tzuhsien.pinpisode.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.spotifynote.SpotifyNoteViewModel
import com.tzuhsien.pinpisode.youtubenote.YouTubeNoteViewModel

@Suppress("UNCHECKED_CAST")
class NoteSourceViewModelFactory(
    private val repository: Repository,
    private val noteId: String?,
    private val sourceId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(YouTubeNoteViewModel::class.java) ->
                    YouTubeNoteViewModel(repository, noteId, sourceId)

                isAssignableFrom(SpotifyNoteViewModel::class.java) ->
                    SpotifyNoteViewModel(repository, noteId, sourceId)

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
