package com.tzuhsien.pinpisode.data.source.remote

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.Result
import com.tzuhsien.pinpisode.data.model.*
import com.tzuhsien.pinpisode.data.source.NoteDataSource
import com.tzuhsien.pinpisode.network.SpotifyApi
import com.tzuhsien.pinpisode.network.YouTubeApi
import com.tzuhsien.pinpisode.util.Util.getString
import com.tzuhsien.pinpisode.util.Util.isInternetConnected
import retrofit2.HttpException
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object NoteRemoteDataSource : NoteDataSource {

    // Firebase
    private const val PATH_NOTES = "notes"
    private const val PATH_TIME_ITEMS = "timeItems"

    private const val KEY_START_AT = "startAt"
    private const val KEY_LAST_EDIT_TIME = "lastEditTime"
    private const val KEY_AUTHORS = "authors"
    private const val KEY_SOURCE = "source"
    private const val KEY_SOURCE_ID = "sourceId"
    private const val KEY_OWNER_ID = "ownerId"
    private const val KEY_LAST_TIME_STAMP = "lastTimestamp"
    private const val KEY_DIGEST = "digest"
    private const val KEY_DURATION = "duration"
    private const val KEY_THUMBNAIL = "thumbnail"
    private const val KEY_TITLE = "title"
    private const val KEY_TEXT = "text"
    private const val KEY_TAGS = "tags"

    // Youtube
    private const val YT_VIDEO_PARAM_PART = "snippet, contentDetails"
    private const val YT_SEARCH_PARAM_PART = "snippet"
    private const val YT_SEARCH_PARAM_TYPE = "video"
    private const val YT_VIDEO_PARAM_CHART = "mostPopular"

    // Spotify
    private const val SPOTIFY_BEARER = "Bearer "
    private const val SPOTIFY_PARAM_TYPE = "episode"

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {

        val liveData = MutableLiveData<List<Note>>()

        Firebase.auth.currentUser?.let {
            FirebaseFirestore.getInstance()
                .collection(PATH_NOTES)
                .whereArrayContains(KEY_AUTHORS, it.uid)
                .orderBy(KEY_LAST_EDIT_TIME, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    Timber.i("addSnapshotListener detect")

                    error?.let { Timber.w("Error getting documents. ${it.message}") }

                    val list = mutableListOf<Note>()
                    if (snapshot != null) {
                        for (doc in snapshot) {
                            Timber.d(doc.id + " => " + doc.data)
                            val noteItem = doc.toObject(Note::class.java)
                            list.add(noteItem)
                        }
                    }

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
                        if (null != note) {
                            continuation.resume(Result.Success(note))
                        } else {
                            continuation.resume(Result.Fail(getString(R.string.note_not_available_anymore)))
                        }
                    } else {
                        task.exception?.let {
                            Timber.w("Error getting documents. ${it.message}")
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
            Timber.w("[getYouTubeVideoInfoById] exception=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun checkIfNoteAlreadyExists(source: String, sourceId: String, currentUser: UserInfo?): Result<Note?> =
        suspendCoroutine { continuation ->

            val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)

            notes
                .whereEqualTo(KEY_SOURCE, source)
                .whereEqualTo(KEY_SOURCE_ID, sourceId)
                .whereEqualTo(KEY_OWNER_ID, currentUser?.id)
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
                            Timber.w("Error finding documents. ${it.message}")
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
                            Timber.w("Error adding documents. ${it.message}")
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
                    KEY_DURATION, note.duration,
                    KEY_TITLE, note.title,
                    KEY_THUMBNAIL, note.thumbnail
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(doc.id))
                    } else {
                        task.exception?.let {
                            Timber.w("Error adding documents. ${it.message}")
                            continuation.resume(Result.Error(it))
                        }
                        continuation.resume(Result.Fail(getString(R.string.unknown_error)))
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
                    Timber.w("Error getting documents. ${it.message}")
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
                            Timber.w("Error adding documents. ${it.message}")
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
                .update(KEY_TITLE, timeItem.title, KEY_TEXT, timeItem.text)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(0))
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
                            Timber.w("Error adding documents. ${it.message}")
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
                            Timber.w("Error adding documents. ${it.message}")
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
                    KEY_TAGS, note.tags,
                    KEY_LAST_EDIT_TIME, note.lastEditTime
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(""))
                    } else {
                        task.exception?.let {
                            Timber.w("Error adding documents. ${it.message}")
                            continuation.resume(Result.Error(it))
                            return@addOnCompleteListener
                        }
                        continuation.resume(Result.Fail(getString(R.string.unknown_error)))
                    }
                }
        }

    override suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean> =
        suspendCoroutine { continuation ->

            val doc = FirebaseFirestore.getInstance().collection(PATH_NOTES).document(noteId)

            doc
                .update(
                    KEY_AUTHORS, authors.toList(),
                    KEY_LAST_EDIT_TIME, Calendar.getInstance().timeInMillis
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.Success(true))
                    } else {
                        task.exception?.let {
                            Timber.w("Error updating document. ${it.message}")
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
                            Timber.w("Error updating documents. ${it.message}")
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
                            Timber.w("Error deleting documents. ${it.message}")
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
            Timber.w("exception=${e.message}")
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

    override suspend fun getSpotifyEpisodeInfo(id: String, authToken: String): Result<SpotifyItem> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = SpotifyApi.retrofitService.getPodcastInfo(
                id = id,
                bearerWithToken = SPOTIFY_BEARER + authToken
            )

            Timber.d("getSpotifyEpisodeInfo: $result")
            if (result.uri.isEmpty()) {
                Result.Fail(MyApplication.applicationContext().getString(R.string.episode_not_found))
            }

            Result.Success(result)

        } catch (e: HttpException) {
            when(e.code()) {
                401 -> {
                    Result.SpotifyAuthError(true)
                }
                429 -> {
                    Result.Fail(getString(R.string.spotify_exceeded_app_rate_limits))
                }
                403 -> {
                    Result.Fail(getString(R.string.spotify_api_bad_oauth_request))
                }
                else -> {
                    Result.Fail(getString(R.string.unknown_error))
                }
            }
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

        } catch (e: HttpException) {
            when(e.code()) {
                401 -> {
                    Result.SpotifyAuthError(true)
                }
                429 -> {
                    Result.Fail(getString(R.string.spotify_exceeded_app_rate_limits))
                }
                403 -> {
                    Result.Fail(getString(R.string.spotify_api_bad_oauth_request))
                }
                else -> {
                    Result.Fail(getString(R.string.unknown_error))
                }
            }
        }
    }

    override suspend fun getUserSavedShows(authToken: String): Result<SpotifyShowResult> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = SpotifyApi.retrofitService.getUserSavedShows(
                bearerWithToken = SPOTIFY_BEARER + authToken,
                limit = 10
            )
            result.error?.let {
                Result.Fail(it.message)
                Timber.d("getUserSavedShows: result.error = Result.Fail(${it.message})")
            }
            Result.Success(result)

        } catch (exception: HttpException) {
            exception.code()

            Timber.d("getUserSavedShows: exception.code() = ${exception.code()}")
            Timber.d("getUserSavedShows: exception.response()?.code() = ${exception.response()?.code()}")
            Timber.d("getUserSavedShows: exception.response().errorBody() = ${exception.response()?.errorBody()}")
            Timber.d("getUserSavedShows: exception.response().body() = ${exception.response()?.body()}")

            when(exception.code()) {
                401 -> {
                    Result.SpotifyAuthError(true)
                }
                429 -> {
                    Result.Fail(getString(R.string.spotify_exceeded_app_rate_limits))
                }
                403 -> {
                    Result.Fail(getString(R.string.spotify_api_bad_oauth_request))
                }
                else -> {
                    Result.Fail(getString(R.string.unknown_error))
                }
            }
        }
    }

    override suspend fun getShowEpisodes(showId: String, authToken: String): Result<Episodes> {
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            val result = SpotifyApi.retrofitService.getShowEpisodes(
                bearerWithToken = SPOTIFY_BEARER + authToken,
                id = showId,
                limit = 1
            )
            Result.Success(result)

        } catch (e: HttpException) {
            when(e.code()) {
                401 -> {
                    Result.SpotifyAuthError(true)
                }
                429 -> {
                    Result.Fail(getString(R.string.spotify_exceeded_app_rate_limits))
                }
                403 -> {
                    Result.Fail(getString(R.string.spotify_api_bad_oauth_request))
                }
                else -> {
                    Result.Fail(getString(R.string.unknown_error))
                }
            }
        }
    }

}