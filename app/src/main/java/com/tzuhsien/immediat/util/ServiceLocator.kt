package com.tzuhsien.immediat.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.tzuhsien.immediat.data.source.DefaultRepository
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource

object ServiceLocator {
    @Volatile
    var repository: Repository? = null
        @VisibleForTesting set

    fun provideTasksRepository(context: Context): Repository {
        synchronized(this) {
            return repository
                ?: repository
                ?: createRepository(context)
        }
    }

    private fun createRepository(context: Context): Repository {
        return DefaultRepository(
            NoteRemoteDataSource
        )
    }
}