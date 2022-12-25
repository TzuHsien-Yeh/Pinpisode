package com.tzuhsien.pinpisode.youtubenote

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.TimeItemDisplay
import com.tzuhsien.pinpisode.databinding.FragmentYoutubeNoteBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.parseDuration
import com.tzuhsien.pinpisode.loading.LoadingDialog
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.spotifynote.SpotifyNoteService
import com.tzuhsien.pinpisode.spotifynote.SpotifyService
import com.tzuhsien.pinpisode.util.DEEPLINK_PATH_YOUTUBE_NOTE
import com.tzuhsien.pinpisode.util.DYNAMIC_LINK_PREFIX
import com.tzuhsien.pinpisode.util.SharingLinkGenerator
import com.tzuhsien.pinpisode.util.SwipeHelper
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

        SpotifyService.pause()
        Intent(context, SpotifyNoteService::class.java).apply {
            action = SpotifyNoteService.ACTION_STOP
            context?.startService(this)
        }

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
                                startAnimation(animation)
                            }
                            Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                        }
                        1 -> {
                            if (second < viewModel.startAt) {
                                Toast.makeText(context,
                                    getString(R.string.clip_end_before_start_warning),
                                    Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.endAt = second
                                binding.btnClip.animation = null
                                viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
                                viewModel.startOrStopToggle = 0
                            }
                            Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")

                        }
                    }
                }
            }
        })

        /**
         * Digest of the video (editText)
         * */
        binding.editDigest.setBackgroundColor(Color.TRANSPARENT)

        viewModel.isLiveNoteReady.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.liveNote.observe(viewLifecycleOwner) { note ->
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
         *  RecyclerView views
         * */
        // Swipe to delete
        binding.recyclerViewTimeItems.addItemDecoration(DividerItemDecoration(context,
            DividerItemDecoration.VERTICAL))
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(
            binding.recyclerViewTimeItems,
            swipeOutListener = OnSwipeOutListener {
                viewModel.deleteTimeItem(it)
            }
        ) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                return listOf(deleteButton)
            }
        })

        val adapter = TimeItemAdapter(uiState = viewModel.uiState)
        binding.recyclerViewTimeItems.adapter = adapter
        viewModel.isLiveTimeItemListReady.observe(viewLifecycleOwner) { timeItemLiveDataAssigned ->
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
         *  Edit or read only mode
         * */
        viewModel.canEdit.observe(viewLifecycleOwner) {
            binding.editDigest.isEnabled = it
            binding.editDigest.hint = if (it) getString(R.string.input_video_summary) else null
            binding.icAddTag.isEnabled = it
            binding.btnClip.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnTakeTimestamp.visibility = if (it) View.VISIBLE else View.GONE
            if (it) {
                // attach swipe to delete helper
                itemTouchHelper.attachToRecyclerView(binding.recyclerViewTimeItems)
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
                findNavController().navigate(NavGraphDirections.actionGlobalTagDialogFragment(it))
            }
        }

        /**
         *  Buttons on the bottom of the page: Coauthoring
         * */
        binding.icCoauthoring.setOnClickListener {
            viewModel.noteToBeUpdated?.let {
                findNavController().navigate(
                    (NavGraphDirections.actionGlobalCoauthorDialogFragment(it))
                )
            }
        }

        /**
         *  Buttons on the bottom of the page: Share this note
         * */
        binding.icShare.setOnClickListener {
            shareNoteLink()
        }

        /** Loading status and error messages **/
        viewModel.error.observe(viewLifecycleOwner) {
            it?.let {
                requireActivity().supportFragmentManager.setFragmentResult(LoadingDialog.REQUEST_ERROR,
                    bundleOf(LoadingDialog.KEY_ERROR_MSG to it))
            }
        }

        viewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(NavGraphDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
                        if (errorMsg != getString(R.string.note_not_available_anymore)) {
                            requireActivity().supportFragmentManager.setFragmentResult(
                                REQUEST_DISMISS,
                                bundleOf(KEY_DONE_LOADING to true))
                        }
                    }
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_DISMISS,
                        bundleOf(KEY_DONE_LOADING to false))
                }
            }
        }

        return binding.root
    }

    private fun shareNoteLink() {

        val deepLink =
            DYNAMIC_LINK_PREFIX + DEEPLINK_PATH_YOUTUBE_NOTE + viewModel.noteId + "/" + viewModel.videoId

        SharingLinkGenerator.generateSharingLink(
            deepLink = deepLink.toUri(),
            previewImageLink = viewModel.noteToBeUpdated?.thumbnail?.toUri()
        ) { generatedLink ->
            Timber.d("generatedLink = $generatedLink")
            shareDynamicLink(generatedLink)
        }
    }

    private fun shareDynamicLink(dynamicLink: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                getString(R.string.share_msg_subject,
                    "YouTube video",
                    viewModel.noteToBeUpdated?.title)
            )
            putExtra(Intent.EXTRA_TEXT, dynamicLink)
        }

        startActivity(Intent.createChooser(intent, null))
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

    fun deleteButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            MyApplication.applicationContext(),
            getString(R.string.delete),
            15.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    viewModel.deleteTimeItem(position)
                }
            })
    }

}