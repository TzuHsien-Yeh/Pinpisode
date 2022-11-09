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
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
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
        /**
         *  Read intent data
         * */
        if (null != intent) {
            val extras: Bundle? = intent?.extras
            val sourceId = extras?.getString(Intent.EXTRA_TEXT)?.extractYoutubeVideoId()

            Timber.d("intent extras : ${extras?.getString(Intent.EXTRA_TEXT)}, $sourceId")

            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            navController.navigate(
                YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(videoIdKey = sourceId!!)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize timber
        Timber.plant(Timber.DebugTree())


        val navView: BottomNavigationView = binding.bottomNavView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController) // make items show their status as selected

        viewModel.currentUser.observe(this){
            if (null == it) {
                navController.navigate(SignInFragmentDirections.actionGlobalSignInFragment())
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

                R.id.youTubeNoteFragment -> {
                    navView.visibility = View.GONE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {

                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                        binding.toolbar.navigationIcon = null
                        binding.toolbarText.text = getString(R.string.pin_your_episodes)
                        binding.bottomNavView.visibility = View.VISIBLE
                    }
                    binding.toolbarText.text = getString(R.string.youtube_note)
                }
            }
        }

        navView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {

                R.id.navigation_note_list -> {
                    navController.navigate(R.id.noteListFragment)
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = getString(R.string.pin_your_episodes)
                    binding.bottomNavView.visibility = View.VISIBLE
                    true
                }

                R.id.navigation_search -> {
                    navController.navigate(R.id.searchFragment)
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = getString(R.string.find_your_episodes)
                    true
                }

                R.id.navigation_profile -> {
                    // navigate to profile
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = getString(R.string.profile)
                    true
                }

                else -> false
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
}
