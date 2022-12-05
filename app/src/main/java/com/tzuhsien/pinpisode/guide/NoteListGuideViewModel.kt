package com.tzuhsien.pinpisode.guide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NoteListGuideViewModel : ViewModel() {

    var trickNumber = 1

    private val _isToShowWelcome = MutableLiveData<Boolean>(true)
    val isToShowWelcome: LiveData<Boolean>
        get() = _isToShowWelcome

    private val _isToShowAddNotes = MutableLiveData<Boolean>(false)
    val isToShowAddNotes: LiveData<Boolean>
        get() = _isToShowAddNotes

    private val _isToShowSearch = MutableLiveData<Boolean>(false)
    val isToShowSearch: LiveData<Boolean>
        get() = _isToShowSearch

    private val _isToShowHowToSort = MutableLiveData<Boolean>(false)
    val isToShowHowToSort: LiveData<Boolean>
        get() = _isToShowHowToSort

    private val _isToShowCoauthorInvitation = MutableLiveData<Boolean>(false)
    val isToShowCoauthorInvitation: LiveData<Boolean>
        get() = _isToShowCoauthorInvitation

    private val _isToShowQuit = MutableLiveData<Boolean>(false)
    val isToShowQuit: LiveData<Boolean>
        get() = _isToShowQuit

    private val _isToShowClosure = MutableLiveData<Boolean>(false)
    val isToShowClosure: LiveData<Boolean>
        get() = _isToShowClosure

    private val _leave = MutableLiveData<Boolean>(false)
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
                dismissGuide()
            }
        }
    }

    private fun dismissGuide() {
        _leave.value = true
    }

    fun doneDismissGuide() {
        _leave.value = false
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

}
