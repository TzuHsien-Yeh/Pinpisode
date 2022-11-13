package com.tzuhsien.immediat.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.spotifynote.SpotifyNoteViewModel
import com.tzuhsien.immediat.youtubenote.YouTubeNoteViewModel

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
