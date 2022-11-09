package com.tzuhsien.immediat.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tzuhsien.immediat.MainViewModel
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.notelist.NoteListViewModel
import com.tzuhsien.immediat.search.SearchViewModel
import com.tzuhsien.immediat.signin.SignInViewModel

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

                isAssignableFrom(NoteListViewModel::class.java) ->
                    NoteListViewModel(repository)

                isAssignableFrom(SignInViewModel::class.java) ->
                    SignInViewModel(repository)

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
