package com.tzuhsien.immediat.notelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
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

        /**
         * Tag list
         * */
        val tagAdapter = TagAdapter(onClickListener = TagAdapter.OnTagClickListener {
            binding.textSelectedTag.text = it
            binding.cardSelectedTag.visibility = View.VISIBLE
            viewModel.hideSelectedTagFromTagSet(it)
        })
        binding.recyclerviewTag.adapter = tagAdapter
        viewModel.tagSet.observe(viewLifecycleOwner, Observer { set ->
            tagAdapter.submitList(set.filter { it != viewModel.selectedTag }.sorted().toList())
        })

//        viewModel.selectedTag.observe(viewLifecycleOwner, Observer {
//            if (null == it) {
//                binding.cardSelectedTag.visibility = View.GONE
//            } else {
//                binding.cardSelectedTag.visibility = View.VISIBLE
//                binding.textSelectedTag.text = it
//            }
//        })


        /**
         * Note list
         * */
        val noteAdapter = NoteAdapter(onClickListener = NoteAdapter.OnNoteClickListener {
            viewModel.navigateToNotePage(it)
        })
        binding.recyclerviewNoteList.adapter = noteAdapter
        viewModel.liveNoteList.observe(viewLifecycleOwner, Observer {
            viewModel.getAllTags(it)
            noteAdapter.submitList(it)
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
