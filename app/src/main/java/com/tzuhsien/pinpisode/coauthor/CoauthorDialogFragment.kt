package com.tzuhsien.pinpisode.coauthor

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.FragmentCoauthorDialogBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.glide
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.LoadingDialog.Companion.REQUEST_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.tag.TagDialogFragmentArgs

class CoauthorDialogFragment : DialogFragment() {

    private val viewModel by viewModels<CoauthorViewModel> { getVmFactory(TagDialogFragmentArgs.fromBundle(requireArguments()).noteKey) }
    private lateinit var binding: FragmentCoauthorDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentCoauthorDialogBinding.inflate(layoutInflater)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val adapter = AuthorAdapter()
        binding.recyclerviewAuthors.adapter = adapter
        viewModel.liveCoauthorInfo.observe(viewLifecycleOwner) { authorList ->
            authorList?.let { list ->
                adapter.submitList(list)
            }
        }

        viewModel.noteOwner.observe(viewLifecycleOwner) {
            binding.imgOwnerPic.glide(it.pic)
        }

        binding.textCoauthors.text = when (viewModel.note.authors.size) {
            1 -> null
            2 -> getString(R.string.coauthor)
            else -> getString(R.string.coauthors)
        }

        binding.searchUserByEmail.visibility = if (viewModel.isUserTheOwner) View.VISIBLE else View.GONE
        binding.textInviteCoauthors.visibility = if (viewModel.isUserTheOwner) View.VISIBLE else View.GONE
        binding.textQuitCoauthoring.visibility = if (viewModel.isUserTheOwner) View.GONE else View.VISIBLE

        binding.textQuitCoauthoring.setOnClickListener {
            viewModel.quitCoauthoringTheNote()
        }

        viewModel.quitCoauthoringResult.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                findNavController().navigate(NavGraphDirections.actionGlobalNoteListFragment())
            }
        }

        binding.searchUserByEmail.setOnQueryTextListener(
            object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        // remove leading and trailing whitespace by .trim()
                        viewModel.findUserByEmail(query.trim())
                    }
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    return false
                }
            }
        )

        viewModel.foundUser.observe(viewLifecycleOwner) {
             it?.let {
                binding.viewGroupUserSearchResult.visibility = View.VISIBLE
                binding.textSearchResultName.text = it.name
                binding.textSearchResultEmail.text = it.email
                binding.imgSearchResultPic.glide(it.pic)
                binding.textResultMsg.visibility = View.GONE
            }
        }

        binding.viewGroupUserSearchResult.setOnClickListener {
            viewModel.sendCoauthorInvitation()
            binding.viewGroupUserSearchResult.visibility = View.GONE
        }

        viewModel.resultMsg.observe(viewLifecycleOwner) {
            binding.textResultMsg.visibility = if (null != it) View.VISIBLE else View.GONE
            it?.let { binding.textResultMsg.text = it }
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

}