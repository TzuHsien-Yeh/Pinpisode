package com.tzuhsien.immediat.data.source.remote

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.DataSource
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.network.SpotifyApi
import com.tzuhsien.immediat.network.YouTubeApi
import com.tzuhsien.immediat.util.Util.getString
import com.tzuhsien.immediat.util.Util.isInternetConnected
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object NoteRemoteDataSource : DataSource {

    // Firebase
    private const val PATH_NOTES = "notes"
    private const val PATH_USERS = "users"
    private const val PATH_INVITATIONS = "invitations"
    private const val PATH_TIME_ITEMS = "timeItems"

    private const val KEY_START_AT = "startAt" // for orderBy()
    private const val KEY_LAST_EDIT_TIME = "lastEditTime" // for orderBy()
    private const val KEY_AUTHORS = "authors"
    private const val KEY_SOURCE = "source"
    private const val KEY_SOURCE_ID = "sourceId"
    private const val KEY_OWNER_ID = "ownerId"
    private const val KEY_LAST_TIME_STAMP = "lastTimestamp"
    private const val KEY_DIGEST = "digest"

    // Youtube
    private const val YT_VIDEO_PARAM_PART = "snippet, contentDetails"
    private const val YT_SEARCH_PARAM_PART = "snippet"
    private const val YT_SEARCH_PARAM_TYPE = "video"
    private const val YT_VIDEO_PARAM_CHART = "mostPopular"

    // Spotify
    private const val SPOTIFY_BEARER = "Bearer "
    private const val SPOTIFY_PARAM_TYPE = "episode, track"

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        val liveData = MutableLiveData<List<Note>>()

        UserManager.userId?.let {
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .whereArrayContains(KEY_AUTHORS, it)
                .orderBy(KEY_LAST_EDIT_TIME, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    Timber.i("addSnapshotListener detect")

                    error?.let {
                        Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}")
                    }

                    val list = mutableListOf<Note>()
                    if (snapshot != null) {
                        for (doc in snapshot) {
                            Timber.d(doc.id + " => " + doc.data)
                            val noteItem = doc.toObject(Note::class.java)
                            list.add(noteItem)
                        }
                    }

                    UserManager.allEditableNoteList = list
                    UserManager.usersNoteList = list.filter { it.ownerId == UserManager.userId }
                    fun getAllTags(): MutableSet<String> {
                        val set = mutableSetOf<String>()
                        for (note in list) {
                            for (tag in note.tags) {
                                set.add(tag)
                            }
                        }
                        return set
                    }
                    UserManager.tagSet = getAllTags()

                    liveData.value = list
                }
        }
        return liveData
    }

    override suspend fun getNoteInfoById(noteId: String): Result<Note> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val note = task.result.toObject(Note::class.java)
                        continuation.resume(Result.Success(note!!))
                    } else {
                        task.exception?.let {
                            Timber.w("Error getting documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

        }


    override fun getLiveNoteById(noteId: String): MutableLiveData<Note?> {

        val liveData = MutableLiveData<Note?>()

        FirebaseFirestore.getInstance()
            .collection(PATH_NOTES)
            .document(noteId)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("Error getting documents. ${it.message}")
                }

                val note = snapshot!!.toObject(Note::class.java)
                liveData.value = note
            }
        return liveData
    }


    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            // this will run on a thread managed by Retrofit
            val listResult = YouTubeApi.retrofitService.getVideoInfo(YT_VIDEO_PARAM_PART, id)

            if (listResult.items.isEmpty()) {
                return Result.Fail(getString(R.string.video_not_available))
            }
            Result.Success(listResult)
        } catch (e: Exception) {
            Timber.w("[${this::class.simpleName}: getYouTubeVideoInfoById] exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String): Result<Note?> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)

            notes
                .whereEqualTo(KEY_SOURCE, source)
                .whereEqualTo(KEY_SOURCE_ID, sourceId)
                .whereEqualTo(KEY_OWNER_ID, UserManager.userId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val querySnapshot: QuerySnapshot? = task.result
                        if (querySnapshot!!.isEmpty) {
                            continuation.resume(Result.Success(null))
                        } else {
                            val result = mutableListOf<Note>()
                            for (note in task.result) {
                                val item = note.toObject(Note::class.java)
                                result.add(item)
                            }
                            continuation.resume(Result.Success(result[0]))
                        }

                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error finding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(
                            R.string.unknown_error)))
                    }

                }
        }

    override suspend fun createNote(source: String, sourceId: String, note: Note): Result<Note> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
            val doc = notes.document()

            note.id = doc.id

            note.source = source

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .set(note)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(note))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(
                            R.string.unknown_error)))
                    }

                }
        }

    override suspend fun updateNoteInfoFromSourceApi(noteId: String, note: Note): Result<String> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
            val doc = notes.document(noteId)

            doc
                .update(
                    "duration", note.duration,
                    "title", note.title,
                    "thumbnail", note.thumbnail
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(doc.id))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(
                            R.string.unknown_error)))
                    }

                }
        }


    override fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>> {
        val liveData = MutableLiveData<List<TimeItem>>()

        FirebaseFirestore.getInstance()
            .collection(PATH_NOTES)
            .document(noteId)
            .collection(PATH_TIME_ITEMS)
            .orderBy(KEY_START_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}")
                }

                val list = mutableListOf<TimeItem>()
                for (doc in snapshot!!) {
                    Timber.d(doc.id + " => " + doc.data)
                    val timeItem = doc.toObject(TimeItem::class.java)
                    list.add(timeItem)
                }

                liveData.value = list
            }
        return liveData
    }

    override suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String> =
        suspendCoroutine { continuation ->
            val noteRef = FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)

            noteRef.update(KEY_LAST_EDIT_TIME, Calendar.getInstance().timeInMillis)

            val doc = noteRef
                .collection(PATH_TIME_ITEMS)
                .document()

            timeItem.id = doc.id

            doc
                .set(timeItem)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(doc.id))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

        }

    override suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*> =
        suspendCoroutine { continuation ->
            val noteRef = FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)

            noteRef.update(KEY_LAST_EDIT_TIME, Calendar.getInstance().timeInMillis)

            val doc = noteRef
                .collection(PATH_TIME_ITEMS)
                .document(timeItem.id)

            doc
                .update("title", timeItem.title, "text", timeItem.text)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(0))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*> =
        suspendCoroutine { continuation ->
            val noteRef = FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)

            noteRef.update(KEY_LAST_EDIT_TIME, Calendar.getInstance().timeInMillis)

            val doc = noteRef
                .collection(PATH_TIME_ITEMS)
                .document(timeItem.id)

            doc
                .delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(0))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun updateNote(noteId: String, note: Note): Result<String> =
        suspendCoroutine { continuation ->
            val doc = FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .update(
                    KEY_DIGEST, note.digest,
                    KEY_LAST_TIME_STAMP, note.lastTimestamp,
                    KEY_LAST_EDIT_TIME, note.lastEditTime
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("updateNote: digest = ${note.digest}")
                        continuation.resume(Result.Success("Updated"))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun updateTags(noteId: String, note: Note): Result<String> =
        suspendCoroutine { continuation ->
            val doc = FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .update(
                    "tags", note.tags,
                    "lastEditTime", note.lastEditTime
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(""))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }


    override suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo): Result<UserInfo> =
        suspendCoroutine { continuation ->
            // Create a new user (or update info) of current signed in google account to users collection
            val doc =
                FirebaseFirestore.getInstance().collection(PATH_USERS).document(firebaseUser.uid)

            user.id = firebaseUser.uid

            doc
                .set(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        UserManager.userId = user.id
                        continuation.resume(Result.Success(user))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding user document. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

        }

    override suspend fun getCurrentUser(): Result<UserInfo?> = suspendCoroutine { continuation ->
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        UserManager.userId = currentUserUid

        Log.d("getCurrentUser", "UserManager.userId = $currentUserUid")
        Timber.d("UserManager.userId = $currentUserUid")
        if (null == currentUserUid) {
            continuation.resume(Result.Success(null))
        } else {
            val doc =
                FirebaseFirestore.getInstance().collection(PATH_USERS).document(currentUserUid)

            doc.get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userInfo = task.result.toObject(UserInfo::class.java)
                        UserManager.userId = userInfo?.id
                        UserManager.userEmail = userInfo?.email
                        UserManager.userName = userInfo?.name
                        UserManager.userPic = userInfo?.pic
                        Timber.d("UserInfo: ${UserManager.userId},${UserManager.userName},${UserManager.userEmail},${UserManager.userPic}")

                        continuation.resume(Result.Success(userInfo))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error getting user document. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }
    }

    override suspend fun findUserByEmail(query: String): Result<UserInfo?> =
        suspendCoroutine { continuation ->

            val user = FirebaseFirestore.getInstance().collection(PATH_USERS)

            user
                .whereEqualTo("email", query)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val querySnapshot: QuerySnapshot? = task.result
                        if (querySnapshot!!.isEmpty) {
                            continuation.resume(Result.Success(null))
                        } else {
                            val result = mutableListOf<UserInfo>()
                            for (user in task.result) {
                                val userInfo = user.toObject(UserInfo::class.java)
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

    override suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean> =
        suspendCoroutine { continuation ->

            val doc = FirebaseFirestore.getInstance().collection(PATH_NOTES).document(noteId)

            doc
                .update(KEY_AUTHORS, authors.toList())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error updating document. ${it.message}\"")
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
            .whereIn("id", note.authors)
            .whereNotEqualTo("id", note.ownerId)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}")
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
                            Timber.w("[${this::class.simpleName}] Error getting document. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }

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

    override fun getLiveIncomingCoauthorInvitations(): MutableLiveData<List<Invitation>> {

        val liveData = MutableLiveData<List<Invitation>>()

        FirebaseFirestore.getInstance()
            .collection(PATH_INVITATIONS)
            .whereEqualTo("inviteeId", UserManager.userId)
            .orderBy("time", Query.Direction.DESCENDING)
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
                            Timber.w("[${this::class.simpleName}] Error getting document. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }

                }
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
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun deleteUserFromAuthors(
        noteId: String,
        authors: List<String>
    ): Result<String> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)
                .update(
                    KEY_AUTHORS, authors,
                    KEY_LAST_EDIT_TIME, Calendar.getInstance().timeInMillis)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success("Quit coauthoring success"))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun deleteNote(noteId: String): Result<String> =
        suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .document(noteId)
                .delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success("Note deleted"))
                    } else {
                        task.exception?.let {
                            Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                    }
                }
        }


    override suspend fun searchOnYouTube(query: String): Result<YouTubeSearchResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = YouTubeApi.retrofitService.getYouTubeSearchResult(
                    part = YT_SEARCH_PARAM_PART,
                type = YT_SEARCH_PARAM_TYPE,
                    maxResult = 20,
                    query = query
                )
            Result.Success(result)

        } catch (e: Exception) {
            Timber.w(" exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun getTrendingVideosOnYouTube(): Result<YouTubeResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = YouTubeApi.retrofitService.getTrendingVideos(
                part = YT_SEARCH_PARAM_PART,
                chart = YT_VIDEO_PARAM_CHART,
                maxResult = 10,
                regionCode = Locale.getDefault().country
            )
            Result.Success(result)

        } catch (e: Exception) {
            Timber.w(" exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun getSpotifyEpisodeInfo(id: String, authToken: String): Result<EpisodeResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = SpotifyApi.retrofitService.getPodcastInfo(
                id = id,
                bearerWithToken = SPOTIFY_BEARER + authToken
            )

            Timber.d("getSpotifyEpisodeInfo: $result")

            Result.Success(result)

        } catch (e: Exception) {
            Timber.w(" exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun searchOnSpotify(query: String, authToken: String): Result<SpotifySearchResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = SpotifyApi.retrofitService.searchOnSpotify(
                bearerWithToken = SPOTIFY_BEARER + authToken,
                type = SPOTIFY_PARAM_TYPE,
                limit = 20,
                query = query
            )
            Result.Success(result)

        } catch (e: Exception) {
            Timber.w(" exception=${e.message}")
            Result.Error(e)
        }
    }

}
