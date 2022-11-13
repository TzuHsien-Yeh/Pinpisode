package com.tzuhsien.immediat.ext

import androidx.fragment.app.Fragment
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.factory.NoteViewModelFactory
import com.tzuhsien.immediat.factory.ViewModelFactory
import com.tzuhsien.immediat.factory.NoteSourceViewModelFactory


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