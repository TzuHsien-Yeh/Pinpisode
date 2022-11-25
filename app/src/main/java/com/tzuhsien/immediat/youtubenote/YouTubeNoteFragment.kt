package com.tzuhsien.immediat.youtubenote

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.coauthor.CoauthorDialogFragmentDirections
import com.tzuhsien.immediat.data.model.TimeItemDisplay
import com.tzuhsien.immediat.databinding.FragmentYoutubeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.parseDuration
import com.tzuhsien.immediat.tag.TagDialogFragmentDirections
import com.tzuhsien.immediat.util.SwipeHelper
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

        viewModel.playStart.observe(viewLifecycleOwner) { startAt ->
            startAt?.let {
                ytPlayer?.seekTo(startAt)
                ytPlayer?.play()
            }
        }

        viewModel.currentSecond.observe(viewLifecycleOwner) { currentSec ->
            viewModel.playEnd?.let {
                if (it <= currentSec) {
                    ytPlayer?.pause()
                    viewModel.clearPlayingMomentStart()
                    viewModel.clearPlayingMomentEnd()
                }
            }
        }

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0f)
                ytPlayer = youTubePlayer
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)

                viewModel.getCurrentSecond(second)

                binding.btnTakeTimestamp.setOnClickListener {
                    Timber.d("binding.btnTakeTimestamp.setOnClickListener clicked")
                    viewModel.createTimeItem(second, null)
                }

                val animation = AnimationUtils.loadAnimation(context, R.anim.ic_clipping)

                binding.btnClip.setOnClickListener {
                    when (viewModel.startOrStopToggle) {
                        0 -> {
                            viewModel.startAt = second
                            viewModel.startOrStopToggle = 1
                            binding.btnClip.apply {
                                setImageResource(R.drawable.ic_clipping_stop)
                                startAnimation(animation)
                            }
                            Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                        }
                        1 -> {
                            viewModel.endAt = second
                            Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")

                            binding.btnClip.setImageResource(R.drawable.ic_clip)
                            binding.btnClip.animation = null
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
        binding.editDigest.setBackgroundColor(Color.TRANSPARENT)

        viewModel.liveNoteDataReassigned.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.liveNoteData.observe(viewLifecycleOwner) { note ->
                    note?.let {
                        binding.editDigest.setText(note.digest)
                        viewModel.noteToBeUpdated = note

                        if (note.duration.parseDuration() == 0L) {
                            viewModel.updateInfoFromYouTube(note)
                        }
                    }
                }
                binding.editDigest.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        viewModel.noteToBeUpdated?.let { note ->
                            note.digest = binding.editDigest.text.toString()
                            viewModel.updateNote()
                        }
                    }
                }
            }
        }


        /**
         *  Edit or read only mode
         * */
        viewModel.canEdit.observe(viewLifecycleOwner) {
            binding.editDigest.isEnabled = it
            if (it) {
                binding.editDigest.visibility = View.VISIBLE
                binding.editDigest.hint = getString(R.string.input_video_summary)
            } else if (viewModel.noteToBeUpdated?.digest.isNullOrEmpty()) {
                binding.editDigest.visibility = View.GONE
            } else {
                binding.editDigest.visibility = View.VISIBLE
                binding.editDigest.hint = getString(R.string.input_video_summary)
            }

            binding.icAddTag.isEnabled = it
            binding.btnClip.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnTakeTimestamp.visibility = if (it) View.VISIBLE else View.GONE
        }

        /**
         *  RecyclerView views
         * */
        // Swipe to delete
        binding.recyclerViewTimeItems.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.recyclerViewTimeItems) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                return listOf(deleteButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewTimeItems)


        val adapter = TimeItemAdapter(
            uiState = viewModel.uiState
        )
        binding.recyclerViewTimeItems.adapter = adapter

        viewModel.timeItemLiveDataReassigned.observe(viewLifecycleOwner) { timeItemLiveDataAssigned ->
            if (timeItemLiveDataAssigned == true) {
                viewModel.liveTimeItemList.observe(viewLifecycleOwner, Observer { list ->
                    list?.let {
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
                    }

                })
            }
        }


        /**
         *  Buttons on the bottom of the page: Toggle the display of timeItems
         * */
        binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_all)
        binding.icTimeItemDisplayOptions.setOnClickListener {
            when (viewModel.displayState) {
                TimeItemDisplay.ALL -> {
                    // to display only timestamps
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_timestamps)
                    viewModel.displayState = TimeItemDisplay.TIMESTAMP
                    viewModel.notifyDisplayChange()
                }
                TimeItemDisplay.TIMESTAMP -> {
                    // to display only clips
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_clips)
                    viewModel.displayState = TimeItemDisplay.CLIP
                    viewModel.notifyDisplayChange()
                }
                TimeItemDisplay.CLIP -> {
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_all)
                    viewModel.displayState = TimeItemDisplay.ALL
                    viewModel.notifyDisplayChange()
                }
            }
        }

        /**
         *  Buttons on the bottom of the page: Navigate to tag fragment
         * */
        binding.icAddTag.setOnClickListener {
            viewModel.noteToBeUpdated?.let {
                findNavController().navigate(TagDialogFragmentDirections.actionGlobalTagDialogFragment(it))
            }
        }

        /**
         *  Buttons on the bottom of the page: Coauthoring
         * */
        binding.icCoauthoring.setOnClickListener {
            findNavController().navigate(
                (CoauthorDialogFragmentDirections.actionGlobalCoauthorDialogFragment(
                    viewModel.noteToBeUpdated!!
                ))
            )
        }

        /**
         *  Buttons on the bottom of the page: Share this note
         * */
        binding.icShare.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.youtube_note_uri, viewModel.noteId, videoId))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.send_to)))
        }

        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.youtubePlayerView.enterFullScreen()

            binding.notePageBottomOptions.visibility = View.GONE

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.youtubePlayerView.exitFullScreen()

            binding.notePageBottomOptions.visibility = View.VISIBLE
        }
    }

    fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            MyApplication.applicationContext(),
            getString(R.string.delete),
            14.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    viewModel.deleteTimeItem(position)
                }
            })
    }

}