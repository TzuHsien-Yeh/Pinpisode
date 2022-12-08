package com.tzuhsien.pinpisode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(private val repository: Repository): ViewModel() {

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        Timber.d("Timber init: ${UserManager.userId}")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun updateLocalUserId() {

        if (null == UserManager.userId) {
            coroutineScope.launch {
                _status.value = LoadApiStatus.LOADING

                when(val currentUserResult = repository.getCurrentUser()) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE
                        UserManager.userId = currentUserResult.data?.id
                        Timber.d("[${this::class.simpleName}]: repository.getCurrentUser(): ${UserManager.userId} = ${currentUserResult.data?.id}")
                        currentUserResult.data
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
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                    }
                }
            }
        }
    }
}

