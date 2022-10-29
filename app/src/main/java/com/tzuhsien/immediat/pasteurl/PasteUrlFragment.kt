package com.tzuhsien.immediat.pasteurl

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.FragmentPasteUrlBinding

class PasteUrlFragment : DialogFragment() {

    private lateinit var viewModel: PasteUrlViewModel
    private lateinit var binding: FragmentPasteUrlBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPasteUrlBinding.inflate(layoutInflater)

        binding.searchviewPasteUrl.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        if (query.contains("https://www.youtube.com/")) {
                            // if query exist contains youtube
                            binding.imageYoutubeLogo.visibility = View.VISIBLE

                            // TODO: call YT api to get the video info

                        } else {
                            binding.imageYoutubeLogo.visibility = View.GONE
                            Toast.makeText(context, "Not a valid link", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    TODO("Not yet implemented")
                }

            }
        )

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PasteUrlViewModel::class.java)
        // TODO: Use the ViewModel
    }

}