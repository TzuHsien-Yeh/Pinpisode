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
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.deleteTimeItem
import com.tzuhsien.immediat.network.LoadApiStatus
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

    val uiState = YouTubeNoteUiState(
        onItemTitleChanged = { item ->
            updateTimeItem(item)
        },
        onItemContentChanged = { item ->
            updateTimeItem(item)
        },
        onItemToDelete = { item ->
            deleteTimeItem(item)
        }
    )

    var liveTimeItemList = MutableLiveData<List<TimeItem>>()

    var liveNoteData = MutableLiveData<Note?>()

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
        liveTimeItemList = repository.getLiveTimeItems(noteId)
        _status.value = LoadApiStatus.DONE
    }

    private fun getLiveNoteById() {
        liveNoteData = repository.getLiveNoteById(noteId)
        _status.value = LoadApiStatus.DONE
    }

    fun takeTimeStamp(second: Float) {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.addNewTimeItem(
                noteId = noteId,
                timeItem = TimeItem(startAt = second, endAt = null)
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

    fun playTimeItem(timeItem: TimeItem) {

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
}

data class YouTubeNoteUiState(
    var onItemTitleChanged: (TimeItem) -> Unit,
    var onItemContentChanged: (TimeItem) -> Unit,
    var onItemToDelete: (TimeItem) -> Unit
)

