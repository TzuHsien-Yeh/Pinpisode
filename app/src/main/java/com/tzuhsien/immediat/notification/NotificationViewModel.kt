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

    private val idListAwaitQuery = mutableListOf<String>()

    private var listToSubmit = mutableListOf<Invitation>()

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

        listToSubmit.addAll(invitations)

        // Save a copy of inviter list from snapshot
        for (invitation in invitations){
            inviterMap.putIfAbsent(invitation.inviterId, null)
        }
        Timber.d("inviterMap.putIfAbsent() result: $inviterMap")

        // List those inviters without userInfo. prepare to query their info
        for(inviter in inviterMap.filterValues { it == null }) {
            idListAwaitQuery.add(inviter.key)
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
                            for (inv in listToSubmit) {
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

            _fullInvitationData.value = listToSubmit
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