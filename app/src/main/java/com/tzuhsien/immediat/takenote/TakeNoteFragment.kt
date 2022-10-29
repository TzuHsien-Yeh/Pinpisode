package com.tzuhsien.immediat.takenote

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tzuhsien.immediat.R

class TakeNoteFragment : Fragment() {

    companion object {
        fun newInstance() = TakeNoteFragment()
    }

    private lateinit var viewModel: TakeNoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_take_note, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TakeNoteViewModel::class.java)
        // TODO: Use the ViewModel
    }

}