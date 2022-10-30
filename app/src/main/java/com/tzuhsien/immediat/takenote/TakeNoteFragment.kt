package com.tzuhsien.immediat.takenote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tzuhsien.immediat.databinding.FragmentTakeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory

class TakeNoteFragment : Fragment() {

    private val viewModel by viewModels<TakeNoteViewModel> {
        getVmFactory(TakeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey)
    }
    private lateinit var binding: FragmentTakeNoteBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentTakeNoteBinding.inflate(layoutInflater)

        return binding.root
    }

}