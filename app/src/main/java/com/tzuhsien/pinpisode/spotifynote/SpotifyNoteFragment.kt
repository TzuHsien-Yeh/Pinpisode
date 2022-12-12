package com.tzuhsien.pinpisode.spotifynote

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.spotify.protocol.types.PlayerState
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.model.TimeItemDisplay
import com.tzuhsien.pinpisode.databinding.FragmentSpotifyNoteBinding
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.formatDuration
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.parseSpotifyImageUri
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.pause
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.resume
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekBack
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekForward
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekTo
import com.tzuhsien.pinpisode.util.DEEPLINK_PATH_SPOTIFY_NOTE
import com.tzuhsien.pinpisode.util.DYNAMIC_LINK_PREFIX
import com.tzuhsien.pinpisode.util.SharingLinkGenerator
import com.tzuhsien.pinpisode.util.SwipeHelper
import timber.log.Timber


class SpotifyNoteFragment : Fragment() {
    private val viewModel by viewModels<SpotifyNoteViewModel> {
        getVmFactory(
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).noteIdKey,
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).sourceIdKey
        )
    }
    private lateinit var binding: FragmentSpotifyNoteBinding
    private lateinit var trackProgressBar: TrackProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSpotifyNoteBinding.inflate(layoutInflater)

        viewModel.isSpotifyConnected.observe(viewLifecycleOwner) { it ->
            Timber.d("viewModel.isSpotifyConnected.observe: $it")
            viewModel.clearConnectionErrorMsg()
            binding.seekTo.isEnabled = it
            binding.playPauseButton.isEnabled = it
            binding.seekBackButton.isEnabled = it
            binding.seekForwardButton.isEnabled = it
            binding.btnClip.isEnabled = it
            binding.btnTakeTimestamp.isEnabled = it

            if (it) {
                SpotifyService.play(SPOTIFY_URI + viewModel.sourceId)
                Timber.d("SpotifyService.play(${SPOTIFY_URI + viewModel.sourceId})")

                SpotifyService.subscribeToPlayerState { state ->
                    Timber.d("SpotifyService.subscribeToPlayerState:  $state ")
                    updateSeekbar(state)
                    updatePlayPauseButton(state)

                    viewModel.updateCurrentPosition(state.playbackPosition)
                    viewModel.startTrackingPosition()

                    SpotifyService.getCoverArt(state.track.imageUri) {
                        binding.imgCoverArt.setImageBitmap(it)
                    }
                    binding.textSourceTitle.text = state.track.name
                    binding.textPublisher.text = if (state.track.isPodcast) {
                        state.track.album.name
                    } else {
                        state.track.artist.name
                    }

                    binding.textTotalTime.text = state.track.duration.formatDuration()

                    if (state.track.uri.extractSpotifySourceId() == viewModel.sourceId) {
                        // Update new info when the player is playing the assigned uri
                        viewModel.newSpotifyNote = Note(
                            source = Source.SPOTIFY.source,
                            sourceId = viewModel.sourceId,
                            tags = listOf(Source.SPOTIFY.source),
                            title = state.track.name,
                            thumbnail = state.track.imageUri.raw.parseSpotifyImageUri(),
                            duration = state.track.duration.toString()
                        )
                        viewModel.invokeCreateNewNoteLiveData()
                    }

                    if (state.isPaused) {
                        // stop the timer from keep updating time
                        viewModel.pauseTrackingPosition()
                    } else {
                        // track current playing position in real time
                        viewModel.unpauseTrackingPosition()
                    }
                }


                Timber.d("viewModel.isViewerCanEdit = ${viewModel.isViewerCanEdit}")
                if (viewModel.isViewerCanEdit) {
                    Intent(context, SpotifyNoteService::class.java).apply {
                        action = SpotifyNoteService.ACTION_START
                        context?.startService(this)
                    }
                    registerTimestampReceiver()
                } else {

                    Timber.d(" viewModel.isSpotifyConnected.observe, ACTION_STOP")
                    Intent(context, SpotifyNoteService::class.java).apply {
                        action = SpotifyNoteService.ACTION_STOP
                        context?.startService(this)
                    }
                }
            }
        }

        viewModel.shouldCreateNewNote.observe(viewLifecycleOwner) { shouldCreateNote ->
            Timber.d("viewModel.shouldCreateNewNote.observe: $shouldCreateNote, note: ${viewModel.newSpotifyNote}")
            if (viewModel.newSpotifyNote.sourceId == viewModel.sourceId) {
                if (shouldCreateNote) {
                    viewModel.createNewSpotifyNote(viewModel.newSpotifyNote)
                    viewModel.hasUploaded = true
                }
            }
        }

        /**
         * Bind player control
         * **/
        binding.playPauseButton.setOnClickListener {
            Timber.d("binding.playPauseButton.setOnClickListener, ${viewModel.playingState}")
            when (viewModel.playingState) {

                PlayingState.STOPPED -> { viewModel.playingState = PlayingState.PLAYING }

                PlayingState.PLAYING -> pause()

                PlayingState.PAUSED -> resume()
            }
        }

        binding.seekBackButton.setOnClickListener { seekBack() }

        binding.seekForwardButton.setOnClickListener { seekForward() }

        /**  Seek bar  **/
        trackProgressBar =
            TrackProgressBar(binding.seekTo) { seekToPosition: Long -> seekTo(seekToPosition) }

        /** Take timestamps / clips buttons **/
        binding.btnTakeTimestamp.setOnClickListener {
            Timber.d("binding.btnTakeTimestamp.setOnClickListener clicked")
            takeTimestamp()
        }

        binding.btnClip.setOnClickListener {
            when (viewModel.startOrStopToggle) {
                0 -> {
                    startClipping()
                    Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                }
                1 -> {
                    endClipping()
                    Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")
                }
            }
        }

        /**
         *  Play the time items
         * **/
        viewModel.playStart.observe(viewLifecycleOwner) { startAt ->
            startAt?.let {
                seekTo(startAt)
                resume()
            }
        }

        viewModel.currentPosition.observe(viewLifecycleOwner) { currentSec ->
            // Update current position text
            binding.textCurrentSecond.text = currentSec.formatDuration()

            viewModel.playEnd?.let {
                if (it <= currentSec) {
                    pause()
                    viewModel.clearPlayingMomentStart()
                    viewModel.clearPlayingMomentEnd()
                }
            }
        }

        /**
         * Digest of the video (editText)
         * */
        viewModel.isLiveNoteReady.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.liveNote.observe(viewLifecycleOwner) { note ->
                    note?.let {
                        binding.editDigest.setText(note.digest)
                        viewModel.noteToBeUpdated = note
                    }
                }
                binding.editDigest.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        viewModel.noteToBeUpdated?.digest = binding.editDigest.text.toString()
                        viewModel.updateNote()
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

        val adapter = SpotifyTimeItemAdapter(uiState = viewModel.uiState)

        binding.recyclerViewTimeItems.adapter = adapter

        viewModel.isLiveTimeItemListReady.observe(viewLifecycleOwner) { timeItemLiveDataAssigned ->
            if (timeItemLiveDataAssigned == true) {
                viewModel.liveTimeItemList.observe(viewLifecycleOwner) { list ->
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
                }
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
                    NavGraphDirections.actionGlobalCoauthorDialogFragment(it))
            }
        }

        /**
         *  Buttons on the bottom of the page: Share this note
         * */
        binding.icShare.setOnClickListener { shareNoteLink() }

        /**
         * Toast to warn about Spotify error
         * */
        viewModel.connectErrorMsg.observe(viewLifecycleOwner) {
            if (null != it) {
                binding.errorMsg.text = it
                binding.errorMsg.visibility = View.VISIBLE
            } else {
                binding.errorMsg.visibility = View.GONE
            }
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when(it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(NavGraphDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_DISMISS,
                        bundleOf(KEY_DONE_LOADING to true))
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
            DYNAMIC_LINK_PREFIX + DEEPLINK_PATH_SPOTIFY_NOTE + viewModel.noteId + "/" + viewModel.sourceId

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
                    "Spotify podcast",
                    viewModel.noteToBeUpdated?.title)
            )
            putExtra(Intent.EXTRA_TEXT, dynamicLink)
        }

        startActivity(Intent.createChooser(intent, null))
    }

    private fun updateSeekbar(playerState: PlayerState) {
        // Update progressbar
        trackProgressBar.apply {
            if (playerState.playbackSpeed > 0) {
                unpause()
            } else {
                pause()
            }
            // Invalidate seekbar length and position
            binding.seekTo.max = playerState.track.duration.toInt()
            binding.seekTo.isEnabled = true
            setDuration(playerState.track.duration)
            update(playerState.playbackPosition)
        }
    }

    private fun updatePlayPauseButton(playerState: PlayerState) {
        if (playerState.isPaused) {
            viewModel.playingState = PlayingState.PAUSED
            binding.playPauseButton.setImageResource(R.drawable.btn_play)
        } else {
            viewModel.playingState = PlayingState.PLAYING
            binding.playPauseButton.setImageResource(R.drawable.btn_pause)
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

    // Receive action on notification
    private val timestampReceiver = TimestampReceiver(TimestampReceiver.OnActionListener {
        when (it) {
            TimestampReceiver.ACTION_TAKE_TIMESTAMP -> takeTimestamp()
            TimestampReceiver.ACTION_CLIP_START -> startClipping() // save clipStartSec
            TimestampReceiver.ACTION_CLIP_END -> endClipping() // createTimeItem
        }
    })

    private fun registerTimestampReceiver() {
        val filter = IntentFilter().apply {
            addAction(TimestampReceiver.ACTION_TAKE_TIMESTAMP)
            addAction(TimestampReceiver.ACTION_CLIP_START)
            addAction(TimestampReceiver.ACTION_CLIP_END)
        }
        context?.registerReceiver(timestampReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        SpotifyService.disconnect()
        Timber.d("onDestroy, ACTION_STOP")
        Intent(context, SpotifyNoteService::class.java).apply {
                action = SpotifyNoteService.ACTION_STOP
                context?.startService(this)
        }

        try {
            context?.unregisterReceiver(timestampReceiver)
        } catch (e: IllegalArgumentException) {
            // Do nothing if the receiver has not been registered
        }

        Timber.d("onDestroy() CALLED")
    }

    /**
     *  Take timestamp / clip
     * */
    private fun takeTimestamp() {
        Timber.d("takeTimestamp")
        viewModel.createTimeItem((viewModel.currentSecond / 1000).toFloat(), null)
    }

    private fun startClipping() {
        Timber.d("clipStart")
        viewModel.startAt = (viewModel.currentSecond / 1000).toFloat()

        val animation = AnimationUtils.loadAnimation(context, R.anim.ic_clipping)

        binding.btnClip.apply {
            startAnimation(animation)
        }
        viewModel.startOrStopToggle = 1

        Intent(context, SpotifyNoteService::class.java).apply {
            this.action = SpotifyNoteService.ACTION_START_CLIPPING
            context?.startService(this)
        }
    }

    private fun endClipping() {
        Timber.d("clipEnd")
        if ((viewModel.currentSecond / 1000).toFloat() < viewModel.startAt) {
            Toast.makeText(context,
                getString(R.string.clip_end_before_start_warning),
                Toast.LENGTH_SHORT).show()
        } else {
            viewModel.endAt = (viewModel.currentSecond / 1000).toFloat()
            binding.btnClip.animation = null
            viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
            viewModel.startOrStopToggle = 0

            Intent(context, SpotifyNoteService::class.java).apply {
                this.action = SpotifyNoteService.ACTION_DONE_CLIPPING
                context?.startService(this)
            }
        }
    }
}