package com.tzuhsien.pinpisode.youtubenote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class YouTubeNoteViewModel(
    private val repository: Repository,
    var noteId: String?,
    var videoId: String,
) : ViewModel() {

    // state of clipping btn
    var startOrStopToggle = 0

    // state of displaying options:
    var displayState: TimeItemDisplay = TimeItemDisplay.ALL

    var startAt: Float = 0f
    var endAt: Float = 0f

    private val _playStart = MutableLiveData<Float?>(null)
    val playStart: LiveData<Float?>
        get() = _playStart

    var playEnd: Float? = null

    private val _currentSecond = MutableLiveData<Float>()
    val currentSecond: LiveData<Float>
        get() = _currentSecond

    val uiState = YouTubeNoteUiState(
        onItemTitleChanged = { item ->
            updateTimeItem(item)
        },
        onItemContentChanged = { item ->
            updateTimeItem(item)
        },
        onTimeClick = { item ->
            playTimeItem(item)
        }
    )

    // initial value is the note info gotten once from firebase
    var noteToBeUpdated: Note? = null

    private var _liveNote = MutableLiveData<Note?>()
    val liveNote: LiveData<Note?>
        get() = _liveNote

    private var _isLiveNoteReady = MutableLiveData(false)
    val isLiveNoteReady: LiveData<Boolean>
        get() = _isLiveNoteReady

    private var _liveTimeItemList = MutableLiveData<List<TimeItem>>()
    val liveTimeItemList: LiveData<List<TimeItem>>
        get() = _liveTimeItemList

    private var _isLiveTimeItemListReady = MutableLiveData(false)
    val isLiveTimeItemListReady: LiveData<Boolean>
        get() = _isLiveTimeItemListReady


    /** Decide whether the viewer can edit the note **/
    private var _canEdit = MutableLiveData(false)
    val canEdit: LiveData<Boolean>
        get() = _canEdit

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /**
     *  Initialization procedures
     * **/
    init {
        if (null != noteId) {
            // NoteId passed in from noteList page or deeplink
            getNoteInfoById() // one time query
        } else {
            _canEdit.value = true
            checkVideoExistence(videoId)
        }
    }

    private fun getNoteInfoById() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = noteId?.let { repository.getNoteInfoById(noteId = it) }

            noteToBeUpdated = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    // save one time note info, check if user is in author list, and start listening to live data
                    checkIfViewerCanEdit(result.data.authors.contains(repository.getCurrentUser()?.id))
                    getLiveNoteById(result.data.id)
                    getLiveTimeItemsResult(result.data.id)
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

    private fun getLiveNoteById(noteId: String) {
        _liveNote = repository.getLiveNoteById(noteId)
        _status.value = LoadApiStatus.DONE

        _isLiveNoteReady.value = true
    }

    private fun getLiveTimeItemsResult(noteId: String) {
        _liveTimeItemList = repository.getLiveTimeItems(noteId)
        _status.value = LoadApiStatus.DONE

        // Let fragment know it's time to start observing the reassigned _liveTimeItemList LiveData
        _isLiveTimeItemListReady.value = true
    }

    private fun checkVideoExistence(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result =
                repository.checkIfNoteAlreadyExists(Source.YOUTUBE.source, videoId)) {
                is Result.Success -> {
                    _error.value = null

                    if (null == result.data) {
                        // note not exist, get info from yt and then create a new note
                        getYoutubeVideoInfoById(videoId)
                    } else {
                        // the note already exist, save one time note info, check if user is in author list, and start listening to live data
                        noteId = result.data.id
                        noteToBeUpdated = result.data
                        checkIfViewerCanEdit(result.data.authors.contains(repository.getCurrentUser()?.id))
                        getLiveNoteById(result.data.id)
                        getLiveTimeItemsResult(result.data.id)
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

    private fun getYoutubeVideoInfoById(videoId: String) {
        coroutineScope.launch {

            when (val result = repository.getYouTubeVideoInfoById(videoId)) {
                is Result.Success -> {
                    _error.value = null

                    createNewYouTubeNote(result.data.items[0])
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

    private fun createNewYouTubeNote(video: Item) {
        repository.getCurrentUser()?.let { currentUser ->
            val newYtNote = Note(
                sourceId = video.id,
                source = Source.YOUTUBE.source,
                ownerId = currentUser.id,
                authors = listOf(currentUser.id),
                tags = listOf(Source.YOUTUBE.source),
                thumbnail = video.snippet.thumbnails.high.url,
                title = video.snippet.title,
                duration = video.contentDetails.duration
            )

            coroutineScope.launch {

                val result = repository.createNote(
                    source = Source.YOUTUBE.source,
                    sourceId = video.id,
                    note = newYtNote
                )

                noteToBeUpdated = when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        // new note created,
                        // save one time note info & noteId, check if user is in author list, and start listening to live data
                        noteId = result.data.id
                        checkIfViewerCanEdit(result.data.authors.contains(currentUser.id))
                        getLiveNoteById(result.data.id)
                        getLiveTimeItemsResult(result.data.id)
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
    }

    private fun checkIfViewerCanEdit(isInAuthors: Boolean) {
        _canEdit.value = isInAuthors
        uiState.canEdit = isInAuthors
    }
    /** End of initialization of the note **/


    /**
     * Deal with editing and updating
     * **/
    fun createTimeItem(startAt: Float, endAt: Float?) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = noteId?.let {
                repository.addNewTimeItem(
                    noteId = it,
                    timeItem = TimeItem(startAt = startAt, endAt = endAt)
                )
            }) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                    Timber.d("liveNoteData.value?.lastTimestamp = ${liveNote.value?.lastTimestamp}")
                    liveNote.value?.let { noteFromDb ->
                        if (noteFromDb.lastTimestamp < startAt) {
                            if (null != endAt && noteFromDb.lastTimestamp < endAt) {
                                noteToBeUpdated?.lastTimestamp = endAt
                                updateNote()
                            } else {
                                noteToBeUpdated?.lastTimestamp = startAt
                                updateNote()
                            }
                        } else {
                            noteToBeUpdated?.lastTimestamp = noteFromDb.lastTimestamp
                            updateNote()
                        }
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

    private fun updateTimeItem(timeItem: TimeItem) {
        coroutineScope.launch {
            when (val result = noteId?.let { repository.updateTimeItem(it, timeItem) }) {
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
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }

    }

    fun deleteTimeItem(timeItemIndex: Int) {

        val timeItemToDelete = liveTimeItemList.value?.get(timeItemIndex)

        timeItemToDelete?.let {

            coroutineScope.launch {

                when (val result =
                    noteId?.let { repository.deleteTimeItem(it, timeItemToDelete) }) {
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
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }

        }

    }

    private fun playTimeItem(timeItem: TimeItem) {
        _playStart.value = timeItem.startAt
        playEnd = timeItem.endAt
    }

    fun updateNote() {
        coroutineScope.launch {

            noteToBeUpdated?.let { noteToBeUpdated ->

                when (val result = repository.updateNote(noteToBeUpdated.id, noteToBeUpdated)) {
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
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }

    fun clearPlayingMomentStart() {
        _playStart.value = null
    }

    fun clearPlayingMomentEnd() {
        playEnd = null
    }

    fun getCurrentSecond(second: Float) {
        _currentSecond.value = second
    }

    fun notifyDisplayChange() {
        _liveTimeItemList.value = _liveTimeItemList.value
    }

    fun updateInfoFromYouTube(note: Note) {
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
                        updateYouTubeInfo(noteToUpdate)
                    }
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
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    private fun updateYouTubeInfo(note: Note) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = noteId?.let {
                repository.updateNoteInfoFromSourceApi(noteId = it, note = note)
            }) {
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
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

data class YouTubeNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    val onTimeClick: (TimeItem) -> Unit,
    var canEdit: Boolean = false,
)