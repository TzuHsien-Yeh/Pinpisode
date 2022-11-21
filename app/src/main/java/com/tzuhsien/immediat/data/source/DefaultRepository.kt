package com.tzuhsien.immediat.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*

class DefaultRepository(private val noteRemoteDataSource: DataSource): Repository {

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        return noteRemoteDataSource.getAllLiveNotes()
    }

    override suspend fun getNoteInfoById(noteId: String): Result<Note> {
        return noteRemoteDataSource.getNoteInfoById(noteId)
    }

    override fun getLiveNoteById(noteId: String):  MutableLiveData<Note?>  {
        return noteRemoteDataSource.getLiveNoteById(noteId)
    }

    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        return noteRemoteDataSource.getYouTubeVideoInfoById(id)
    }

    override suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String): Result<Note?> {
        return noteRemoteDataSource.checkIfNoteAlreadyExists(source, sourceId)
    }

    override suspend fun createNote(source: String, sourceId: String, note: Note): Result<Note> {
        return noteRemoteDataSource.createNote(source, sourceId, note)
    }

    override suspend fun updateNoteInfoFromSourceApi(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateNoteInfoFromSourceApi(noteId, note)
    }

    override fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>> {
        return noteRemoteDataSource.getLiveTimeItems(noteId)
    }

    override suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String> {
        return noteRemoteDataSource.addNewTimeItem(noteId, timeItem)
    }

    override suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*> {
        return noteRemoteDataSource.updateTimeItem(noteId, timeItem)
    }

    override suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*> {
        return noteRemoteDataSource.deleteTimeItem(noteId, timeItem)
    }

    override suspend fun updateNote(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateNote(noteId, note)
    }

    override suspend fun updateTags(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateTags(noteId, note)
    }

    override suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo): Result<UserInfo> {
        return noteRemoteDataSource.updateUser(firebaseUser, user)
    }

    override suspend fun getCurrentUser(): Result<UserInfo?> {
        return noteRemoteDataSource.getCurrentUser()
    }

    override suspend fun findUserByEmail(query: String): Result<UserInfo?> {
        return noteRemoteDataSource.findUserByEmail(query)
    }

    override suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean> {
        return noteRemoteDataSource.updateNoteAuthors(noteId, authors)
    }

    override fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>> {
        return noteRemoteDataSource.getLiveCoauthorsInfoOfTheNote(note)
    }

    override suspend fun getUserInfoById(id: String): Result<UserInfo> {
        return noteRemoteDataSource.getUserInfoById(id)
    }

    /**  Coauthor invitation  **/
    override suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean> {
        return noteRemoteDataSource.sendCoauthorInvitation(note, inviteeId)
    }

    override fun getLiveIncomingCoauthorInvitations(): MutableLiveData<List<Invitation>> {
        return noteRemoteDataSource.getLiveIncomingCoauthorInvitations()
    }

    override suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>> {
        return noteRemoteDataSource.getUserInfoByIds(userIds)
    }

    override suspend fun deleteInvitation(invitationId: String): Result<Boolean> {
        return noteRemoteDataSource.deleteInvitation(invitationId)
    }

    override suspend fun deleteUserFromAuthors(noteId: String, authors: List<String>): Result<String> {
        return noteRemoteDataSource.deleteUserFromAuthors(noteId, authors)
    }

    override suspend fun deleteNote(noteId: String): Result<String> {
        return noteRemoteDataSource.deleteNote(noteId)
    }

    override suspend fun searchOnYouTube(query: String): Result<YouTubeSearchResult> {
        return noteRemoteDataSource.searchOnYouTube(query)
    }

    override suspend fun getTrendingVideosOnYouTube(): Result<YouTubeResult> {
        return noteRemoteDataSource.getTrendingVideosOnYouTube()
    }

    override suspend fun getSpotifyEpisodeInfo(id: String, authToken: String): Result<SpotifyItem> {
        return noteRemoteDataSource.getSpotifyEpisodeInfo(id, authToken)
    }

    override suspend fun searchOnSpotify(query: String, authToken: String): Result<SpotifySearchResult> {
        return noteRemoteDataSource.searchOnSpotify(query, authToken)
    }

    override suspend fun getUserSavedShows(authToken: String): Result<SpotifyShowResult> {
        return noteRemoteDataSource.getUserSavedShows(authToken)
    }

    override suspend fun getShowEpisodes(showId: String, authToken: String): Result<Episodes> {
        return noteRemoteDataSource.getShowEpisodes(showId, authToken)
    }


}