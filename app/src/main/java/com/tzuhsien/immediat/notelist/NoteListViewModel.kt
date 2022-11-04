package com.tzuhsien.immediat.notelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide.init
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import timber.log.Timber

class NoteListViewModel(private val repository: Repository) : ViewModel() {
    var liveNoteList = MutableLiveData<List<Note>>()

    var set = mutableSetOf<String>()
    var tagSet = MutableLiveData<Set<String>>()

//    private val _selectedTag = MutableLiveData<String>()
//    val selectedTag: LiveData<String>
//        get() = _selectedTag

    var selectedTag: String? = null

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _navigateToYoutubeNote = MutableLiveData<Note>()
    val navigateToYoutubeNote: LiveData<Note>
        get() = _navigateToYoutubeNote

    val uiState = NoteListUiState(
        onTagClick = {
            selectedTag = it
        }
    )

    init {
        getAllLiveNotes()
    }

    private fun getAllLiveNotes() {
        _status.value = LoadApiStatus.DONE
        liveNoteList = repository.getAllLiveNotes()
    }

    fun getAllTags(notes: List<Note>){
        val set = mutableSetOf<String>()
            for (note in notes) {
                for (tag in note.tags) {
                    set.add(tag)
                }
            }
        tagSet.value = set
    }

    fun navigateToNotePage(note: Note) {
        when (note.source) {
            Source.YOUTUBE.source -> {
                _navigateToYoutubeNote.value = note
            }

//            Source.SPOTIFY.source -> {
//                _navigateToSpotifyNote.value = note
//            }
        }

    }

    fun hideSelectedTagFromTagSet(tag: String) {
        selectedTag = tag
        tagSet.value = tagSet.value // invoke live data change
    }

}

data class NoteListUiState(
    val onTagClick: (String) -> Unit
)