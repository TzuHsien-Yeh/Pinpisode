package com.tzuhsien.pinpisode.data.source.local

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Invitation
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.UserDataSource
import timber.log.Timber

object UserLocalDataSource : UserDataSource {
    override suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser> {
        TODO("Not yet implemented")
    }

    override suspend fun updateLocalUser(user: UserInfo?): Boolean {
        Timber.d("updateLocalUser")
        UserManager.userId = user?.id
        UserManager.userEmail = user?.email
        UserManager.userName = user?.name
        UserManager.userPic = user?.pic
        return true
    }

    override fun getLocalCurrentUser(): UserInfo? {
        return if (null == UserManager.userId || null == UserManager.userEmail || null == UserManager.userName) {
            null
        } else {
            UserInfo(id = UserManager.userId!!, email = UserManager.userEmail!!, name = UserManager.userName!!, pic = UserManager.userPic)
        }
    }

    override fun setSpotifyAuthToken(authToken: String): Boolean {
        UserManager.userSpotifyAuthToken = authToken
        return true
    }

    override fun getSpotifyAuthToken(): String? {
        return UserManager.userSpotifyAuthToken
    }

    override suspend fun updateCurrentUser(): Result<UserInfo?> {
        TODO("Not yet implemented")
    }

    override suspend fun findUserByEmail(query: String): Result<UserInfo?> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserInfoById(id: String): Result<UserInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>> {
        TODO("Not yet implemented")
    }

    override fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>> {
        TODO("Not yet implemented")
    }

    override suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getLiveIncomingCoauthorInvitations(currentUser: UserInfo?): MutableLiveData<List<Invitation>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteInvitation(invitationId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

}