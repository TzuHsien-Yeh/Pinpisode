package com.tzuhsien.immediat

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tzuhsien.immediat.databinding.ActivityMainBinding
import com.tzuhsien.immediat.notelist.NoteListFragmentDirections
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize timber
        Timber.plant(Timber.DebugTree())

        val navView: BottomNavigationView = binding.bottomNavView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController) // make items show their status as selected

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.toolbar.navigationIcon = null

        navController.addOnDestinationChangedListener{controller, destination, arguments->
            when(destination.id){
                R.id.navigation_note_list -> {
                    navView.visibility= View.VISIBLE
                    binding.toolbar.visibility= View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text= "Pin your episodes!"
                }

                R.id.navigation_search -> {
                    navView.visibility= View.VISIBLE
                    binding.toolbar.visibility= View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text= "Find your episodes"
                }

                R.id.navigation_coauthoring -> {
                    navView.visibility= View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = "還沒想好"
                }

                R.id.navigation_share -> {
                    navView.visibility= View.VISIBLE
//                    binding.toolbar.visibility = View.VISIBLE
//                    binding.toolbar.navigationIcon = null
//                    binding.toolbarText.text = "Share to Inspire"
                }

                R.id.youTubeNoteFragment -> {
                    navView.visibility= View.GONE
                    binding.toolbar.visibility= View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigateUp()
                        binding.toolbar.navigationIcon = null
                        binding.bottomNavView.visibility = View.VISIBLE
                    }
                    binding.toolbarText.text= "YouTube Note"
                }
            }
        }

        navView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {

                R.id.navigation_note_list -> {
                    navController.navigate(R.id.noteListFragment)
                    true
                }

                R.id.navigation_search -> {
                    navController.navigate(R.id.searchFragment)
                    true
                }

                else -> true
            }
        }
    }

    fun navigateUpInToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController(R.id.nav_host_fragment_activity_main).navigateUp()
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
