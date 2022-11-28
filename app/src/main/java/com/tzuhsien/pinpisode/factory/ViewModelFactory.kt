package com.tzuhsien.pinpisode.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.pinpisode.MainViewModel
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.notelist.NoteListViewModel
import com.tzuhsien.pinpisode.notification.NotificationViewModel
import com.tzuhsien.pinpisode.profile.ProfileViewModel
import com.tzuhsien.pinpisode.search.SearchViewModel
import com.tzuhsien.pinpisode.search.result.SearchResultViewModel
import com.tzuhsien.pinpisode.signin.SignInViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(MainViewModel::class.java) ->
                    MainViewModel(repository)

                isAssignableFrom(SearchViewModel::class.java) ->
                    SearchViewModel(repository)

                isAssignableFrom(SearchResultViewModel::class.java) ->
                    SearchResultViewModel(repository)

                isAssignableFrom(NoteListViewModel::class.java) ->
                    NoteListViewModel(repository)

                isAssignableFrom(SignInViewModel::class.java) ->
                    SignInViewModel(repository)

                isAssignableFrom(ProfileViewModel::class.java) ->
                    ProfileViewModel(repository)

                isAssignableFrom(NotificationViewModel::class.java) ->
                    NotificationViewModel(repository)

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
