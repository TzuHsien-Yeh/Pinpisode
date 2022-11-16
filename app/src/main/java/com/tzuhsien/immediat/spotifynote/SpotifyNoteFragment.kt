package com.tzuhsien.immediat.spotifynote

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.spotify.protocol.types.PlayerState
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.coauthor.CoauthorDialogFragmentDirections
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.data.model.TimeItemDisplay
import com.tzuhsien.immediat.databinding.FragmentSpotifyNoteBinding
import com.tzuhsien.immediat.ext.formatDuration
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.parseSpotifyImageUri
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekBack
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekForward
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekTo
import com.tzuhsien.immediat.tag.TagDialogFragmentDirections
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

    override fun onDestroy() {
        super.onDestroy()
        SpotifyService.disconnect()
        Timber.d("onDestroy(): SpotifyService.disconnect()")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSpotifyNoteBinding.inflate(layoutInflater)

        viewModel.isSpotifyConnected.observe(viewLifecycleOwner) { it ->
            binding.playPauseButton.isEnabled = it
            binding.seekBackButton.isEnabled = it
            binding.seekForwardButton.isEnabled = it
            binding.btnClip.isEnabled = it
            binding.btnTakeTimestamp.isEnabled = it

            if (it) {
                SpotifyService.play(SPOTIFY_URI + viewModel.sourceId)

                SpotifyService.subscribeToPlayerState { state ->
                    updateSeekbar(state)
                    updatePlayPauseButton(state)
                    viewModel.updateCurrentPosition(state.playbackPosition)
                    viewModel.startTrackingPosition()

                    SpotifyService.getCoverArt(state.track.imageUri) {
                        binding.imgCoverArt.setImageBitmap(it)
                    }
                    binding.textSourceTitle.text = state.track.name
                    binding.textTotalTime.text = state.track.duration.formatDuration()

                    if (!state.isPaused) {
                        // Update new info after the playerState catching up with the current playing track
                        viewModel.updateNewInfo(state)

                        // track current playing position in real time
                        viewModel.unpauseTrackingPosition()
                    } else {
                        // stop the timer from keep updating time
                        viewModel.pauseTrackingPosition()
                    }

                }

            }

        }

        viewModel.getInfoFromPlayerState.observe(viewLifecycleOwner) {
            it?.let {

                viewModel.shouldCreateNewNote.observe(viewLifecycleOwner) { shouldCreateNote ->
                    SpotifyService.getCurrentTrack { track ->
                        val newSpotifyNote = Note(
                            source = Source.SPOTIFY.source,
                            sourceId = viewModel.sourceId,
                            tags = listOf(Source.SPOTIFY.source),
                            title = track.name,
                            thumbnail = track.imageUri.raw!!.parseSpotifyImageUri(),
                            duration = track.duration.toString()
                        )

                        if (shouldCreateNote) {
                            viewModel.createNewSpotifyNote(newSpotifyNote)
                            viewModel.createNewNoteFinished()
                        }
                    }

                }
            }

        }

        /**
         * Bind player control
         * **/
        binding.playPauseButton.setOnClickListener {
            when (viewModel.playingState) {
                PlayingState.STOPPED -> {
                    viewModel.playingState = PlayingState.PLAYING
                }

                PlayingState.PLAYING -> {
                    SpotifyService.pause()
                }

                PlayingState.PAUSED -> {
                    SpotifyService.resume()
                }
            }
        }

        binding.seekBackButton.setOnClickListener {
            seekBack()
        }
        binding.seekForwardButton.setOnClickListener {
            seekForward()
        }

        /**  Seek bar  **/
        binding.seekTo.apply {
            this.isEnabled = false
            progressDrawable.setColorFilter(Color.parseColor("#ff4664"), PorterDuff.Mode.SRC_ATOP)
            indeterminateDrawable.setColorFilter(Color.parseColor("#ff4664"),
                PorterDuff.Mode.SRC_ATOP)
        }
        trackProgressBar =
            TrackProgressBar(binding.seekTo) { seekToPosition: Long -> seekTo(seekToPosition) }


        /**
         *  Update UI according to current position
         * **/
        viewModel.currentPosition.observe(viewLifecycleOwner) { currentSec ->

            Timber.d("viewModel.currentPosition.observe: $currentSec")
            // Text of time to show progress
            binding.textCurrentSecond.text = currentSec.formatDuration()

            /**
             *  Take timestamps / clips buttons
             * */
            binding.btnTakeTimestamp.setOnClickListener {
                Timber.d("binding.btnTakeTimestamp.setOnClickListener clicked")
                viewModel.createTimeItem((currentSec/1000).toFloat(), null)
            }
            binding.btnClip.setOnClickListener {
                when (viewModel.startOrStopToggle) {
                    0 -> {
                        viewModel.startAt = (currentSec/1000).toFloat()
                        viewModel.startOrStopToggle = 1
                        binding.btnClip.setImageResource(R.drawable.ic_clipping_stop)
                        Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                    }
                    1 -> {
                        viewModel.endAt = (currentSec/1000).toFloat()
                        Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")

                        binding.btnClip.setImageResource(R.drawable.ic_clip)
                        viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
                        viewModel.startOrStopToggle = 0
                    }
                }
            }
        }

        /**
         *  Play the time items
         * **/
        viewModel.playStart.observe(viewLifecycleOwner) { startAt ->
            startAt?.let {
                SpotifyService.seekTo((it * 1000).toLong())
                SpotifyService.resume()
                viewModel.clearPlayingMomentStart()
                Timber.d("viewModel.playStart.observe startAt: $startAt")
            }
        }

        // count down to stop if it is a clip
        viewModel.clipLength.observe(viewLifecycleOwner) { clipLength ->

            if (null != clipLength) {
                object : CountDownTimer( (clipLength * 1000).toLong(), 500) {
                    override fun onTick(millisUntilFinished: Long) {
                        viewModel.clearPlayingMomentEnd()
                        Timber.d("countDownTimer onTick, clip length: $clipLength")
//                        // cancel the countDownTimer once another time item clicked
//                        viewModel.playStart.observe(viewLifecycleOwner) {
//
//                            Timber.d("viewModel.playStart in countDownTimer : $it")
//                            it?.let {
//                                cancel()
//                                Timber.d("countDownTimer canceled: viewModel.playStart: $it")
//                            }
//                        }
                    }

                    // Callback function, fired when the time is up
                    override fun onFinish() {
                        SpotifyService.pause()
                    }
                }.start()

            }
        }


        /**
         * Digest of the video (editText)
         * */
        binding.editDigest.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.noteToBeUpdated?.digest = binding.editDigest.text.toString()
                viewModel.updateNote()
            }
        }
        viewModel.liveNoteData.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.editDigest.setText(note.digest)
                viewModel.noteToBeUpdated = note
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
        val adapter = SpotifyTimeItemAdapter(
            uiState = viewModel.uiState
        )
        binding.recyclerViewTimeItems.adapter = adapter

        viewModel.timeItemLiveDataReassigned.observe(viewLifecycleOwner) { timeItemLiveDataAssigned ->
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
            findNavController().navigate(TagDialogFragmentDirections.actionGlobalTagDialogFragment(
                viewModel.noteToBeUpdated!!))
        }

        /**
         *  Buttons on the bottom of the page: Coauthoring
         * */
        binding.icCoauthoring.setOnClickListener {
            findNavController().navigate(
                CoauthorDialogFragmentDirections.actionGlobalCoauthorDialogFragment(
                    viewModel.noteToBeUpdated!!
                )
            )
        }

        /**
         *  Buttons on the bottom of the page: Share this note
         * */
        binding.icShare.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.spotify_note_uri, viewModel.noteId, viewModel.sourceId))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.send_to)))
        }

        return binding.root
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

    /**
     *  Get player position every 0.5 sec
     * **/
    ///
//    private val timer = Timer()
//
//    fun startTick(){
//        timer.scheduleAtFixedRate(timerTask {
//            viewModel.currentSecond += 500L
//            SpotifyService.pause()
//            SpotifyService.resume()
//        }, 0, 500)
//    }
//
//    fun pauseTick(){
//        timer.cancel()
//    }
//
//    fun updateCurrentPositionFromPlayer(playerState: PlayerState){
//        viewModel.currentSecond = playerState.playbackPosition
//    }


}