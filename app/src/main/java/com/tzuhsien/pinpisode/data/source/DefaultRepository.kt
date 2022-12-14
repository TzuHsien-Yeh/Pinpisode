package com.tzuhsien.pinpisode.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*
import com.tzuhsien.pinpisode.util.Util.getString
import timber.log.Timber

class DefaultRepository(
    private val noteRemoteDataSource: NoteDataSource,
    private val userRemoteDataSource: UserDataSource,
    private val userLocalDataSource: UserDataSource,
) : Repository {

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        return noteRemoteDataSource.getAllLiveNotes()
    }

    override suspend fun getNoteInfoById(noteId: String): Result<Note> {
        return noteRemoteDataSource.getNoteInfoById(noteId)
    }

    override fun getLiveNoteById(noteId: String): MutableLiveData<Note?> {
        return noteRemoteDataSource.getLiveNoteById(noteId)
    }

    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        return noteRemoteDataSource.getYouTubeVideoInfoById(id)
    }

    override suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String): Result<Note?> {
        return noteRemoteDataSource.checkIfNoteAlreadyExists(source, sourceId, getCurrentUser())
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

    /**
     *  Users
     * */
    override suspend fun signInWithGoogle(credential: AuthCredential): Result<AuthResult> {
        return userRemoteDataSource.signInWithGoogle(credential)
    }

    override suspend fun updateCurrentUser(): Result<UserInfo?> {
        // Create or update the current signed-in user to users collection
        return when (val userResult = userRemoteDataSource.updateCurrentUser()) {
            is Result.Success -> {
                if (userLocalDataSource.updateLocalUser(userResult.data)) {
                    Result.Success(getCurrentUser())
                } else {
                    Timber.d("Error updating local user")
                    Result.Fail("Error updating local user")
                }
            }
            is Result.Fail -> {
                Timber.d(userResult.error)
                Result.Fail(userResult.error)
            }
            is Result.Error -> {
                Timber.d("[Error]userRemoteDataSource.getCurrentUser: ${userResult.exception}")
                Result.Error(userResult.exception)
            }
            else -> {
                Timber.d(getString(R.string.unknown_error))
                Result.Fail(getString(R.string.unknown_error))
            }
        }
    }

    override fun getCurrentUser(): UserInfo? {
        return userLocalDataSource.getLocalCurrentUser()
    }

    override fun markNewUser(isNewUser: Boolean): Boolean {
        return userLocalDataSource.markNewUser(isNewUser)
    }

    override fun checkWhetherToShowNoteListGuide(): Boolean {
        return userLocalDataSource.checkWhetherToShowNoteListGuide()
    }

    override fun markGuideAsHasShown(guide: Guide): Boolean {
        return userLocalDataSource.markGuideAsHasShown(guide)
    }

    override fun setSpotifyAuthToken(authToken: String): Boolean {
        return userLocalDataSource.setSpotifyAuthToken(authToken)
    }

    override fun getSpotifyAuthToken(): String? {
        return userLocalDataSource.getSpotifyAuthToken()
    }

    override suspend fun findUserByEmail(query: String): Result<UserInfo?> {
        return userRemoteDataSource.findUserByEmail(query)
    }

    override suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean> {
        return noteRemoteDataSource.updateNoteAuthors(noteId, authors)
    }

    override fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>> {
        return userRemoteDataSource.getLiveCoauthorsInfoOfTheNote(note)
    }

    override suspend fun getUserInfoById(id: String): Result<UserInfo> {
        return userRemoteDataSource.getUserInfoById(id)
    }

    /**  Coauthor invitation  **/
    override suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean> {
        return userRemoteDataSource.sendCoauthorInvitation(note, inviteeId)
    }

    override fun getLiveIncomingCoauthorInvitations(): MutableLiveData<List<Invitation>> {
        return userRemoteDataSource.getLiveIncomingCoauthorInvitations(getCurrentUser())
    }

    override suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>> {
        return userRemoteDataSource.getUserInfoByIds(userIds)
    }

    override suspend fun deleteInvitation(invitationId: String): Result<Boolean> {
        return userRemoteDataSource.deleteInvitation(invitationId)
    }

    override suspend fun deleteUserFromAuthors(
        noteId: String,
        authors: MutableList<String>,
    ): Result<String> {
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

    override suspend fun searchOnSpotify(
        query: String,
        authToken: String,
    ): Result<SpotifySearchResult> {
        return noteRemoteDataSource.searchOnSpotify(query, authToken)
    }

    override suspend fun getUserSavedShows(authToken: String): Result<SpotifyShowResult> {
        return noteRemoteDataSource.getUserSavedShows(authToken)
    }

    override suspend fun getShowEpisodes(showId: String, authToken: String): Result<Episodes> {
        return noteRemoteDataSource.getShowEpisodes(showId, authToken)
    }
}