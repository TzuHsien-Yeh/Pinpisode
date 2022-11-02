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
    private const val KEY_LAST_EDIT_TIME = "lastEditTime" // for orderby()

    override suspend fun getAllNotes() {
        TODO("Not yet implemented")
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

    override suspend fun updateYouTubeVideoInfo(videoId: String, note: Note): Result<String> =
        suspendCoroutine { continuation ->
            //TODO: LOGIN 後就進入LIST PAGE 拿USER為owner的所有筆記 > 加新的videoId之前local端先比對該sourceId不在user的筆記清單中才建立新note文件
//            val notesRef = FirebaseFirestore.getInstance()
//                .collection(PATH_NOTES)
//                .whereEqualTo("sourceId", videoId)
//                .whereEqualTo("ownerId", com.tzuhsien.immediat.data.source.local.UserManager.userId)
//                .whereEqualTo("source", Source.YOUTUBE.source)
//
//            notesRef.get().addOnSuccessListener { task ->
//                if (task.isSuccessful) {
//                    for (document in task.result) {
//                        Timber.d(document.id + " => " + document.data)
//
//                        if (document.exists()) {

//                            continuation.resume(Result.Success(document.id))
//                        } else {
            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
            val doc = notes.document()

            note.id = doc.id

            note.lastEditTime = Calendar.getInstance().timeInMillis

            doc
                .set(note)
                .addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        Timber.i("[${this::class.simpleName}] note: $note ")

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
//                    }
//
//                } else {
//                    task.exception?.let {
//                        Timber.w("[${this::class.simpleName}] Error querying documents. ${it.message}\"")
//                        return@addOnCompleteListener
//                    }
//                }
//            }

//        }


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
                .set(note)
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