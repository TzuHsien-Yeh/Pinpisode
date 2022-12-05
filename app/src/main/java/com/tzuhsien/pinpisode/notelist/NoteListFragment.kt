package com.tzuhsien.pinpisode.notelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Sort
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.FragmentNoteListBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.loading.LoadingDialogDirections
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.notification.NotificationFragmentDirections
import com.tzuhsien.pinpisode.signin.SignInFragmentDirections
import com.tzuhsien.pinpisode.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.pinpisode.util.SwipeHelper
import com.tzuhsien.pinpisode.youtubenote.YouTubeNoteFragmentDirections
import kotlinx.coroutines.*
import timber.log.Timber

class NoteListFragment : Fragment() {

    private val viewModel by viewModels<NoteListViewModel> {
        getVmFactory()
    }
    private lateinit var binding: FragmentNoteListBinding
    private val scrollJob = Job()
    val coroutineScope = CoroutineScope(scrollJob + Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        /**
         * Check and handle sign in status
         * */
        if (null == GoogleSignIn.getLastSignedInAccount(requireContext())) {
            findNavController().navigate(SignInFragmentDirections.actionGlobalSignInFragment())
        } else {
            viewModel.updateLocalUserId()
        }

        binding = FragmentNoteListBinding.inflate(layoutInflater)

        /**
         * Update YouTube live event duration from yt api
         * */
        viewModel.liveNoteList.observe(viewLifecycleOwner) {
            for (note in it) {
                viewModel.updateInfoFromYouTube(note)
            }
        }

        /**
         * Tag list
         * */
        val tagAdapter = TagAdapter(onClickListener = TagAdapter.OnTagClickListener {
            binding.textSelectedTag.text = it
            binding.cardSelectedTag.visibility = View.VISIBLE
            viewModel.tagSelected(it)
        })
        binding.recyclerviewTag.adapter = tagAdapter
        viewModel.tagsToDisplay.observe(viewLifecycleOwner) { set ->
            tagAdapter.submitList(set)
        }
        binding.cardSelectedTag.setOnClickListener {
            it.visibility = View.GONE
            viewModel.tagSelected(null)
        }

        /**
         * Sorting and ordering
         * */
        binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
        binding.sortAsc.alpha = 1F
        binding.sortDesc.alpha = 0.5F
        binding.cardSortBy.setOnClickListener {
            when (viewModel.sortOption) {
                Sort.LAST_EDIT -> {
                    // Change to sort by duration onClick
                    binding.textSortOptions.text = Sort.DURATION.VALUE
                    binding.sortAsc.alpha = 1F
                    binding.sortDesc.alpha = 0.5F
                    viewModel.sort(Sort.DURATION)
                }
                Sort.DURATION -> {
                    binding.textSortOptions.text = Sort.TIME_LEFT.VALUE
                    binding.sortAsc.alpha = 0.5F
                    binding.sortDesc.alpha = 1F
                    viewModel.sort(Sort.TIME_LEFT)
                }
                Sort.TIME_LEFT -> {
                    binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
                    binding.sortAsc.alpha = 1F
                    binding.sortDesc.alpha = 0.5F
                    viewModel.sort(Sort.LAST_EDIT)
                }
            }
        }
        binding.btnSwitchDirection.setOnClickListener {
            if (viewModel.isAscending) {
                //change to DESC
                binding.sortAsc.alpha = 1F
                binding.sortDesc.alpha = 0.5F
                viewModel.changeSortDirection()
            } else {
                //change to ASC
                binding.sortAsc.alpha = 0.5F
                binding.sortDesc.alpha = 1F
                viewModel.changeSortDirection()
            }
        }

        /**
         * Note list
         * */
        val noteAdapter = NoteAdapter(uiState = viewModel.uiState)
        binding.recyclerviewNoteList.adapter = noteAdapter

        // Swipe to delete
        binding.recyclerviewNoteList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(
            binding.recyclerviewNoteList,
            swipeOutListener = OnSwipeOutListener {
                viewModel.deleteOrQuitCoauthoringNote(it)
            }
        ) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                return listOf(deleteButton)
            }

        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerviewNoteList)

        viewModel.noteListToDisplay.observe(viewLifecycleOwner) { list ->
            noteAdapter.submitList(list)
            coroutineScope.launch {
                delay(200L)
                binding.recyclerviewNoteList.scrollToPosition(0)
            }
        }

        /**
         *  Navigation
         * */
        // Profile page
        Glide.with(binding.imgPicToProfile)
            .load(UserManager.userPic)
            .into(binding.imgPicToProfile)
        binding.textUserName.text = UserManager.userName
        binding.imgPicToProfile.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToProfileFragment())
        }
        binding.textUserName.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToProfileFragment())
        }

        // Search page
        binding.btnToSearchPage.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToSearchFragment())
        }

        // Notification page
        binding.btnNotificationBell.setOnClickListener {
            findNavController().navigate(NotificationFragmentDirections.actionGlobalNotificationFragment())
        }
        /** Show badge if there's any incoming coauthor invitations **/
        viewModel.invitationList.observe(viewLifecycleOwner) {
            binding.badgeNotificationNotEmpty.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(
                    YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(
                        noteIdKey = it.id,
                        videoIdKey = it.sourceId
                    )
                )
                viewModel.doneNavigationToNote()
            }
        })

        viewModel.navigateToSpotifyNote.observe(viewLifecycleOwner) {
            it?.let {
                findNavController().navigate(
                    SpotifyNoteFragmentDirections.actionGlobalSpotifyNoteFragment(
                        noteIdKey = it.id,
                        sourceIdKey = it.sourceId
                    )
                )
                viewModel.doneNavigationToNote()
            }
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            Timber.d("viewModel.status.observe: $it")
            when(it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(LoadingDialogDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    Timber.d("LoadApiStatus.DONE")
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to true))
                }
                LoadApiStatus.ERROR -> {
                    Timber.d("LoadApiStatus.ERROR")
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to false))
                }
            }
        }

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        scrollJob.cancel()
    }

    fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            MyApplication.applicationContext(),
            getString(R.string.quit),
            16.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    viewModel.deleteOrQuitCoauthoringNote(position)
                }
            })
    }

}
