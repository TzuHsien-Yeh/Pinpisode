package com.tzuhsien.pinpisode.ext

import androidx.fragment.app.Fragment
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.factory.NoteViewModelFactory
import com.tzuhsien.pinpisode.factory.ViewModelFactory
import com.tzuhsien.pinpisode.factory.NoteSourceViewModelFactory


fun Fragment.getVmFactory(): ViewModelFactory {
    val repository = (requireContext().applicationContext as MyApplication).repository
    return ViewModelFactory(repository)
}

fun Fragment.getVmFactory(noteId: String?, sourceId: String): NoteSourceViewModelFactory {
    val repository = (requireContext().applicationContext as MyApplication).repository
    return NoteSourceViewModelFactory(repository, noteId, sourceId)
}

fun Fragment.getVmFactory(note: Note): NoteViewModelFactory {
    val repository = (requireContext().applicationContext as MyApplication).repository
    return NoteViewModelFactory(repository, note)
}