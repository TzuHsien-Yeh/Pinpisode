package com.tzuhsien.pinpisode.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Invitation
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.UserInfo

interface UserDataSource {
    /**
     * Sign in and update current user
     * */
    suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser>

    suspend fun updateCurrentUser(): Result<UserInfo?>

    suspend fun updateLocalUser(user: UserInfo?): Boolean

    fun getLocalCurrentUser(): UserInfo?

    fun setSpotifyAuthToken(authToken: String): Boolean

    fun getSpotifyAuthToken(): String?

    /**
     * Get user info
     * */
    suspend fun findUserByEmail(query: String): Result<UserInfo?>

    suspend fun getUserInfoById(id: String): Result<UserInfo>

    suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>>

    fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>>

    /**
     * Coauthor invitations
     * */
    suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean>

    fun getLiveIncomingCoauthorInvitations(currentUser: UserInfo?): MutableLiveData<List<Invitation>>

    suspend fun deleteInvitation(invitationId: String): Result<Boolean>

}