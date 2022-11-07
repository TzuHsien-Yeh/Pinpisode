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
import androidx.navigation.fragment.findNavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.TimeItemDisplay
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource.getLiveNoteById
import com.tzuhsien.immediat.databinding.FragmentYoutubeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.parseDuration
import com.tzuhsien.immediat.tag.TagDialogFragmentDirections
import timber.log.Timber


class YouTubeNoteFragment : Fragment() {

    private val viewModel by viewModels<YouTubeNoteViewModel> {
        getVmFactory(
            YouTubeNoteFragmentArgs.fromBundle(requireArguments()).noteIdKey,
            YouTubeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey
        )
    }
    private lateinit var binding: FragmentYoutubeNoteBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentYoutubeNoteBinding.inflate(layoutInflater)

        /**
         *  YouTube player and behavior control
         * */
        val youTubePlayerView: YouTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView)

        val videoId = viewModel.videoId

        var ytPlayer: YouTubePlayer? = null

        viewModel.playStart.observe(viewLifecycleOwner, Observer { startAt ->
            startAt?.let {
                ytPlayer?.seekTo(startAt)
                ytPlayer?.play()
            }
        })

        viewModel.currentSecond.observe(viewLifecycleOwner, Observer { currentSec ->
            viewModel.playMomentEnd?.let {
                if (it <= currentSec) {
                    ytPlayer?.pause()
                    viewModel.clearPlayingMomentStart()
                    viewModel.clearPlayingMomentEnd()
                }
            }
        })

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0f)
                ytPlayer = youTubePlayer
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)

                viewModel.getCurrentSecond(second)
                Timber.d("viewModel.getCurrentSecond(second): $second")

                binding.btnTakeTimestamp.setOnClickListener {
                    viewModel.createTimeItem(second, null)
                }

                binding.btnClip.setOnClickListener {
                    when (viewModel.startOrStopToggle) {
                        0 -> {
                            viewModel.startAt = second
                            viewModel.startOrStopToggle = 1
                            binding.btnClip.setImageResource(R.drawable.ic_square)
                            Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                        }
                        1 -> {
                            viewModel.endAt = second
                            Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")

                            binding.btnClip.setImageResource(R.drawable.ic_youtube_black)
//                            binding.btnClip.setImageResource(R.drawable.ic_end_clipping)
                            viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
                            viewModel.startOrStopToggle = 0
                        }
                    }
                }
            }
        })

        /**
         * Digest of the video (editText)
         * */
        binding.editDigest.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.newNote.digest = binding.editDigest.text.toString()
                viewModel.updateNote()
            }
        }
        viewModel.liveNoteData.observe(viewLifecycleOwner, Observer {
            it?.let { note ->
                binding.editDigest.setText(note.digest)
                viewModel.newNote = note

                if (note.duration.parseDuration() == 0L) {
                    viewModel.updateInfoFromYouTube(note)
                }
            }
        })

        /**
         *  RecyclerView views
         * */
        val adapter = TimeItemAdapter(
            uiState = viewModel.uiState
        )
        binding.recyclerViewTimeItems.adapter = adapter
        viewModel.liveTimeItemList.observe(viewLifecycleOwner, Observer { list ->
            when (viewModel.displayState) {
                TimeItemDisplay.ALL -> {
                    adapter.submitList(list)
                }
                TimeItemDisplay.TIMESTAMP -> {
                    adapter.submitList(list.filter { it.endAt == null })
                }
                TimeItemDisplay.CLIP -> {
                    adapter.submitList(list.filter { it.endAt != null })
                }
            }
        })

        binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_youtube_black)
        binding.icTimeItemDisplayOptions.setOnClickListener {
            when (viewModel.displayState) {
                TimeItemDisplay.ALL -> {
                    // to display only timestamps
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_pin)
                    viewModel.displayState = TimeItemDisplay.TIMESTAMP
                    viewModel.notifyDisplayChange()
                }
                TimeItemDisplay.TIMESTAMP -> {
                    // to display only clips
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_square)
                    viewModel.displayState = TimeItemDisplay.CLIP
                    viewModel.notifyDisplayChange()
                }

                TimeItemDisplay.CLIP -> {
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_youtube_black)
                    viewModel.displayState = TimeItemDisplay.ALL
                    viewModel.notifyDisplayChange()
                }
            }
        }

        // Navigate to tag fragment
        binding.icAddTag.setOnClickListener {
            findNavController().navigate(TagDialogFragmentDirections.actionGlobalTagDialogFragment(
                viewModel.newNote))
        }

        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.youtubePlayerView.enterFullScreen()
            // TODO : hide toolbar and bottom tool bar

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.youtubePlayerView.exitFullScreen()
        }
    }
}