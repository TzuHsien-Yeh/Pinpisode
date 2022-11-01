package com.tzuhsien.immediat.youtubenote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class YouTubeNoteViewModel(private val repository: Repository, val videoId: String) : ViewModel() {

    private val _timeItemList = MutableLiveData<List<TimeItem>>()
    val timeItemList: LiveData<List<TimeItem>>
        get() = _timeItemList

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    val testTime = 10.3

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        Timber.d("videoId Passed in TakeNote: $videoId")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun takeTimeStamp(second: Float) {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.addNewTimeItem(
                videoId = videoId,
                timeItem = TimeItem(startAt = second, endAt = null)
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

    fun playTimeItem(timeItem: TimeItem) {

    }
}