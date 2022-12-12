package com.tzuhsien.pinpisode.spotifynote

import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.model.TimeItem
import com.tzuhsien.pinpisode.data.model.TimeItemDisplay
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import com.tzuhsien.pinpisode.util.Util.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

const val SPOTIFY_URI = "spotify:"

class SpotifyNoteViewModel(
    private val repository: Repository,
    var noteId: String?,
    var sourceId: String,
) : ViewModel() {
    var hasUploaded: Boolean = false

    var newSpotifyNote: Note = Note()

    var playingState = PlayingState.STOPPED

    private val _isSpotifyConnected = MutableLiveData<Boolean>(false)
    val isSpotifyConnected: LiveData<Boolean>
        get() = _isSpotifyConnected

    // initial value is the note info gotten once from firebase
    var noteToBeUpdated: Note? = null

    private val _shouldCreateNewNote = MutableLiveData<Boolean>(false)
    val shouldCreateNewNote: LiveData<Boolean>
        get() = _shouldCreateNewNote

    private var _liveNote = MutableLiveData<Note?>()
    val liveNote: LiveData<Note?>
        get() = _liveNote

    private var _isLiveNoteReady = MutableLiveData(false)
    val isLiveNoteReady: LiveData<Boolean>
        get() = _isLiveNoteReady

    private var _liveTimeItemList = MutableLiveData<List<TimeItem>>()
    val liveTimeItemList: LiveData<List<TimeItem>>
        get() = _liveTimeItemList

    private var _isLiveTimeItemListReady = MutableLiveData<Boolean>(false)
    val isLiveTimeItemListReady: LiveData<Boolean>
        get() = _isLiveTimeItemListReady


    /** current position info from playerState **/
    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long>
        get() = _currentPosition

    var currentSecond: Long = 0L

    /** Clipping **/
    // state of clipping btn
    var startOrStopToggle = 0

    // Save start and stop position of a clip
    var startAt: Float = 0f
    var endAt: Float = 0f

    /**  play the time items **/
    private val _playStart = MutableLiveData<Long?>(null)
    val playStart: LiveData<Long?>
        get() = _playStart
    var playEnd: Long? = null

    /** Decide whether the viewer can edit the note **/
    private var _canEdit = MutableLiveData(false)
    val canEdit: LiveData<Boolean>
        get() = _canEdit

    // For deciding whether to launch foreground service after spotify connection
    var isViewerCanEdit: Boolean = true

    // state of displaying options:
    var displayState: TimeItemDisplay = TimeItemDisplay.ALL

    private var _connectErrorMsg = MutableLiveData<String?>()
    val connectErrorMsg: LiveData<String?>
        get() = _connectErrorMsg

    val uiState = SpotifyNoteUiState(
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

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /**
     *  Initialization of the note
     * **/
    init {
        connectToSpotify()
        if (null != noteId) {
            // NoteId passed in from noteList page or deeplink
            getNoteInfoById() // one time query
        } else {
            _canEdit.value = true
            isViewerCanEdit = true
            checkSpotifyNoteExistence(sourceId)
        }
    }

    private fun connectToSpotify() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            SpotifyService.connectToAppRemote(MyApplication.applicationContext()) { connection ->
                when (connection) {
                    ConnectState.CONNECTED -> {
                        _isSpotifyConnected.value = true
                        _status.value = LoadApiStatus.DONE
                        _connectErrorMsg.value = null
                    }
                    ConnectState.NOT_INSTALLED -> {
                        Timber.d("ConnectState.NOT_installed")
                        _connectErrorMsg.value = ConnectState.NOT_INSTALLED.msg
                    }
                    ConnectState.NOT_LOGGED_IN -> {
                        Timber.d("ConnectState.NOT_LOGGED_IN")
                        _connectErrorMsg.value = ConnectState.NOT_LOGGED_IN.msg
                    }
                    ConnectState.UNKNOWN_ERROR -> {
                        Timber.d("ConnectState.UNKNOWN")
                        _connectErrorMsg.value = ConnectState.UNKNOWN_ERROR.msg
                    }
                }
            }
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
                    _error.value = getString(R.string.unknown_error)
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

    private fun checkSpotifyNoteExistence(sourceId: String) {
        Timber.d("checkSpotifyNoteExistence sourceId: $sourceId")
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result =
                repository.checkIfNoteAlreadyExists(Source.SPOTIFY.source, sourceId)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("checkSpotifyNoteExistence: success")
                    if (null == result.data) {
                        // note not exist, create a new note with info from playerState
                        _shouldCreateNewNote.value = true
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

    fun createNewSpotifyNote(newSpotifyNote: Note) {
        Timber.d("createNewSpotifyNote(newSpotifyNote)")

        repository.getCurrentUser()?.let { currentUser ->

            newSpotifyNote.apply {
                ownerId = currentUser.id
                authors = listOf(currentUser.id)
            }

            if (!hasUploaded) {
                coroutineScope.launch {

                    val result = repository.createNote(
                        source = Source.SPOTIFY.source,
                        sourceId = sourceId,
                        note = newSpotifyNote
                    )

                    noteToBeUpdated = when (result) {
                        is Result.Success -> {
                            _error.value = null
                            _status.value = LoadApiStatus.DONE

                            Timber.d("createNewSpotifyNote SUCCESS")
                            // new note created
                            hasUploaded = true
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
                            hasUploaded = false
                            null
                        }
                        is Result.Error -> {
                            _error.value = result.exception.toString()
                            _status.value = LoadApiStatus.ERROR

                            hasUploaded = false
                            null
                        }
                        else -> {
                            _error.value = getString(R.string.unknown_error)
                            _status.value = LoadApiStatus.ERROR

                            hasUploaded = false
                            null
                        }
                    }
                }
            } else {
                _status.value = LoadApiStatus.DONE
            }
        }

    }

    private fun checkIfViewerCanEdit(isInAuthors: Boolean) {
        _canEdit.value = isInAuthors
        uiState.canEdit = isInAuthors
        isViewerCanEdit = isInAuthors
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
                    _error.value = getString(R.string.unknown_error)
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
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }

    }

    fun deleteTimeItem(timeItemIndex: Int) {

        coroutineScope.launch {

            val timeItemToDelete = liveTimeItemList.value?.get(timeItemIndex)

            timeItemToDelete?.let {
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
                        _error.value = getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }

        }

    }

    private fun playTimeItem(timeItem: TimeItem) {
        _playStart.value = timeItem.startAt.toLong().times(1000)
        playEnd = timeItem.endAt?.toLong()?.times(1000)
    }

    fun clearPlayingMomentStart() {
        _playStart.value = null
    }

    fun clearPlayingMomentEnd() {
        playEnd = null
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
                        _error.value = getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }

    fun invokeCreateNewNoteLiveData() {
        _shouldCreateNewNote.value = _shouldCreateNewNote.value
    }

    fun notifyDisplayChange() {
        _liveTimeItemList.value = _liveTimeItemList.value
    }

    /**
     *  Get player position every 0.20 sec
     * **/
    val positionHandler = Handler()
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            _currentPosition.value = _currentPosition.value?.plus(200L)
            currentSecond += 200L
            positionHandler.postDelayed(this, 200.toLong())
        }
    }

    fun updateCurrentPosition(position: Long) {
        _currentPosition.value = position
        currentSecond = position
    }

    fun startTrackingPosition() {
        positionUpdateRunnable.run()
    }

    fun pauseTrackingPosition() {
        positionHandler.removeCallbacks(positionUpdateRunnable)
    }

    fun unpauseTrackingPosition() {
        positionHandler.removeCallbacks(positionUpdateRunnable)
        positionHandler.postDelayed(positionUpdateRunnable, 200)
    }

    fun clearConnectionErrorMsg() {
        _connectErrorMsg.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

data class SpotifyNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    val onTimeClick: (TimeItem) -> Unit,
    var canEdit: Boolean = false,
)