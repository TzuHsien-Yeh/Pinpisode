package com.tzuhsien.immediat.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.MyApplication.Companion.applicationContext
import com.tzuhsien.immediat.databinding.FragmentSearchBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.factory.ViewModelFactory
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragment
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel by viewModels<SearchViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSearchBinding.inflate(layoutInflater)

        binding.searchviewSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        viewModel.findMediaSource(query)
                    }
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    return false
                }
            }
        )

        viewModel.toastMsg.observe(viewLifecycleOwner, Observer {
            it?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.showToastCompleted()
            }
        })

        viewModel.navigateToTakeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(YouTubeNoteFragmentDirections.actionGlobalTakeNoteFragment(it))
                viewModel.doneNavigateToTakeNote()
            }
        })

        return binding.root
    }

}