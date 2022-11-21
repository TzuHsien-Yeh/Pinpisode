package com.tzuhsien.immediat.signin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.FragmentSignInBinding
import com.tzuhsien.immediat.ext.getVmFactory
import timber.log.Timber


class SignInFragment : Fragment() {
    companion object {
        const val GOOGLE_SIGN_IN = 1903
    }

    private val viewModel by viewModels<SignInViewModel> { getVmFactory() }
    private lateinit var binding: FragmentSignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(requireContext(), getGSO())

        binding = FragmentSignInBinding.inflate(layoutInflater)
        binding.btnSignIn.setOnClickListener { signIn() }
        auth = Firebase.auth

        viewModel.navigateUp.observe(viewLifecycleOwner) {
            it?.let {
                findNavController().navigate(SignInFragmentDirections.actionGlobalNoteListFragment())
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        updateUI(account)
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (null != account) {
            findNavController().navigateUp()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == GOOGLE_SIGN_IN) {
            val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(completedTask)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)

//            youtubeAuth(account)

        } catch (e: ApiException) {
            Timber.d("Google Sign in exception: ${e.statusCode}")
            Toast.makeText(context, "Sign in failed", Toast.LENGTH_LONG).show()
        }
    }

//    private fun youtubeAuth(account: GoogleSignInAccount) {
//        val authToken = GoogleAuthUtil.getToken(
//            MyApplication.applicationContext(),
//            account.account!!,
//            "oauth2:https://www.googleapis.com/auth/youtube",
//            Bundle()
//        )
//        Timber.d("authToken: $authToken")
//    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    viewModel.updateUser(task.result.user!!, account)
                } else {
                    //handle error
                    Timber.d("firebaseAuthWithGoogle: Failed!")
                }
            }
    }

    private fun getGSO(): GoogleSignInOptions {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        return  GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestScopes(Scope("https://www.googleapis.com/auth/youtube"))
            .requestEmail()
            .build()
    }

}