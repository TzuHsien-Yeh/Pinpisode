package com.tzuhsien.immediat.notelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Sort
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.ext.parseDuration
import com.tzuhsien.immediat.network.LoadApiStatus
import timber.log.Timber

class NoteListViewModel(private val repository: Repository) : ViewModel() {
    private var _liveNoteList = MutableLiveData<List<Note>>()
    val liveNoteList: LiveData<List<Note>>
        get() = _liveNoteList

    var set = mutableSetOf<String>()

    private val _tagSet = MutableLiveData<Set<String>>()
    val tagSet: LiveData<Set<String>>
        get() = _tagSet

    var selectedTag: String? = null

    var isAscending: Boolean = true // 0: Ascending order; 1: Descending

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _navigateToYoutubeNote = MutableLiveData<Note?>(null)
    val navigateToYoutubeNote: LiveData<Note?>
        get() = _navigateToYoutubeNote

    init {
        getAllLiveNotes()
    }

    private fun getAllLiveNotes() {
        _status.value = LoadApiStatus.DONE
        _liveNoteList = repository.getAllLiveNotes()
    }

    fun getAllTags(notes: List<Note>) {
        val set = mutableSetOf<String>()
        for (note in notes) {
            for (tag in note.tags) {
                set.add(tag)
            }
        }
        _tagSet.value = set
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

    fun tagSelected(tag: String?) {
        selectedTag = tag

        // invoke live data change
        _tagSet.value = _tagSet.value
        _liveNoteList.value = _liveNoteList.value
    }

    fun sortNotes(by: Sort) {
        when (by) {
            Sort.LAST_EDIT -> {
                _liveNoteList.value = if (isAscending) {
                    _liveNoteList.value?.sortedByDescending { it.lastEditTime }
                } else {
                    _liveNoteList.value?.sortedBy { it.lastEditTime }
                }
            }
            Sort.DURATION -> {
                _liveNoteList.value = if (isAscending) {
                    _liveNoteList.value?.sortedBy { it.duration.parseDuration() }
                } else {
                    _liveNoteList.value?.sortedByDescending { it.duration.parseDuration() }
                }
            }
            Sort.TIME_LEFT -> {
                _liveNoteList.value = if (isAscending) {
                    _liveNoteList.value?.sortedBy {
                        (it.duration.parseDuration()?.minus(it.lastTimestamp.toLong() * 1000))
                    }
                } else {
                    _liveNoteList.value?.sortedByDescending {
                        (it.duration.parseDuration()?.minus(it.lastTimestamp.toLong() * 1000))
                    }
                }
            }

        }
    }

    fun changeOrderDirection() {
        _liveNoteList.value = _liveNoteList.value?.reversed()
    }

    fun doneNavigationToYtNote() {
        _navigateToYoutubeNote.value = null
    }

}