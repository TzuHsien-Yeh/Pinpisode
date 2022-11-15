package com.tzuhsien.immediat.coauthor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CoauthorViewModel(private val repository: Repository, val note: Note) : ViewModel() {

    private val _foundUser = MutableLiveData<UserInfo?>()
    val foundUser: LiveData<UserInfo?>
        get() = _foundUser

    private val _isInviteSuccess = MutableLiveData<Boolean>(false)
    val isInviteSuccess: LiveData<Boolean>
        get() = _isInviteSuccess

    var errorMsg: String? = null

    private val _noteOwner = MutableLiveData<UserInfo>()
    val noteOwner: LiveData<UserInfo>
            get() = _noteOwner

    private var _liveCoauthorInfo = MutableLiveData<List<UserInfo>>()
    val liveCoauthorInfo: LiveData<List<UserInfo>>
        get() = _liveCoauthorInfo

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        getNoteOwner()
        getLiveAuthorsOfTheNote()
    }

    private fun getNoteOwner() {

        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            val result = repository.getUserInfoById(note.ownerId)

            _noteOwner.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    errorMsg = result.error
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

    private fun getLiveAuthorsOfTheNote() {
        _liveCoauthorInfo = repository.getLiveCoauthorsInfoOfTheNote(note)
    }

    fun findUserByEmail(query: String) {
        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            val result = repository.findUserByEmail(query)

            _foundUser.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    errorMsg = result.error
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

    fun sendCoauthorInvitation(){
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = foundUser.value?.let { repository.sendCoauthorInvitation(note, it.id) }

            _isInviteSuccess.value = when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
                    result.data
                }
                is Result.Fail -> {
                    _error.value = result.error
                    _status.value = LoadApiStatus.ERROR
                    false
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
                    _status.value = LoadApiStatus.ERROR
                    false
                }
                else -> {
                    _error.value = Util.getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    false
                }
            }
        }
    }

    fun resetFoundUser() {
        _foundUser.value = null
    }

}