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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SignInViewModel(private val repository: Repository): ViewModel() {

    companion object {
        const val GOOGLE_SIGN_IN = 1903
    }

    var source: String? = null
    var sourceId: String? = null

    // Build a GoogleSignInClient with the options specified by gso.
    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(MyApplication.applicationContext(), getGSO())
    var auth: FirebaseAuth = Firebase.auth

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

//    init {
//        getGoogleSignedInAccount()
//    }
//
//    private fun getGoogleSignedInAccount() {
//        val account = GoogleSignIn.getLastSignedInAccount(MyApplication.applicationContext())
//        if (null != account) navigate()
//    }

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
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    UserManager.isNewUser = task.result.additionalUserInfo?.isNewUser ?: false
                    task.result.user?.let { updateUser(it, account) }
                } else {
                    //handle error
                    Timber.d("firebaseAuthWithGoogle: Failed!")
                }
            }
    }

    private fun getGSO(): GoogleSignInOptions {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        return  GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }


    fun updateUser(firebaseUser: FirebaseUser, account: GoogleSignInAccount) {

        // create a new user or update google account data to the existing user in users collection
        val userInfo = UserInfo(
            name = account.displayName ?: getString(R.string.unknown_user_name),
            email = account.email ?: getString(R.string.unknown_email),
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
                    _error.value = getString(R.string.unknown_error)
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
                    _error.value = getString(R.string.unknown_error)
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