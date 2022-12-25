package com.tzuhsien.pinpisode.loading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.DialogLoadingBinding
import timber.log.Timber



class LoadingDialog : AppCompatDialogFragment() {

    companion object {
        const val REQUEST_DISMISS = "dismissRequest"
        const val KEY_DONE_LOADING = "doneLoading"
        const val REQUEST_ERROR = "errorRequest"
        const val KEY_ERROR_MSG = "errorMsg"
    }

    private lateinit var binding: DialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TagDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DialogLoadingBinding.inflate(layoutInflater)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            REQUEST_DISMISS,
         this,
        ) { _, bundle ->

            Timber.d("setFragmentResultListener bundle: ${bundle.getBoolean(KEY_DONE_LOADING)}")
            when (bundle.getBoolean(KEY_DONE_LOADING)) {
                true -> {
                    dismiss()
                }
                false -> {
                    binding.imgLoading.visibility = View.GONE
                    binding.lottieLoading.visibility = View.GONE
                    binding.imgError.visibility = View.VISIBLE
                    Timber.d("ERROR")
                    binding.root.setOnClickListener {
                        findNavController().navigate(NavGraphDirections.actionGlobalNoteListFragment())
                    }
                }
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            REQUEST_ERROR,
            this,
        ) { _, bundle ->
            binding.textErrorMsg.visibility = View.VISIBLE
            binding.textErrorMsg.text = bundle.getString(KEY_ERROR_MSG)
        }

        return binding.root
    }

    override fun dismiss() {
        super.dismiss()
        Timber.d("loading dialog dismissed")
    }

}