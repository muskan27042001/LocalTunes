package com.example.musicplayerapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musicplayerapp.databinding.ActivityFavouriteBinding

class FavouriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavouriteBinding
    private lateinit var adapter: FavouriteAdapter

    companion object{
        // favourite songs list
        var favouriteSongs: ArrayList<Music> = ArrayList()
        var favouritesChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        favouriteSongs = checkPlaylist(favouriteSongs)
        binding.backBtnFA.setOnClickListener { finish() }
        binding.favouriteRV.setHasFixedSize(true)
        binding.favouriteRV.setItemViewCacheSize(13)
        binding.favouriteRV.layoutManager = GridLayoutManager(this, 4)
        adapter = FavouriteAdapter(this, favouriteSongs)
        binding.favouriteRV.adapter = adapter

        favouritesChanged = false
        // if favourite songs are less than 1 then hide shuffle button
        //if(favouriteSongs.size < 1) binding.shuffleBtnFA.visibility = View.INVISIBLE

        //if(favouriteSongs.isNotEmpty()) binding.instructionFV.visibility = View.GONE

        binding.moreDotsFavouriteActivity.setOnClickListener {
            val popupMenu = PopupMenu(this@FavouriteActivity, it)
            popupMenu.menuInflater.inflate(R.menu.favourite_activity_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.shuffleFavouriteSongs -> {
                        val intent = Intent(this, PlayerActivity::class.java)
                        intent.putExtra("index", 0)
                        intent.putExtra("class", "FavouriteShuffle")
                        startActivity(intent)
                        true
                    }
                    // Handle other menu items as needed
                    else -> false
                }
            }

            popupMenu.show()
        }
        // SHUFFLE BUTTON
        /*binding.shuffleBtnFA.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouriteShuffle")
            startActivity(intent)
        }*/
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(favouritesChanged) {
            adapter.updateFavourites(favouriteSongs)
            favouritesChanged = false
        }
    }
}