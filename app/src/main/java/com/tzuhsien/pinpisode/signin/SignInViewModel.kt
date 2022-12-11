package com.tzuhsien.pinpisode.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SignInViewModel(private val repository: Repository) : ViewModel() {

    companion object {
        const val GOOGLE_SIGN_IN = 1903
    }

    var source: String? = null
    var sourceId: String? = null

    // Build a GoogleSignInClient with the options specified by gso.
    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(MyApplication.applicationContext(), getGSO())

    private val _msg = MutableLiveData<String>(null)
    val msg: LiveData<String>
        get() = _msg

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

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Timber.d("Google Sign in exception: ${e.statusCode}")
            _msg.value = getString(R.string.sign_in_failed)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        coroutineScope.launch {

            when (val result = repository.signInWithGoogle(credential)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    updateUser()
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    _msg.value = getString(R.string.sign_in_failed)
                    Timber.d("firebaseAuthWithGoogle: Error")
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    Timber.d("firebaseAuthWithGoogle: result.fail: ${result.error}")
                    _msg.value = getString(R.string.sign_in_failed)
                }
                else -> {
                    Timber.d("firebaseAuthWithGoogle else")
                    _msg.value = getString(R.string.sign_in_failed)
                }
            }
        }
    }

    private fun getGSO(): GoogleSignInOptions {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }


    private fun updateUser() {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.updateCurrentUser()) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    navigate()
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    _msg.value = getString(R.string.sign_in_failed)
                    Timber.d("updateUser fail")
                }
                else -> {
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    _msg.value = getString(R.string.sign_in_failed)

                    Timber.d("updateUser else")
                }
            }
        }
    }

    private fun navigate() {
        when (source) {
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