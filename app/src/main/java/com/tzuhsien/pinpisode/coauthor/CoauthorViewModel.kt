package com.tzuhsien.pinpisode.coauthor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CoauthorViewModel(private val repository: Repository, val note: Note) : ViewModel() {

    private val _noteOwner = MutableLiveData<UserInfo>()
    val noteOwner: LiveData<UserInfo>
        get() = _noteOwner

    val isUserTheOwner = note.ownerId == repository.getCurrentUser()?.id

    private val _foundUser = MutableLiveData<UserInfo?>()
    val foundUser: LiveData<UserInfo?>
        get() = _foundUser

    private val _resultMsg = MutableLiveData<String?>(null)
    val resultMsg: LiveData<String?>
        get() = _resultMsg

    private val _quitCoauthoringResult = MutableLiveData<String>(null)
    val quitCoauthoringResult: LiveData<String>
        get() = _quitCoauthoringResult

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
                    null
                }
                is Result.Error -> {
                    _error.value = result.exception.toString()
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

                    if (null == result.data) {
                        _resultMsg.value = getString(R.string.user_not_found)
                    }

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
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }
    }

    fun sendCoauthorInvitation() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result =
                foundUser.value?.let { repository.sendCoauthorInvitation(note, it.id) }) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    resetFoundUser()
                    _resultMsg.value = getString(R.string.coauthor_invitation_sent)
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
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                }
            }
        }
    }

    private fun resetFoundUser() {
        _foundUser.value = null
    }

    fun quitCoauthoringTheNote() {
        val newAuthorList = mutableListOf<String>()
        newAuthorList.addAll(note.authors)
        newAuthorList.remove(repository.getCurrentUser()?.id)

        coroutineScope.launch {
            _status.value = LoadApiStatus.LOADING

            _quitCoauthoringResult.value = when (val result = repository.deleteUserFromAuthors(
                noteId = note.id,
                authors = newAuthorList
            )) {
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
                    _error.value = getString(R.string.unknown_error)
                    _status.value = LoadApiStatus.ERROR
                    null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}