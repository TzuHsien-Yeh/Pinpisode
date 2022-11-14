package com.tzuhsien.immediat.coauthor

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.databinding.FragmentCoauthorDialogBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.tag.TagDialogFragmentArgs

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
            Glide.with(binding.imgOwnerPic)
                .load(it?.pic)
                .into(binding.imgOwnerPic)
        }

        if (UserManager.userId == viewModel.note.ownerId) {
            binding.searchUserByEmail.visibility = View.VISIBLE
            binding.textAddCoauthors.visibility = View.VISIBLE
            binding.textOnlyOwnerCanInviteCoauthors.visibility = View.GONE
        } else {
            binding.searchUserByEmail.visibility = View.GONE
            binding.textAddCoauthors.visibility = View.GONE
            binding.textOnlyOwnerCanInviteCoauthors.visibility = View.VISIBLE
        }

        binding.searchUserByEmail.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        viewModel.findUserByEmail(query)
                    }
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    return false
                }
            }
        )

        binding.textNotFound.visibility = View.GONE
        binding.viewGroupUserSearchResult.visibility = View.GONE

        viewModel.foundUser.observe(viewLifecycleOwner) {
            if (null != it) {
                binding.viewGroupUserSearchResult.visibility = View.VISIBLE
                binding.textNotFound.visibility = View.GONE
                binding.textSearchResultName.text = it.name
                binding.textSearchResultEmail.text = it.email

                Glide.with(binding.imgSearchResultPic)
                    .load(it.pic)
                    .into(binding.imgSearchResultPic)

            } else {
                binding.textNotFound.visibility = View.VISIBLE
                binding.viewGroupUserSearchResult.visibility = View.GONE
            }
        }

        binding.viewGroupUserSearchResult.setOnClickListener {
            viewModel.addUserAsCoauthor()
            binding.viewGroupUserSearchResult.visibility = View.GONE
            viewModel.resetFoundUser()
        }

        viewModel.addSuccess.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, "Add Success", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }


}