package com.tzuhsien.immediat.youtubenote

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.FragmentYoutubeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory
import timber.log.Timber


class YouTubeNoteFragment : Fragment() {

    private val viewModel by viewModels<YouTubeNoteViewModel> {
        getVmFactory(
            YouTubeNoteFragmentArgs.fromBundle(requireArguments()).noteIdKey,
            YouTubeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey
        )
    }
    private lateinit var binding: FragmentYoutubeNoteBinding

    var startOrStopToggle = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentYoutubeNoteBinding.inflate(layoutInflater)

        val youTubePlayerView: YouTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                val videoId = viewModel.videoId

                youTubePlayer.loadVideo(videoId, 0f)

//                youTubePlayer.seekTo(viewModel.)
                // TODO: recyclerview item onclick > play at the second / clip
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)

                binding.btnTakeTimestamp.setOnClickListener {
                    viewModel.createTimeItem(second, null)
                }

                binding.btnClip.setOnClickListener {
                    when(startOrStopToggle) {
                        0 -> {
                            viewModel.startAt = second
                            startOrStopToggle = 1
                            binding.btnClip.setImageResource(R.drawable.ic_square)
                            Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                        }
                        1 -> {
                            viewModel.endAt = second
                            Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")

                            binding.btnClip.setImageResource(R.drawable.ic_youtube_black)
//                            binding.btnClip.setImageResource(R.drawable.ic_end_clipping)
                            viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
                            startOrStopToggle = 0
                        }
                    }
                }
            }

        })

        viewModel.liveNoteData.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.editDigest.setText(it.digest)
                viewModel.newNote = it
            }
        })

        binding.editDigest.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                viewModel.newNote.digest = binding.editDigest.text.toString()
                viewModel.updateNote()
            }
        }

        val adapter = TimeItemAdapter(
            TimeItemAdapter.OnClickListener { viewModel.playTimeItem(it) },
            uiState = viewModel.uiState
        )
        binding.recyclerViewTimeItems.adapter = adapter
        viewModel.liveTimeItemList.observe(viewLifecycleOwner, Observer {
            Timber.d("viewModel.liveTimeItemList.observe: $it")
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        })

        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.youtubePlayerView.enterFullScreen()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.youtubePlayerView.exitFullScreen()
        }
    }
}