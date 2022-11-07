package com.tzuhsien.immediat.tag

import android.system.Os.remove
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import kotlinx.coroutines.*
import timber.log.Timber

class TagViewModel(private val repository: Repository, val note: Note): ViewModel() {

    var inputNewTag: String? = null

    val allTags = UserManager.tagSet
    val tagsOfCurrentNote = mutableSetOf<String>().also { it.addAll(note.tags) }

    private val _leave = MutableLiveData<Boolean>()
    val leave: LiveData<Boolean>
        get() = _leave

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun updateTagSet(tag: String, isChecked: Boolean){
        if (isChecked) {
            tagsOfCurrentNote.add(tag)
        } else {
            tagsOfCurrentNote.remove(tag)
        }
    }

    fun addNewTag() {
        inputNewTag?.let {
            tagsOfCurrentNote.add(it)
        }
    }

    fun saveChanges() {

        val newNote = note.also {
            it.tags = tagsOfCurrentNote.toList()
        }

        coroutineScope.launch {
            when (val result = repository.updateTags(note.id, newNote)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    _leave.value = true
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

    fun onLeaveCompleted() {
        _leave.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}