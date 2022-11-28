package com.tzuhsien.pinpisode.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Invitation
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotificationViewModel(private val repository: Repository): ViewModel() {

    private var inviterMap = mutableMapOf<String, UserInfo?>()

    var invitationList = MutableLiveData<List<Invitation>>()

    private val idListAwaitQuery = mutableListOf<String>()

    private var _fullInvitationData = MutableLiveData<List<Invitation>>()
    val fullInvitationData: LiveData<List<Invitation>>
        get() = _fullInvitationData

    val uiState = NotificationUiState(
        onAcceptClicked = { item ->
            addInviteeToCoauthors(item)
        },
        onDeclineClicked = { item ->
            deleteInvitation(item)
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
    }

    fun getInvitersInfo(invitations: List<Invitation>){
        // Save a copy of inviter list from snapshot, put only those were not in the map list
        for (invitation in invitations){
            inviterMap.putIfAbsent(invitation.inviterId, null)
        }

        // update inviter info from inviterMap
        for (inv in invitations) {
            for (inviter in inviterMap){
                if (inviter.key == inv.inviterId) {
                    inv.inviter = inviter.value
                }
            }
        }

        // List those inviters without userInfo. prepare to query their info
        for(inv in invitations.filter { it.inviter == null }) {
            idListAwaitQuery.add(inv.inviterId)
        }

        // Limitation of Firebase whereIn query is for 10 userInfo at a time,
        // so chunk the list and query each of the chunked list
        val listForOneQuery = idListAwaitQuery.chunked(10)

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            for (list in listForOneQuery) {

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

            _fullInvitationData.value = invitations
            idListAwaitQuery.clear()
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

                    // delete invitation item once it's accepted and the invitee is successfully added to authors
                    deleteInvitation(item)
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

    private fun deleteInvitation(item: Invitation) {
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

    fun emptyFullInvitationData() {
        _fullInvitationData.value = listOf()
    }
}

data class NotificationUiState(
    val onAcceptClicked: (Invitation) -> Unit,
    val onDeclineClicked: (Invitation) -> Unit
    )