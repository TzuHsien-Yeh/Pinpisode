package com.tzuhsien.immediat.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.tag.TagViewModel
import com.tzuhsien.immediat.youtubenote.YouTubeNoteViewModel

@Suppress("UNCHECKED_CAST")
class NoteViewModelFactory(
    private val repository: Repository,
    private val note: Note
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(TagViewModel::class.java) ->
                    TagViewModel(repository = repository, note = note)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
