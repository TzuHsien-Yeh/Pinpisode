package com.tzuhsien.immediat.search.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.LoadApiStatus
import com.tzuhsien.immediat.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchResultViewModel(private val repository: Repository) : ViewModel() {

    var source: Source? = null
    var queryKeyword: String? = null

    private val _needSpotifyAuth = MutableLiveData<Boolean>()
    val needSpotifyAuth: LiveData<Boolean>
        get() = _needSpotifyAuth

    private val _ytSearchResultList = MutableLiveData<List<ItemX>>()
    val ytSearchResultList: LiveData<List<ItemX>>
        get() = _ytSearchResultList

    private val _spotifySearchResultList = MutableLiveData<List<SpotifyItem>>()
    val spSearchResultList: LiveData<List<SpotifyItem>>
        get() = _spotifySearchResultList

    val uiState = SearchResultUiState(
        onYoutubeItemClick = { item ->
            _navigateToYoutubeNote.value = item.id.videoId
        },
        onSpotifyItemClick = { spotifyItem ->
            _navigateToSpotifyNote.value = spotifyItem.id
        }
    )

    // status: The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

    // error: The internal MutableLiveData that stores the error of the most recent request
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?>
        get() = _error

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _navigateToYoutubeNote = MutableLiveData<String?>()
    val navigateToYoutubeNote: LiveData<String?>
        get() = _navigateToYoutubeNote

    private val _navigateToSpotifyNote = MutableLiveData<String?>()
    val navigateToSpotifyNote: LiveData<String?>
        get() = _navigateToSpotifyNote

    init {
        when (source) {
            Source.YOUTUBE -> {
                searchOnYouTube()
            }
            Source.SPOTIFY -> {
                Timber.d("Source.SPOTIFY")
                if (UserManager.userSpotifyAuthToken.isEmpty()) {
                    _needSpotifyAuth.value = true
                } else {
                    searchOnSpotify()
                }
            }
            else -> {}
        }
    }

    fun search(query: String) {
        Timber.d("search()")

        when (source) {
            Source.YOUTUBE -> {
                queryKeyword = query
                searchOnYouTube()
            }
            Source.SPOTIFY -> {
                Timber.d("Source.SPOTIFY")
                if (UserManager.userSpotifyAuthToken.isEmpty()) {
                    _needSpotifyAuth.value = true
                } else {
                    queryKeyword = query
                    searchOnSpotify()
                }
            }
            else -> {}
        }

    }

    private fun searchOnYouTube() {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = queryKeyword?.let { repository.searchOnYouTube(it) }

            when (result) {
                is Result.Success -> {
                    _error.value = null
                    _status.value = LoadApiStatus.DONE

                    Timber.d("youtubeSearchResults: ${result.data}")

                    putYtResultToItemList(result.data)
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

    private fun putYtResultToItemList(ytResult: YouTubeSearchResult) {
        val list = mutableListOf<ItemX>()
        for (item in ytResult.items) {
            list.add(item)
        }

        _ytSearchResultList.value = list
    }

    fun searchOnSpotify() {

        Timber.d("searchOnSpotify")

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = queryKeyword?.let { repository.searchOnSpotify(it, authToken = UserManager.userSpotifyAuthToken) }

                when (result) {
                    is Result.Success -> {
                        _error.value = null
                        _status.value = LoadApiStatus.DONE

                        Timber.d("spotifySearchResults: ${result.data}")

                    putSpResultToItemList(result.data)

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

    private fun putSpResultToItemList(spResult: SpotifySearchResult) {
        val list = mutableListOf<SpotifyItem>()

        spResult.episodes?.let { episodes ->
            for (item in episodes.items) {
                list.add(item)
            }
        }

        spResult.tracks?.let { tracks ->
            for (item in tracks.items) {
                list.add(item)
            }
        }

        _spotifySearchResultList.value = list
    }

    fun doneRequestSpotifyAuthToken() {
        _needSpotifyAuth.value = false
    }

    fun doneNavigation() {
        _navigateToYoutubeNote.value = null
        _navigateToSpotifyNote.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}

data class SearchResultUiState(
    val onYoutubeItemClick: (ItemX) -> Unit,
    val onSpotifyItemClick: (SpotifyItem) -> Unit
)