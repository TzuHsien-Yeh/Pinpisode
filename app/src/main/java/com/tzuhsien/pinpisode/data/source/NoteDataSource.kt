package com.tzuhsien.pinpisode.data.source

import androidx.lifecycle.MutableLiveData
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*

interface NoteDataSource {

    fun getAllLiveNotes(): MutableLiveData<List<Note>>

    /**
     * Operation with the note (single source)
     * */
    suspend fun getNoteInfoById(noteId: String): Result<Note>

    fun getLiveNoteById(noteId: String): MutableLiveData<Note?>

    suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult>

    suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String, currentUser: UserInfo?): Result<Note?>

    suspend fun createNote(source: String, sourceId: String, note: Note): Result<Note>

    suspend fun updateNoteInfoFromSourceApi(noteId: String, note: Note): Result<String>

    fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>>

    suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String>

    suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun updateNote(noteId: String, note: Note): Result<String>

    suspend fun updateTags(noteId: String, note: Note): Result<String>

    suspend fun deleteNote(noteId: String): Result<String>

    /** Edit authors **/
    suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean>

    suspend fun deleteUserFromAuthors(noteId: String, authors: List<String>): Result<String>

    /**
     * Search or get Info from Yt & Sp
     * */
    suspend fun searchOnYouTube(query: String): Result<YouTubeSearchResult>

    suspend fun getTrendingVideosOnYouTube(): Result<YouTubeResult>

    suspend fun getSpotifyEpisodeInfo(id: String, authToken: String): Result<SpotifyItem>

    suspend fun searchOnSpotify(query: String, authToken: String): Result<SpotifySearchResult>

    suspend fun getUserSavedShows(authToken: String): Result<SpotifyShowResult>

    suspend fun getShowEpisodes(showId: String, authToken: String): Result<Episodes>
}