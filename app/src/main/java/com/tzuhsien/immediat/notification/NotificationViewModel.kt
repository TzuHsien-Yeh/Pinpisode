package com.tzuhsien.immediat.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Invitation
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationViewModel(private val repository: Repository): ViewModel() {

    private var inviterMap = mutableMapOf<String, UserInfo?>()

    var invitationList = MutableLiveData<List<Invitation>>()

    private val listAwaitQuery = mutableListOf<String>()

    private var _fullInvitationData = MutableLiveData<List<Invitation>>()
    val fullInvitationData: LiveData<List<Invitation>>
        get() = _fullInvitationData

    val uiState = NotificationUiState(
        onAcceptClicked = { item ->
            addInviteeToCoauthors(item)
        },
        onDeclineClicked = { item ->
            declineInvitation(item)
        }
    )

    private val _addSuccess = MutableLiveData<Boolean>(false)
    val addSuccess: LiveData<Boolean>
        get() = _addSuccess


    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


    init {
        getLiveIncomingCoauthorInvitations()
    }

    private fun getLiveIncomingCoauthorInvitations(){
        _status.value = LoadApiStatus.DONE
        invitationList = repository.getLiveIncomingCoauthorInvitations()

        invitationList.value?.let {
            for (invitation in it){
                inviterMap.putIfAbsent(invitation.inviterId, null)
            }

            Timber.d("inviterMap.putIfAbsent() result: $inviterMap")
        }

    }

    fun getInvitersInfo(invitations: List<Invitation>){

        for(inviter in inviterMap.filterValues { it == null }) {
            listAwaitQuery.add(inviter.key)
        }

        val listForOneQuery = listAwaitQuery.chunked(10)

        for (list in listForOneQuery) {
            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                when (val result = repository.getUserInfoByIds(list)) {

                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        // Add UserInfo to the userId in the map
                        for (inviterInfo in result.data) {
                            inviterMap.replace(inviterInfo.id, inviterInfo)
                        }

                        // mapping the livedata list with the result UserInfo map
                        for (user in inviterMap) {
                            for (inv in invitations) {
                                if (inv.inviterId == user.key) {
                                    inv.inviter = user.value
                                }
                            }
                        }
                        _fullInvitationData.value = invitations

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

    }

    private fun addInviteeToCoauthors(item: Invitation) {
        val authorSet = mutableSetOf<String>()
        authorSet.addAll(item.note.authors)
        authorSet.add(item.inviteeId)

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = repository.updateNoteAuthors(item.note.id, authorSet)

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

    private fun declineInvitation(item: Invitation) {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            when (val result = repository.deleteInvitation(item.id)) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE
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




}

data class NotificationUiState(
    val onAcceptClicked: (Invitation) -> Unit,
    val onDeclineClicked: (Invitation) -> Unit
    )