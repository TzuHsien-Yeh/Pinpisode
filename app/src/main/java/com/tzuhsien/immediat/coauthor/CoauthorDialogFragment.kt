package com.tzuhsien.immediat.coauthor

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.DialogTagBinding
import com.tzuhsien.immediat.databinding.FragmentCoauthorDialogBinding
import com.tzuhsien.immediat.databinding.FragmentYoutubeNoteBinding
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
        }

        viewModel.addSuccess.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(context, "Add Success", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }


}