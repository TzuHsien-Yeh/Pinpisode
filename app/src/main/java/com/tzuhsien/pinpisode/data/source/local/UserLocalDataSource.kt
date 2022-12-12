package com.tzuhsien.pinpisode.data.source.local

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Guide
import com.tzuhsien.pinpisode.data.model.Invitation
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.UserDataSource
import timber.log.Timber

object UserLocalDataSource : UserDataSource {
    override suspend fun signInWithGoogle(credential: AuthCredential): Result<AuthResult> {
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

    override fun markNewUser(isNewUser: Boolean): Boolean {
        UserManager.isNewUser = isNewUser
        UserManager.shouldShowNoteListGuide = isNewUser

        Timber.d("isNewUser = ${UserManager.isNewUser},shouldShowNoteListGuide: ${UserManager.shouldShowNoteListGuide}")
        return true
    }

    override fun checkWhetherToShowNoteListGuide(): Boolean {
        Timber.d("UserManager.shouldShowNoteListGuide = ${UserManager.shouldShowNoteListGuide}")

        return UserManager.shouldShowNoteListGuide
    }

    override fun markGuideAsHasShown(guide: Guide): Boolean {
        when(guide) {
            Guide.NOTE_LIST -> UserManager.shouldShowNoteListGuide = false
            Guide.YOUTUBE_NOTE -> TODO()
            Guide.SPOTIFY_NOTE -> TODO()
        }
        return true
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