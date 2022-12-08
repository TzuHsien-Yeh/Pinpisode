package com.tzuhsien.pinpisode.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.tzuhsien.pinpisode.data.source.DefaultRepository
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.data.source.remote.NoteRemoteDataSource

object ServiceLocator {
    @Volatile
    var repository: Repository? = null
        @VisibleForTesting set

    fun provideTasksRepository(context: Context): Repository {
        synchronized(this) {
            return repository
                ?: createRepository(context)
        }
    }

    private fun createRepository(context: Context): Repository {
        return DefaultRepository(
            NoteRemoteDataSource
        )
    }
}