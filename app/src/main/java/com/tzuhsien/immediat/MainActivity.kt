package com.tzuhsien.immediat

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.AnnotationsProto.http
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.databinding.ActivityMainBinding
import com.tzuhsien.immediat.ext.extractYoutubeVideoId
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.notelist.NoteListFragmentDirections
import com.tzuhsien.immediat.signin.SignInFragmentDirections
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel> { getVmFactory() }
    private lateinit var binding: ActivityMainBinding

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        /** Read intent data **/

        Timber.d("onNewIntent CALLED")
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        intent?.data?.let {
            if (it.toString().contains("http://pinpisode/")) {
                navController.handleDeepLink(intent)
                Timber.d("[onNewIntent] handleDeepLink called")
            }
        }

        intent?.extras?.let {
            handleIntent(it)
            Timber.d("[onNewIntent] handleIntent called")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize timber
        Timber.plant(Timber.DebugTree())

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        /** Read intent data if the activity has not been created when getting intent from other app **/

        Timber.d("onCreate CALLED")
        Timber.d("intent.data: ${intent.data}, ${intent.data?.host}, ${intent.data?.query}")
        Timber.d("intent.extra: ${intent.extras?.getString(Intent.EXTRA_TEXT)}")

        if (null != intent) {
            intent.data?.let {
                if (it.toString().contains("http://pinpisode/")) {
                    navController.handleDeepLink(intent)
                    Timber.d("[onCreate] handleDeepLink called")
                }
            }

            intent.extras?.let {
                handleIntent(it)
                Timber.d("[onCreate] handleIntent called")
            }
        }

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.toolbar.visibility= View.VISIBLE
        binding.toolbar.navigationIcon = null
        binding.toolbarText.text = getString(R.string.pin_your_episodes)

        navController.addOnDestinationChangedListener{controller, destination, arguments->
            when(destination.id){

                R.id.noteListFragment -> {
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = getString(R.string.pin_your_episodes)
                }

                R.id.searchFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.find_your_episodes)
                }

                R.id.profileFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.profile)
                }

                R.id.youTubeNoteFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.youtube_note)
                }

                R.id.signInFragment -> {
                    binding.toolbar.visibility = View.GONE
                }

            }
        }

    }

    /**
     *  EditText behavior in MainActivity (exit edit mode when the user touched other places)
     * */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handleIntent(intentExtras: Bundle){

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        /**
         * Check and handle sign in status
         * */
        if (null == GoogleSignIn.getLastSignedInAccount(applicationContext)) {
            Timber.d("[${this::class.simpleName}]: null == GoogleSignIn.getLastSignedInAccount(applicationContext)")
            navController.navigate(SignInFragmentDirections.actionGlobalSignInFragment())
        } else {
            viewModel.updateLocalUserId()
        }

        if (UserManager.userId != null) {
            val sourceId = intentExtras.getString(Intent.EXTRA_TEXT)?.extractYoutubeVideoId()
            if (null != sourceId){
                Timber.d("HANDLE INTENT FUN intent extras : $sourceId")

                navController.navigate(
                    YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(videoIdKey = sourceId)
                )
            }
        }

    }
}
