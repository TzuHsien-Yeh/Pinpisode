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
import com.tzuhsien.immediat.ext.utcToLocalTime
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
                    viewModel.resetMsg()
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
        binding.textSearchResult.visibility = View.GONE

        viewModel.ytVideoData.observe(viewLifecycleOwner, Observer {
           it?.let {
               if (it.items.isNotEmpty()) {

                   val item = it.items[0]

                   binding.textSearchResult.visibility = View.VISIBLE
                   binding.cardSingleVideoResult.visibility = View.VISIBLE
                   binding.textTitle.text = item.snippet.title
                   Glide.with(this)
                       .load(item.snippet.thumbnails.high.url)
                       .into(binding.imgThumbnail)
                   binding.textChannelName.text = item.snippet.channelTitle
                   binding.textPublishedTime.text = item.snippet.publishedAt.utcToLocalTime()
                   viewModel.ytSingleResultId = item.id // including video sourceId
               } else {
                   binding.cardSingleVideoResult.visibility = View.GONE
               }
           }
        })

        binding.cardSingleVideoResult.setOnClickListener {
            viewModel.navigateToYoutubeNote(viewModel.ytSingleResultId!!)
        }

        // Display search result
        viewModel.youTubeSearchResult.observe(viewLifecycleOwner) {
            binding.recyclerviewSearchResult.visibility = if (null == it) View.GONE else View.VISIBLE
            binding.recyclerviewYtTrending.visibility = if (null == it) View.VISIBLE else View.GONE
        }

        val resultAdapter = SearchResultAdapter(viewModel.uiState)
        binding.recyclerviewSearchResult.adapter = resultAdapter
        viewModel.searchResultList.observe(viewLifecycleOwner) {
            resultAdapter.submitList(it)
        }

        val ytTrendingAdapter = YtTrendingAdapter(viewModel.uiState)
        binding.recyclerviewYtTrending.adapter = ytTrendingAdapter
        viewModel.ytTrendingList.observe(viewLifecycleOwner) {
            ytTrendingAdapter.submitList(it)

            Timber.d("_ytTrendingList.value = $it")
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(
                    YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(
                        videoIdKey = it
                    )
                )
                viewModel.doneNavigateToTakeNote()
            }
        })

        return binding.root
    }

}