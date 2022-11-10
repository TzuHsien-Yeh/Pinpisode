package com.tzuhsien.immediat.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber


class ProfileViewModel(private val repository: Repository) : ViewModel() {

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun updateLocalUserId() {

        if (null == UserManager.userId) {
            coroutineScope.launch {
                _status.value = LoadApiStatus.LOADING

                when(val currentUserResult = repository.getCurrentUser()) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        Timber.d("[${this::class.simpleName}]: repository.getCurrentUser(): ${UserManager.userId} = ${currentUserResult.data?.id}")
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
}