package com.tzuhsien.immediat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel(private val repository: Repository): ViewModel() {

    private val _currentUser = MutableLiveData<UserInfo?>()
    val currentUser: LiveData<UserInfo?>
        get() = _currentUser

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        if (null == UserManager.userId) {
            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                val result = repository.getCurrentUser()

                _currentUser.value = when (result) {
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
                        _error.value = Util.getString(R.string.unknown_error)
                        _status.value = LoadApiStatus.ERROR
                        null
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

