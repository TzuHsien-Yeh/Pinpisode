package com.tzuhsien.immediat.notelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import timber.log.Timber

class NoteListViewModel(private val repository: Repository) : ViewModel() {
    var liveNoteList = MutableLiveData<List<Note>>()

    val set = mutableSetOf<String>()
    var tagSet = MutableLiveData<Set<String>>()

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _navigateToYoutubeNote = MutableLiveData<Note>()
    val navigateToYoutubeNote: LiveData<Note>
        get() = _navigateToYoutubeNote

    init {
        getAllLiveNotes()
    }

    private fun getAllLiveNotes() {
        _status.value = LoadApiStatus.DONE
        liveNoteList = repository.getAllLiveNotes()
    }

    fun getAllTags(notes: List<Note>){
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

    fun removeSelectedTagFromTagSet(selectedTag: String) {
        set.filter { it == selectedTag }.forEach{ set.remove(it) }
        tagSet.value = set
        Timber.d ("set.filter { it == selectedTag }.forEach{ set.remove(it) } : $set")
    }
}