package com.tzuhsien.immediat.youtubenote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.ServiceLocator.repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class YouTubeNoteViewModel(
    private val repository: Repository,
    private val noteId: String,
    val videoId: String
    ) : ViewModel() {

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

    private var _liveNoteData = MutableLiveData<Note?>()
    val liveNoteData: LiveData<Note?>
        get() = _liveNoteData

    var newNote: Note = Note()

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
        getLiveTimeItemsResult()
        getLiveNoteById()
    }

    private fun getLiveTimeItemsResult() {
        _liveTimeItemList = repository.getLiveTimeItems(noteId)
        _status.value = LoadApiStatus.DONE
    }

    private fun getLiveNoteById() {
        _liveNoteData = repository.getLiveNoteById(noteId)
        _status.value = LoadApiStatus.DONE
    }

    fun createTimeItem(startAt: Float, endAt: Float?) {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.addNewTimeItem(
                noteId = noteId,
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

            when (val result = repository.updateTimeItem(noteId, timeItem)) {
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

            when (val result = repository.deleteTimeItem(noteId, timeItem)) {
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

            when (val result = repository.updateNote(noteId, newNote)) {
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
}

data class YouTubeNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    var onItemToDelete: (TimeItem) -> Unit,
    val onTimeClick: (TimeItem) -> Unit
)