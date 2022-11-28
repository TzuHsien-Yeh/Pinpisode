package com.tzuhsien.pinpisode.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.pinpisode.coauthor.CoauthorViewModel
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.tag.TagViewModel

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

                isAssignableFrom(CoauthorViewModel::class.java) ->
                    CoauthorViewModel(repository, note)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
