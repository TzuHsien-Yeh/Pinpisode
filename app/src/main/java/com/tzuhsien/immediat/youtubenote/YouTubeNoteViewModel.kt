package com.tzuhsien.immediat.youtubenote

import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.deleteTimeItem
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.getLiveNoteById
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.updateNote
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.updateTimeItem
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.ServiceLocator.repository
import com.tzuhsien.immediat.util.Util
import com.tzuhsien.immediat.util.Util.getString
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

    var playMomentEnd: Float? = null

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
        onItemToDelete = { item ->
            deleteTimeItem(item)
        },
        onTimeClick = { item ->
            playTimeItem(item)
        }
    )

    private var _liveTimeItemList = MutableLiveData<List<TimeItem>>()
    val liveTimeItemList: LiveData<List<TimeItem>>
        get() = _liveTimeItemList

    private var _reObserveTimeItems = MutableLiveData<Boolean>(false)
    val reObserveTimeItems: LiveData<Boolean>
        get() = _reObserveTimeItems

    private var _liveNoteData = MutableLiveData<Note?>()
    val liveNoteData: LiveData<Note?>
        get() = _liveNoteData

    // updating note data from live data from firebase
    var newNote: Note = Note()

    // Get yt video data if entering youtubeNote page from yt deeplink
    private val _ytVideoData = MutableLiveData<YouTubeResult?>(null)
    val ytVideoData: LiveData<YouTubeResult?>
        get() = _ytVideoData

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        Timber.d("[${this::class.simpleName}] noteId Passed in: $noteId")

        if (null != noteId) {
            // NoteId passed in from search page or noteList page
            getLiveNoteById()
        } else {
            checkVideoExistence(videoId)
        }
    }

    fun getLiveTimeItemsResult(id: String) {
        _liveTimeItemList = repository.getLiveTimeItems(id)
        _status.value = LoadApiStatus.DONE

        // Let fragment know it's time to start observing the reassigned _liveTimeItemList LiveData
        _reObserveTimeItems.value = true
    }

    private fun getLiveNoteById() {
        noteId?.let {
            _liveNoteData = repository.getLiveNoteById(it)
            _status.value = LoadApiStatus.DONE
        }
    }

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
                                newNote.lastTimestamp = endAt
                                updateNote()
                            } else {
                                newNote.lastTimestamp = startAt
                                updateNote()
                            }
                        } else {
                            newNote.lastTimestamp = noteFromDb.lastTimestamp
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

    private fun deleteTimeItem(timeItem: TimeItem) {
        coroutineScope.launch {
            when (val result = repository.deleteTimeItem(noteId!!, timeItem)) {
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
        playMomentEnd = timeItem.endAt
    }


    fun updateNote() {
        coroutineScope.launch {
            when (val result = repository.updateNote(noteId!!, newNote)) {
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

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun clearPlayingMomentStart() {
        _playStart.value = null
    }

    fun clearPlayingMomentEnd() {
        playMomentEnd = null
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

    private fun updateYouTubeInfo(note: Note) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.updateYouTubeInfo(
                noteId = noteId!!,
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


    private fun checkVideoExistence(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.checkIfYouTubeNoteExists(videoId)

            _liveNoteData.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("checkIfYouTubeNoteExists: result ${result.data}")

                    if (null == result.data) {
                        // get info from yt and then create a new note
                        getYoutubeVideoInfoById(videoId)
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

    private fun getYoutubeVideoInfoById(videoId: String) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.getYouTubeVideoInfoById(videoId)

            _ytVideoData.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    createNewYouTubeNote(result.data.items[0])
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

    private fun createNewYouTubeNote(video: Item) {
        val newYtNote = Note(
            sourceId = video.id,
            source = Source.YOUTUBE.source,
            ownerId = UserManager.userId,
            authors = listOf(UserManager.userId),
            tags = listOf(Source.YOUTUBE.source),
            thumbnail = video.snippet.thumbnails.high.url,
            title = video.snippet.title,
            duration = video.contentDetails.duration
        )

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.createYouTubeVideoNote(video.id, newYtNote)

            _liveNoteData.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
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

}

data class YouTubeNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    var onItemToDelete: (TimeItem) -> Unit,
    val onTimeClick: (TimeItem) -> Unit,
)