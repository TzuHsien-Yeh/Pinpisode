package com.tzuhsien.pinpisode.loading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.DialogLoadingBinding
import timber.log.Timber


const val REQUEST_KEY_DISMISS = "dismissRequest"
const val BUNDLE_KEY_DONE_LOADING = "doneLoading"

class LoadingDialog : AppCompatDialogFragment() {

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
            REQUEST_KEY_DISMISS,
         this,
        ) { _, bundle ->

            Timber.d("setFragmentResultListener bundle: ${bundle.getBoolean(BUNDLE_KEY_DONE_LOADING)}")
            when (bundle.getBoolean(BUNDLE_KEY_DONE_LOADING)) {
                true -> {
                    dismiss()
                }
                false -> {
                    Timber.d("ERROR")
                }
            }
        }

        return binding.root
    }

    override fun dismiss() {
        super.dismiss()
        Timber.d("loading dialog dismissed")
    }

}