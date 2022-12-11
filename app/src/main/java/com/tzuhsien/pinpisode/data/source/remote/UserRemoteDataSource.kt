package com.tzuhsien.pinpisode.data.source.remote

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.Invitation
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.UserDataSource
import com.tzuhsien.pinpisode.util.Util.getString
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object UserRemoteDataSource : UserDataSource {

    // Firebase
    private const val PATH_USERS = "users"
    private const val PATH_INVITATIONS = "invitations"
    private const val KEY_EMAIL = "email"
    private const val KEY_ID = "id"
    private const val KEY_TIME = "time"
    private const val KEY_INVITEE_ID = "inviteeId"

    override suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser> =
        suspendCoroutine { continuation ->

            Firebase.auth
                .signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (null != task.result.user) {

                        Timber.d("Firebase.auth.currentUser.email: ${Firebase.auth.currentUser?.email}")
                        Timber.d("Firebase.auth.currentUser.uid: ${Firebase.auth.currentUser?.uid}")
                        Timber.d("Firebase.auth.currentUser.displayName: ${Firebase.auth.currentUser?.displayName}")
                        Timber.d("Firebase.auth.currentUser.profilePic: ${Firebase.auth.currentUser?.photoUrl}")

                        continuation.resume(Result.Success(task.result.user!!))
                    } else {
                        continuation.resume(Result.Fail(getString(R.string.no_signed_in_firebase_user)))
                    }
                }
                .addOnFailureListener {
                    continuation.resume(Result.Error(it))
                }
        }

    override suspend fun updateCurrentUser(): Result<UserInfo?>  = suspendCoroutine { continuation ->

        val currentUser = Firebase.auth.currentUser
        Timber.d("updateCurrentUser: $currentUser")

        if (null == currentUser) {

            continuation.resume(Result.Success(null))

        } else {

            val doc = Firebase.firestore.collection(PATH_USERS).document(currentUser.uid)

            val user = UserInfo(
                id = currentUser.uid,
                name = currentUser.displayName ?: getString(R.string.unknown_user_name),
                email = currentUser.email ?: getString(R.string.unknown_email),
                pic = currentUser.photoUrl.toString()
            )

            doc
                .set(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(user))
                    } else {
                        task.exception?.let {
                            Timber.w("Error getting user document. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }
    }

    override suspend fun updateLocalUser(user: UserInfo?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLocalCurrentUser(): UserInfo? {
        TODO("Not yet implemented")
    }

    override fun setSpotifyAuthToken(authToken: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSpotifyAuthToken(): String? {
        TODO("Not yet implemented")
    }

    override suspend fun findUserByEmail(query: String): Result<UserInfo?> =
        suspendCoroutine { continuation ->

            val user = FirebaseFirestore.getInstance().collection(PATH_USERS)

            user
                .whereEqualTo(KEY_EMAIL, query)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val querySnapshot: QuerySnapshot? = task.result
                        if (querySnapshot!!.isEmpty) {
                            continuation.resume(Result.Success(null))
                        } else {
                            val result = mutableListOf<UserInfo>()
                            for (u in task.result) {
                                val userInfo = u.toObject(UserInfo::class.java)
                                result.add(userInfo)
                            }

                            if (result.isEmpty()) {
                                continuation.resume(Result.Fail(getString(R.string.user_not_found)))
                            } else {
                                continuation.resume(Result.Success(result[0]))
                            }
                        }

                    } else {
                        continuation.resume(Result.Fail(MyApplication.instance.getString(
                            R.string.unknown_error)))
                    }

                }
        }

    override suspend fun getUserInfoById(id: String): Result<UserInfo> =
        suspendCoroutine { continuation ->

            FirebaseFirestore.getInstance()
                .collection(PATH_USERS)
                .document(id)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result.toObject(UserInfo::class.java)
                        Timber.d("$user")
                        continuation.resume(Result.Success(user!!))
                    } else {
                        task.exception?.let {
                            Timber.w("Error getting document. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

        }

    override suspend fun getUserInfoByIds(userIds: List<String>): Result<List<UserInfo>> =
        suspendCoroutine { continuation ->

            FirebaseFirestore.getInstance().collection(PATH_USERS)
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val userInfoList = mutableListOf<UserInfo>()
                        for (doc in task.result) {
                            val user = doc.toObject(UserInfo::class.java)
                            userInfoList.add(user)
                        }

                        continuation.resume(Result.Success(userInfoList))

                    } else {
                        task.exception?.let {
                            Timber.w("Error getting document. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override fun getLiveCoauthorsInfoOfTheNote(note: Note): MutableLiveData<List<UserInfo>> {
        val liveData = MutableLiveData<List<UserInfo>>()

        FirebaseFirestore.getInstance()
            .collection(PATH_USERS)
            .whereIn(KEY_ID, note.authors)
            .whereNotEqualTo(KEY_ID, note.ownerId)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("Error getting documents. ${it.message}")
                }

                val list = mutableListOf<UserInfo>()
                if (snapshot != null) {
                    for (doc in snapshot) {
                        Timber.d(doc.id + " => " + doc.data)
                        val user = doc.toObject(UserInfo::class.java)
                        list.add(user)
                    }
                }
                liveData.value = list
            }
        return liveData
    }

    override suspend fun sendCoauthorInvitation(note: Note, inviteeId: String): Result<Boolean> =
        suspendCoroutine { continuation ->

            val doc = FirebaseFirestore.getInstance().collection(PATH_INVITATIONS).document()

            val newInvitation = Invitation(
                id = doc.id,
                note = note,
                inviterId = note.ownerId,
                inviteeId = inviteeId,
                time = Calendar.getInstance().timeInMillis
            )

            doc
                .set(newInvitation)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.w("Error adding documents. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

        }

    override fun getLiveIncomingCoauthorInvitations(currentUser: UserInfo?): MutableLiveData<List<Invitation>> {

        val liveData = MutableLiveData<List<Invitation>>()

        FirebaseFirestore.getInstance()
            .collection(PATH_INVITATIONS)
            .whereEqualTo(KEY_INVITEE_ID, currentUser?.id)
            .orderBy(KEY_TIME, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("Error getting documents. ${it.message}")
                }

                val list = mutableListOf<Invitation>()
                for (doc in snapshot!!) {
                    Timber.d(doc.id + " => " + doc.data)
                    val invite = doc.toObject(Invitation::class.java)
                    list.add(invite)
                }

                liveData.value = list
            }
        return liveData

    }

    override suspend fun deleteInvitation(invitationId: String): Result<Boolean> =
        suspendCoroutine { continuation ->

            FirebaseFirestore.getInstance().collection(PATH_INVITATIONS)
                .document(invitationId)
                .delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.w("Error adding documents. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }
}