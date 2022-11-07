package com.tzuhsien.immediat.data.source.remote

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.YouTubeResult
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

        FirebaseFirestore.getInstance()
            .collection(PATH_NOTES)
            .whereArrayContains("authors", UserManager.userId)
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
        return liveData
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

    override suspend fun getCoauthoringNotes() {
        TODO("Not yet implemented")
    }

    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            // this will run on a thread managed by Retrofit
            val listResult = YouTubeApi.retrofitService.getVideoInfo(id)

            listResult.error?.let {
                return Result.Fail(it)
            }
            Result.Success(listResult)
        } catch (e: Exception) {
            Timber.w("[${this::class.simpleName}: getYouTubeVideoInfoById] exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun createYouTubeVideoNote(videoId: String, note: Note): Result<String> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
            val doc = notes.document()

            note.id = doc.id

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .set(note)
                .addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        continuation.resume(Result.Success(doc.id))
                    } else {
                        task2.exception?.let {
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

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .update(
                    "duration", note.duration,
                    "title", note.title,
                    "thumbnail", note.thumbnail
                )
                .addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        continuation.resume(Result.Success(doc.id))
                    } else {
                        task2.exception?.let {
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


    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }

}