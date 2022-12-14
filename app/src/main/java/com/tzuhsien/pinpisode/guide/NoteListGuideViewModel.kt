package com.tzuhsien.pinpisode.guide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.data.model.Guide
import com.tzuhsien.pinpisode.data.source.Repository

class NoteListGuideViewModel(private val repository : Repository) : ViewModel() {

    var trickNumber = 1

    private val _isToShowWelcome = MutableLiveData(true)
    val isToShowWelcome: LiveData<Boolean>
        get() = _isToShowWelcome

    private val _isToShowAddNotes = MutableLiveData(false)
    val isToShowAddNotes: LiveData<Boolean>
        get() = _isToShowAddNotes

    private val _isToShowSearch = MutableLiveData(false)
    val isToShowSearch: LiveData<Boolean>
        get() = _isToShowSearch

    private val _isToShowHowToSort = MutableLiveData(false)
    val isToShowHowToSort: LiveData<Boolean>
        get() = _isToShowHowToSort

    private val _isToShowCoauthorInvitation = MutableLiveData(false)
    val isToShowCoauthorInvitation: LiveData<Boolean>
        get() = _isToShowCoauthorInvitation

    private val _isToShowQuit = MutableLiveData(false)
    val isToShowQuit: LiveData<Boolean>
        get() = _isToShowQuit

    private val _isToShowClosure = MutableLiveData(false)
    val isToShowClosure: LiveData<Boolean>
        get() = _isToShowClosure

    private val _leave = MutableLiveData(false)
    val leave: LiveData<Boolean>
        get() = _leave

    fun showNext() {
        when(trickNumber) {
            1 -> {
                trickNumber = 2
                showFirstTrick()
            }
            2 -> {
                trickNumber = 3
                showSecondTrick()
            }
            3 -> {
                trickNumber = 4
                showThirdTrick()
            }
            4 -> {
                trickNumber = 5
                showForthTrick()
            }
            5 -> {
                trickNumber = 6
                showFifthTrick()
            }
            6 -> {
                trickNumber = 7
                showSixthTrick()
            }
            7 -> {
                doneNoteListGuideForNewUser()
                dismissGuide()
            }
        }
    }

    private fun showFirstTrick() {
        _isToShowWelcome.value = false
        _isToShowAddNotes.value = true
    }

    private fun showSecondTrick() {
        _isToShowAddNotes.value = false
        _isToShowSearch.value = true
    }

    private fun showThirdTrick() {
        _isToShowSearch.value = false
        _isToShowHowToSort.value = true
    }

    private fun showForthTrick() {
        _isToShowHowToSort.value = false
        _isToShowCoauthorInvitation.value = true
    }

    private fun showFifthTrick() {
        _isToShowCoauthorInvitation.value = false
        _isToShowQuit.value = true
    }

    private fun showSixthTrick() {
        _isToShowQuit.value = false
        _isToShowClosure.value = true
    }

    private fun doneNoteListGuideForNewUser() {
        repository.markGuideAsHasShown(Guide.NOTE_LIST)
    }

    private fun dismissGuide() {
        _leave.value = true
    }

    fun doneDismissGuide() {
        _leave.value = false
    }

}
