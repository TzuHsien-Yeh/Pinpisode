package com.tzuhsien.immediat.notelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.updateNote
import com.tzuhsien.immediat.ext.parseDuration
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.*
import org.checkerframework.checker.units.qual.s
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

    private val _updatedYoutubeResult = MutableLiveData<YouTubeResult>()
    val updatedYoutubeResult: LiveData<YouTubeResult>
        get() = _updatedYoutubeResult

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _navigateToYoutubeNote = MutableLiveData<Note?>(null)
    val navigateToYoutubeNote: LiveData<Note?>
        get() = _navigateToYoutubeNote

    init {
        getAllLiveNotes()
        _tagSet.value = UserManager.tagSet
    }

    private fun getAllLiveNotes() {
        _status.value = LoadApiStatus.DONE
        _liveNoteList = repository.getAllLiveNotes()
    }

    fun updateInfoFromYouTube(noteId: String, note: Note) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.getYouTubeVideoInfoById(note.sourceId)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    if (result.data.items[0].contentDetails.duration != note.duration) {
                        val noteToUpdate = Note()
                        noteToUpdate.duration = result.data.items[0].contentDetails.duration
                        noteToUpdate.thumbnail = result.data.items[0].snippet.thumbnails.high.url
                        noteToUpdate.title = result.data.items[0].snippet.title
                        updateYouTubeInfo(noteId, noteToUpdate)
                    }
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    null
                }
                else -> {
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }
    }

    private fun updateYouTubeInfo(noteId: String, note: Note) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.updateYouTubeInfo(
                noteId = noteId,
                note = note
            )) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                }
                else -> {
                    _error.value = MyApplication.instance.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
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