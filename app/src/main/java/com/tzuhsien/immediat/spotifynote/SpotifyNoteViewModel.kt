package com.tzuhsien.immediat.spotifynote

import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.spotify.protocol.types.PlayerState
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.TimeItemDisplay
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
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

    var playingState = PlayingState.STOPPED

    private val _getInfoFromPlayerState = MutableLiveData<PlayerState?>(null)
    val getInfoFromPlayerState: LiveData<PlayerState?>
        get() = _getInfoFromPlayerState

    private val _isSpotifyConnected = MutableLiveData<Boolean>(false)
    val isSpotifyConnected: LiveData<Boolean>
        get() = _isSpotifyConnected

    // initial value is the note info gotten once from firebase
    var noteToBeUpdated: Note? = null

    private val _shouldCreateNewNote = MutableLiveData<Boolean>(false)
    val shouldCreateNewNote: LiveData<Boolean>
        get() = _shouldCreateNewNote

    private var _liveNoteData = MutableLiveData<Note?>()
    val liveNoteData: LiveData<Note?>
        get() = _liveNoteData

    private var _liveNoteDataReassigned = MutableLiveData(false)
    val liveNoteDataReassigned: LiveData<Boolean>
        get() = _liveNoteDataReassigned

    private var _liveTimeItemList = MutableLiveData<List<TimeItem>>()
    val liveTimeItemList: LiveData<List<TimeItem>>
        get() = _liveTimeItemList

    private var _timeItemLiveDataReassigned = MutableLiveData<Boolean>(false)
    val timeItemLiveDataReassigned: LiveData<Boolean>
        get() = _timeItemLiveDataReassigned


    /** current position info from playerState **/
    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long>
        get() = _currentPosition

    // state of clipping btn
    var startOrStopToggle = 0

    var startAt: Float = 0f
    var endAt: Float = 0f

    /**  play the time items **/
    private val _toPlay = MutableLiveData<List<Float?>?>(null)
    val toPlay: LiveData<List<Float?>?>
        get() = _toPlay

    private val _playStart = MutableLiveData<Float?>(null)
    val playStart: LiveData<Float?>
        get() = _playStart

    private val _clipLength = MutableLiveData<Float?>(null)
    val clipLength: LiveData<Float?>
        get() = _clipLength

    /** Decide whether the viewer can edit the note **/
    private var _canEdit = MutableLiveData<Boolean>(false)
    val canEdit: LiveData<Boolean>
        get() = _canEdit

    // state of displaying options:
    var displayState: TimeItemDisplay = TimeItemDisplay.ALL


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
            checkSpotifyNoteExistence(sourceId)
        }
    }

    private fun connectToSpotify() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val connectResult = SpotifyService.connectToAppRemote()

            if (connectResult.isConnected) {
                _status.value = LoadApiStatus.DONE
                _isSpotifyConnected.value = true
            } else {
                _error.value = "Spotify is not connected"
            }

        }
    }

    private fun getNoteInfoById() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getNoteInfoById(noteId = noteId!!)

            noteToBeUpdated = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    // save one time note info, check if user is in author list, and start listening to live data
                    checkIfViewerCanEdit(result.data.authors.contains(UserManager.userId))
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
                    _error.value = MyApplication.instance.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }

    }

    private fun getLiveNoteById(noteId: String) {
        _liveNoteData = repository.getLiveNoteById(noteId)
        _status.value = LoadApiStatus.DONE

        _liveNoteDataReassigned.value = true
    }

    private fun getLiveTimeItemsResult(noteId: String) {
        _liveTimeItemList = repository.getLiveTimeItems(noteId)
        _status.value = LoadApiStatus.DONE

        // Let fragment know it's time to start observing the reassigned _liveTimeItemList LiveData
        _timeItemLiveDataReassigned.value = true
    }

    private fun checkSpotifyNoteExistence(sourceId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.checkIfNoteAlreadyExists(Source.SPOTIFY.source, sourceId)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    if (null == result.data) {
                        // note not exist, create a new note with info from playerState
                        _shouldCreateNewNote.value = true
                    } else {
                        // the note already exist, save one time note info, check if user is in author list, and start listening to live data
                        noteToBeUpdated = result.data
                        checkIfViewerCanEdit(result.data.authors.contains(UserManager.userId))
                        getLiveNoteById(result.data.id)
                        getLiveTimeItemsResult(result.data.id)
                        createNewNoteFinished()
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
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.createNote(
                source = Source.SPOTIFY.source,
                sourceId = sourceId,
                note = newSpotifyNote
            )

            noteToBeUpdated = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    // new note created,
                    // save one time note info & noteId, check if user is in author list, and start listening to live data
                    noteId = result.data.id
                    checkIfViewerCanEdit(result.data.authors.contains(UserManager.userId))
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
                    _error.value = MyApplication.instance.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }
    }

    fun updateNewInfo(state: PlayerState) {
        _getInfoFromPlayerState.value = state
    }

    fun createNewNoteFinished() {
        _shouldCreateNewNote.value = false
        _getInfoFromPlayerState.value = null
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

            when (val result = repository.addNewTimeItem(
                noteId = noteId!!,
                timeItem = TimeItem(startAt = startAt, endAt = endAt)
            )) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                    Timber.d("liveNoteData.value?.lastTimestamp = ${liveNoteData.value?.lastTimestamp}")
                    liveNoteData.value?.let { noteFromDb ->
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
                    _error.value = MyApplication.instance.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    private fun updateTimeItem(timeItem: TimeItem) {
        coroutineScope.launch {
            when (val result = repository.updateTimeItem(noteId!!, timeItem)) {
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

    fun deleteTimeItem(timeItemIndex: Int) {
        coroutineScope.launch {
            val timeItemToDelete = liveTimeItemList.value?.get(timeItemIndex)

            when (val result = repository.deleteTimeItem(noteId!!, timeItemToDelete!!)) {
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

    private fun playTimeItem(timeItem: TimeItem) {
        _playStart.value = timeItem.startAt
        _clipLength.value = (timeItem.endAt?.minus(timeItem.startAt))
        _toPlay.value = listOf(timeItem.startAt, timeItem.endAt)
    }

    fun updateNote() {
        coroutineScope.launch {
            when (val result = repository.updateNote(noteId!!, noteToBeUpdated!!)) {
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

    fun clearPlayingMomentStart() {
        _playStart.value = null
    }

    fun clearPlayingMomentEnd() {
        _clipLength.value = null
    }

    fun clearPlaying() {
        _toPlay.value = null
    }

    fun notifyDisplayChange() {
        _liveTimeItemList.value = _liveTimeItemList.value
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /**
     *  Get player position every 0.5 sec
     * **/
    val positionHandler = Handler()
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            _currentPosition.value = _currentPosition.value?.plus(500L)
            positionHandler.postDelayed(this, 500.toLong())
        }
    }

    fun updateCurrentPosition(position: Long) {
        _currentPosition.value = position
    }

    fun startTrackingPosition() {
        positionUpdateRunnable.run()
    }

    fun pauseTrackingPosition() {
        positionHandler.removeCallbacks(positionUpdateRunnable)
    }

    fun unpauseTrackingPosition() {
        positionHandler.removeCallbacks(positionUpdateRunnable)
        positionHandler.postDelayed(positionUpdateRunnable, 500)
    }


}

data class SpotifyNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    val onTimeClick: (TimeItem) -> Unit,
    var canEdit: Boolean = false
)