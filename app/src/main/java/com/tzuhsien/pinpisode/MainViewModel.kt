package com.tzuhsien.pinpisode

import androidx.lifecycle.ViewModel
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.data.source.Repository

class MainViewModel(private val repository: Repository): ViewModel() {

    fun getSignedInUser(): UserInfo? {
        return repository.getCurrentUser()
    }
}

