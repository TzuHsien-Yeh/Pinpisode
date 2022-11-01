package com.tzuhsien.immediat.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.databinding.FragmentSearchBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber

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

        viewModel.showMsg.observe(viewLifecycleOwner, Observer {
            if (null != it) {
                binding.textResourceNotFound.text = it
                binding.textResourceNotFound.visibility = View.VISIBLE

                Timber.d("not available")
            } else {
                binding.textResourceNotFound.visibility = View.GONE
            }
        })

        binding.cardSingleVideoResult.visibility = View.GONE

        viewModel.ytVideoData.observe(viewLifecycleOwner, Observer {
           it?.let {
               if (it.items.isNotEmpty()) {

                   val item = it.items[0]

                   binding.textTitle.text = item.snippet.title
                   Glide.with(this)
                       .load(item.snippet.thumbnails.high.url)
                       .into(binding.imgThumbnail)
                   binding.cardSingleVideoResult.visibility = View.VISIBLE
//                   binding.textPublishedTime.text = item.snippet.publishedAt.toDate().formatTo( "yyyy-MM-dd HH:mm:ss")

                   viewModel.setYoutubeNoteData(item)
               } else {
                   binding.cardSingleVideoResult.visibility = View.GONE
               }
           }
        })

        binding.cardSingleVideoResult.setOnClickListener {
            viewModel.updateYouTubeVideoInfo()
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(YouTubeNoteFragmentDirections.actionGlobalTakeNoteFragment(it))
                viewModel.doneNavigateToTakeNote()
            }
        })


        return binding.root
    }

}