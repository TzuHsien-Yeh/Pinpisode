package com.tzuhsien.pinpisode.search.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.util.Util
import kotlinx.coroutines.*
import timber.log.Timber

class SearchResultViewModel(private val repository: Repository) : ViewModel() {

    var queryKeyword: String? = null
    var source: Source? = null

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
            _navigateToSpotifyNote.value = spotifyItem.uri.extractSpotifySourceId()
        }
    )

    private val _status = MutableLiveData<LoadApiStatus>()
    val status: LiveData<LoadApiStatus>
        get() = _status

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

    fun getSpotifyAuthToken(): String? {
        return repository.getSpotifyAuthToken()
    }

    fun saveSpotifyAuthToken(authToken: String) {
        repository.setSpotifyAuthToken(authToken)
        _needSpotifyAuth.value = false
    }

    fun doneRequestSpotifyAuthToken() {
        _needSpotifyAuth.value = false
    }

    fun search(query: String?) {
        when (source) {
            Source.YOUTUBE -> {
                searchOnYouTube(query)
            }
            Source.SPOTIFY -> {
                if (null == getSpotifyAuthToken()) {
                    _needSpotifyAuth.value = true
                } else {
                    searchOnSpotify(query)
                }
            }
            else -> {}
        }
    }

    fun searchOnYouTube(query: String?) {

        coroutineScope.launch {

            _status.value = LoadApiStatus.LOADING

            val result = query?.let {
                withContext(Dispatchers.IO) {
                    repository.searchOnYouTube(it)
                }
            }

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

    fun searchOnSpotify(query: String?) {

        Timber.d("searchOnSpotify")
        if (null == getSpotifyAuthToken()) {

            _needSpotifyAuth.value = true

        } else {
            coroutineScope.launch {

                _status.value = LoadApiStatus.LOADING

                val result = queryKeyword?.let { query ->
                    withContext(Dispatchers.IO) {
                        repository.searchOnSpotify(
                            query = query,
                            authToken = getSpotifyAuthToken()!!
                        )
                    }
                }

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
                    is Result.SpotifyAuthError -> {
                        _needSpotifyAuth.value = result.expired
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

    fun emptySearchResultLists() {
        _ytSearchResultList.value = listOf()
        _spotifySearchResultList.value = listOf()
    }

    fun doneNavigation() {
        _navigateToYoutubeNote.value = null
        _navigateToSpotifyNote.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setViewPagerSource(s: Source) {
        source = s
    }
}

data class SearchResultUiState(
    val onYoutubeItemClick: (ItemX) -> Unit,
    val onSpotifyItemClick: (SpotifyItem) -> Unit,
)