package com.tzuhsien.pinpisode.tag

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.*

class TagViewModel(private val repository: Repository, val note: Note) : ViewModel() {

    var hasDrawnTags: Boolean = false
    var inputNewTag: String? = null

    val set = mutableSetOf<String>()
    val allTags = Transformations.map(repository.getAllLiveNotes()) {
        for (note in it) {
            for (tag in note.tags) {
                set.add(tag)
            }
        }
        set
    }

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

    fun doneDrawingTags() {
        hasDrawnTags = true
    }

    fun updateTagSet(tag: String, isChecked: Boolean) {
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

            val result = withContext(Dispatchers.IO) {
                repository.updateTags(note.id, newNote)
            }

            when (result) {
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
                    _error.value = Util.getString(R.string.unknown_error)
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