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

    private val _foundUser = MutableLiveData<UserInfo?>(null)
    val foundUser: LiveData<UserInfo?>
        get() = _foundUser

    private val _addSuccess = MutableLiveData<Boolean>(false)
    val addSuccess: LiveData<Boolean>
        get() = _addSuccess

    var errorMsg: String? = null

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

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

    fun addUserAsCoauthor() {
        val authorSet = mutableSetOf<String>()
        authorSet.addAll(note.authors)

        foundUser.value?.let {
            authorSet.add(it.id)
        }

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.updateNoteAuthors(note.id, authorSet)

            _addSuccess.value = when (result) {
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

}