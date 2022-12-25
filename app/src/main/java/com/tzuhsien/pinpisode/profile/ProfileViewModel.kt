package com.tzuhsien.pinpisode.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.*


class ProfileViewModel(private val repository: Repository) : ViewModel() {

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun getCurrentUser(): UserInfo? {
        return repository.getCurrentUser()
    }

    fun updateUser() {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            val result = withContext(Dispatchers.IO) {
                repository.updateCurrentUser()
            }

            when (result) {
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

    fun signOut() {
        GoogleSignIn.getClient(MyApplication.applicationContext(),
            GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        Firebase.auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}