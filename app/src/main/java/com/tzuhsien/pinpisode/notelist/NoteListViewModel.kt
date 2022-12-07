package com.tzuhsien.pinpisode.notelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.Sort
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.ext.parseDuration
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class NoteListViewModel(private val repository: Repository) : ViewModel() {

    private var _liveNoteList = repository.getAllLiveNotes()
    val liveNoteList: LiveData<List<Note>>
        get() = _liveNoteList

    val noteListToDisplay = Transformations.map(liveNoteList) { list ->
        val toDisplay =
            if (null != selectedTag) list.filter { it.tags.contains(selectedTag) } else list

        when (sortOption) {
            Sort.LAST_EDIT -> {
                if (isAscending) {
                    // The more recently edited note has larger time in millis
                    toDisplay.sortedByDescending { it.lastEditTime }
                } else {
                    toDisplay.sortedBy { it.lastEditTime }
                }
            }
            Sort.DURATION -> {
                if (isAscending) {
                    toDisplay.sortedBy { it.duration.parseDuration() }
                } else {
                    toDisplay.sortedByDescending { it.duration.parseDuration() }
                }
            }
            Sort.TIME_LEFT -> {
                if (isAscending) {
                    toDisplay.sortedBy {
                        (it.duration.parseDuration()?.minus(it.lastTimestamp.toLong() * 1000))
                    }
                } else {
                    toDisplay.sortedByDescending {
                        (it.duration.parseDuration()?.minus(it.lastTimestamp.toLong() * 1000))
                    }
                }
            }
        }
    }

    val tagsToDisplay = Transformations.map(liveNoteList) { notes ->
        val set = mutableSetOf<String>()
        for (note in notes) {
            for (tag in note.tags) {
                set.add(tag)
            }
        }
        set.filter { it != selectedTag }
    }

    var selectedTag: String? = null

    var isAscending: Boolean = true

    var sortOption: Sort = Sort.LAST_EDIT

    var invitationList = repository.getLiveIncomingCoauthorInvitations()

    val uiState = NoteListUiState(
        onNoteClicked = { note ->
            navigateToNotePage(note)
        },
        onNoteQuit = { position ->
            deleteOrQuitCoauthoringNote(position)
        }
    )

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

    private val _navigateToSpotifyNote = MutableLiveData<Note?>(null)
    val navigateToSpotifyNote: LiveData<Note?>
        get() = _navigateToSpotifyNote

    fun updateInfoFromYouTube(note: Note) {

        if (note.source == Source.YOUTUBE.source && note.duration.parseDuration() == 0L) {

            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                when (val result = repository.getYouTubeVideoInfoById(note.sourceId)) {
                    is Result.Success -> {
                        _error.value = null

                        if (result.data.items[0].contentDetails?.duration != note.duration) {
                            val noteToUpdate = Note()
                            noteToUpdate.duration = result.data.items[0].contentDetails!!.duration
                            noteToUpdate.thumbnail =
                                result.data.items[0].snippet.thumbnails.high.url
                            noteToUpdate.title = result.data.items[0].snippet.title
                            updateYouTubeInfo(note.id, noteToUpdate)
                        } else {

                            _status.value = LoadApiStatus.DONE
                        }
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
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }

    private fun updateYouTubeInfo(noteId: String, note: Note) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.updateNoteInfoFromSourceApi(
                noteId = noteId,
                note = note
            )) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
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

    private fun navigateToNotePage(note: Note) {
        when (note.source) {
            Source.YOUTUBE.source -> {
                _navigateToYoutubeNote.value = note
            }

            Source.SPOTIFY.source -> {
                _navigateToSpotifyNote.value = note
            }
        }
    }

    fun doneNavigationToNote() {
        _navigateToYoutubeNote.value = null
        _navigateToSpotifyNote.value = null
    }

    fun updateLocalUserId() {

        if (null == UserManager.userId) {

            coroutineScope.launch {
                _status.value = LoadApiStatus.LOADING

                when (val currentUserResult = repository.getCurrentUser()) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE
                        UserManager.userId = currentUserResult.data?.id
                    }
                    is Result.Fail -> {
                        _error.value = currentUserResult.error
                        _status.value = LoadApiStatus.ERROR
                    }
                    is Result.Error -> {
                        _error.value = currentUserResult.exception.toString()
                        _status.value = LoadApiStatus.ERROR
                    }
                    else -> {
                        _error.value = MyApplication.instance.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }


    fun deleteOrQuitCoauthoringNote(noteIndex: Int) {
        Timber.d("deleteOrQuitCoauthoringNote: noteIndex = $noteIndex")

        val noteToBeRemoved = noteListToDisplay.value?.get(noteIndex)
        Timber.d("deleteOrQuitCoauthoringNote: noteToBeRemoved = $noteToBeRemoved")

        if (noteToBeRemoved?.ownerId == UserManager.userId) {
            deleteNote(noteToBeRemoved!!)
        } else {
            quitCoauthoringNote(noteToBeRemoved!!)
        }
    }

    private fun quitCoauthoringNote(noteToBeRemoved: Note) {

        val newAuthorList = mutableListOf<String>()
        newAuthorList.addAll(noteToBeRemoved.authors)
        newAuthorList.remove(UserManager.userId)

        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            when (val result = repository.deleteUserFromAuthors(
                noteId = noteToBeRemoved.id,
                authors = newAuthorList
            )) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
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

    private fun deleteNote(noteToBeRemoved: Note) {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            when (val result = repository.deleteNote(noteId = noteToBeRemoved.id)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
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

    fun tagSelected(tag: String?) {
        selectedTag = tag

        // notify live data change
        _liveNoteList.value = _liveNoteList.value
    }

    fun sort(sort: Sort) {
        sortOption = sort

        // default order direction for each sorting option
        isAscending = when (sort) {
            Sort.LAST_EDIT -> true
            Sort.DURATION -> true
            Sort.TIME_LEFT -> false
        }

        // notify live data change
        _liveNoteList.value = _liveNoteList.value
    }

    fun changeSortDirection() {
        isAscending = !isAscending

        // notify live data change
        _liveNoteList.value = _liveNoteList.value
    }
}

data class NoteListUiState(
    val onNoteClicked: (Note) -> Unit,
    val onNoteQuit: (Int) -> Unit,
)