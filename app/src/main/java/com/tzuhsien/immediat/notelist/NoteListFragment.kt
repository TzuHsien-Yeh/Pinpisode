package com.tzuhsien.immediat.notelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.FragmentNoteListBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections

class NoteListFragment : Fragment() {

    private val viewModel by viewModels<NoteListViewModel> {
        getVmFactory()
    }
    private lateinit var binding: FragmentNoteListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNoteListBinding.inflate(layoutInflater)

        val adapter = NoteAdapter(onClickListener = NoteAdapter.OnNoteClickListener {
            viewModel.navigateToNotePage(it)
        })
        binding.recyclerviewNoteList.adapter = adapter

        viewModel.liveNoteList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(
                YouTubeNoteFragmentDirections.actionGlobalTakeNoteFragment(
                    noteIdKey = it.id,
                    videoIdKey = it.sourceId
                )
            )
        })

        return binding.root
    }

}
