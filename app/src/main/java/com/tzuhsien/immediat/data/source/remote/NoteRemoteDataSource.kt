package com.tzuhsien.immediat.data.source.remote

import android.os.Build.VERSION_CODES.P
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.snapshots
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
import com.tzuhsien.immediat.data.source.DataSource
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.data.succeeded
import com.tzuhsien.immediat.network.YouTubeApi
import com.tzuhsien.immediat.util.Util.getString
import com.tzuhsien.immediat.util.Util.isInternetConnected
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object NoteRemoteDataSource : DataSource {

    private const val PATH_NOTES = "notes"
    private const val PATH_USERS = "users"
    private const val PATH_TIME_ITEMS = "timeItems"
    private const val KEY_START_AT = "startAt" // for orderBy()
    private const val KEY_LAST_EDIT_TIME = "lastEditTime" // for orderBy()

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        val liveData = MutableLiveData<List<Note>>()

        UserManager.userId?.let {
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .whereArrayContains("authors", it)
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
                            Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}\"")
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
                    Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}")
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
            val listResult = YouTubeApi.retrofitService.getVideoInfo(id)

            if (listResult.items.isEmpty()) {
                return Result.Fail(getString(R.string.video_not_available))
            }
            Result.Success(listResult)
        } catch (e: Exception) {
            Timber.w("[${this::class.simpleName}: getYouTubeVideoInfoById] exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun checkIfYouTubeNoteExists(videoId: String): Result<Note?> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)

            notes
                .whereEqualTo("source", Source.YOUTUBE.source)
                .whereEqualTo("sourceId", videoId)
                .whereEqualTo("ownerId", UserManager.userId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val querySnapshot: QuerySnapshot? = task.result
                        if (querySnapshot!!.isEmpty) {
                            continuation.resume(Result.Success(null))
                        } else {
                            val result = mutableListOf<Note>()
                            for(note in task.result) {
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

    override suspend fun createYouTubeVideoNote(videoId: String, note: Note): Result<Note> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
            val doc = notes.document()

            note.id = doc.id

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

    override suspend fun updateYouTubeInfo(noteId: String, note: Note): Result<String> =
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

            noteRef.update("lastEditTime", Calendar.getInstance().timeInMillis)

            val doc = noteRef
                .collection(PATH_TIME_ITEMS)
                .document()

            timeItem.id = doc.id

            doc
                .set(timeItem)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.i("[${this::class.simpleName}] time items: $timeItem ")
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

            noteRef.update("lastEditTime", Calendar.getInstance().timeInMillis)

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

            noteRef.update("lastEditTime", Calendar.getInstance().timeInMillis)

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
                    "digest", note.digest,
                    "lastTimestamp", note.lastTimestamp,
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


    override suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo) : Result<UserInfo> =
        suspendCoroutine { continuation ->
        val doc = FirebaseFirestore.getInstance().collection(PATH_USERS).document(firebaseUser.uid)

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
}
