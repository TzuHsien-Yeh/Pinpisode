package com.tzuhsien.immediat.spotifynote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.LoadApiStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val SPOTIFY_URI = "spotify:"

class SpotifyNoteViewModel(
    private val repository: Repository,
    var noteId: String?,
    var sourceId: String,
) : ViewModel() {

    var playingState = PlayingState.STOPPED

    private val _isSpotifyConnected = MutableLiveData<Boolean>(false)
    val isSpotifyConnected: LiveData<Boolean>
        get() = _isSpotifyConnected

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        connectToSpotify()
    }

    private fun connectToSpotify() {
        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val connectResult = SpotifyService.connectToAppRemote()

            if (connectResult.isConnected) {
                _status.value = LoadApiStatus.DONE
                _isSpotifyConnected.value = true
            } else {
                _error.value = "Spotify is not connected"
            }

        }
    }

}