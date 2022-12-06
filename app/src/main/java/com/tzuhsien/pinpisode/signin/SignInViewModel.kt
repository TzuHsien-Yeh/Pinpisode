package com.tzuhsien.pinpisode.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SignInViewModel(private val repository: Repository): ViewModel() {

    var source: String? = null
    var sourceId: String? = null

    private val _navigateUp = MutableLiveData(false)
    val navigateUp: LiveData<Boolean>
        get() = _navigateUp

    private val _navigateToYtNote = MutableLiveData<String?>(null)
    val navigateToYtNote: LiveData<String?>
        get() = _navigateToYtNote

    private val _navigateToSpNote = MutableLiveData<String?>(null)
    val navigateToSpNote: LiveData<String?>
        get() = _navigateToSpNote

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun updateUser(firebaseUser: FirebaseUser, account: GoogleSignInAccount) {

        // create a new user or update google account data to the existing user in users collection
        val userInfo = UserInfo(
            name = account.displayName!!,
            email = account.email!!,
            pic = account.photoUrl.toString()
        )

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.updateUser(firebaseUser, userInfo)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    updateLocalUserData()
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

    private fun updateLocalUserData() {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            when(val currentUserResult = repository.getCurrentUser()) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    navigate()
                }
                is Result.Fail -> {
                    _error.value = currentUserResult.error
                    _status.value = LoadApiStatus.ERROR
                    null
                }
                is Result.Error -> {
                    _error.value = currentUserResult.exception.toString()
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

    private fun navigate() {
        when(source) {
            Source.YOUTUBE.source -> _navigateToYtNote.value = sourceId
            Source.SPOTIFY.source -> _navigateToSpNote.value = sourceId
            null -> _navigateUp.value = true
        }
     }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun doneNavigation() {
        _navigateUp.value = false
        _navigateToYtNote.value = null
        _navigateToSpNote.value = null
    }
}