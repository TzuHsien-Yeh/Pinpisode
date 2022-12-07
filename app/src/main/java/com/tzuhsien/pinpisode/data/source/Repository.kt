package com.tzuhsien.pinpisode.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*

interface Repository {
    /**
     *  Pages that show a whole list of notes
     * */
    fun getAllLiveNotes(): MutableLiveData<List<Note>>

    /**
     *  For the note (single source)
     */
    suspend fun getNoteInfoById(noteId: String): Result<Note>

    fun getLiveNoteById(noteId: String): MutableLiveData<Note?>

    suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult>

    suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String): Result<Note?>

    suspend fun createNote(source: String, sourceId: String, note: Note): Result<Note>

    suspend fun updateNoteInfoFromSourceApi(noteId: String, note: Note): Result<String>

    fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>>

    suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String>

    suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun updateNote(noteId: String, note: Note): Result<String>

    suspend fun updateTags(noteId: String, note: Note): Result<String>

    /**
     *  Users
     * */
    suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo) : Result<UserInfo>

    suspend fun getCurrentUser(): Result<UserInfo?>

    suspend fun findUserByEmail(query: String): Result<UserInfo?>

    suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean>

    fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>>

    suspend fun getUserInfoById(id: String): Result<UserInfo>

    /**
     * Coauthor invitation
     * */
    suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean>

    fun getLiveIncomingCoauthorInvitations(): MutableLiveData<List<Invitation>>

    suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>>

    suspend fun deleteInvitation(invitationId: String): Result<Boolean>

    suspend fun deleteUserFromAuthors(noteId: String, authors: MutableList<String>): Result<String>

    suspend fun deleteNote(noteId: String): Result<String>

    /**
     * Search api
     * */
    suspend fun searchOnYouTube(query: String): Result<YouTubeSearchResult>

    suspend fun getTrendingVideosOnYouTube(): Result<YouTubeResult>

    suspend fun getSpotifyEpisodeInfo(id: String, authToken: String): Result<SpotifyItem>

    suspend fun searchOnSpotify(query: String, authToken: String): Result<SpotifySearchResult>

    suspend fun getUserSavedShows(authToken: String): Result<SpotifyShowResult>

    suspend fun getShowEpisodes(showId: String, authToken: String): Result<Episodes>
}