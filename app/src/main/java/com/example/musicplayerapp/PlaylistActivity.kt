package com.example.musicplayerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayerapp.databinding.ActivityPlaylistBinding
import com.example.musicplayerapp.databinding.AddPlaylistDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter: PlaylistViewAdapter

    companion object{
        var musicPlaylist: MusicPlaylist = MusicPlaylist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        // initialize binding
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        // setting view
        setContentView(binding.root)
        // only fixed size of playlists will be shown(loaded) ie 13 , it saves memory
        binding.playlistRV.setHasFixedSize(true)
        binding.playlistRV.setItemViewCacheSize(13)
        // setting grid layout
        binding.playlistRV.layoutManager = LinearLayoutManager(this@PlaylistActivity)
        // initializing adapter
        adapter = PlaylistViewAdapter(this, playlistList = musicPlaylist.ref)
        // setting adapter
        binding.playlistRV.adapter = adapter
        // on pressing back button, finish app
        binding.backBtnPLA.setOnClickListener { finish() }
        // adding playlist
        //binding.addPlaylistBtn.setOnClickListener { customAlertDialog() }

        //if(musicPlaylist.ref.isNotEmpty()) binding.instructionPA.visibility = View.GONE

        binding.moreDotsPlaylistActivity.setOnClickListener {
            val popupMenu = PopupMenu(this@PlaylistActivity, it)
            popupMenu.menuInflater.inflate(R.menu.playlist_activity_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.newPlaylist -> {
                        customAlertDialog()
                        true
                    }
                    // Handle other menu items as needed
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    // CUSTOM ALERT DIALOG
    private fun customAlertDialog(){
        val customDialog = LayoutInflater.from(this@PlaylistActivity).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(this)
        val dialog = builder.setView(customDialog)
            .setTitle("Playlist Details")
            .setPositiveButton("ADD"){ dialog, _ ->
                val playlistName = binder.playlistName.text
                if(playlistName != null )
                    if(playlistName.isNotEmpty())
                    {
                        addPlaylist(playlistName.toString())
                    }
                dialog.dismiss()
            }.create()
        dialog.show()
        setDialogBtnBackground(this, dialog)
    }

    // ADD PLAYLIST
    private fun addPlaylist(name: String){
        var playlistExists = false
        for(i in musicPlaylist.ref) {
            if (name == i.name){
                playlistExists = true
                break
            }
        }
        if(playlistExists)
            Toast.makeText(this, "Playlist Already Exist!!", Toast.LENGTH_SHORT).show()
        else {
            val tempPlaylist = Playlist()
            tempPlaylist.name = name
            tempPlaylist.playlist = ArrayList()
            val calendar = Calendar.getInstance().time
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            tempPlaylist.createdOn = sdf.format(calendar)
            musicPlaylist.ref.add(tempPlaylist)
            adapter.refreshPlaylist()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}