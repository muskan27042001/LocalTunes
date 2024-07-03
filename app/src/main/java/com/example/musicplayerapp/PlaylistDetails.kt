package com.example.musicplayerapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayerapp.databinding.ActivityPlaylistDetailsBinding
import com.example.musicplayerapp.databinding.AddPlaylistDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder

class PlaylistDetails : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private lateinit var adapter: MusicAdapter
    private lateinit var playListName : String

    companion object{
        var currentPlaylistPos: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        // initialize binding
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        // setting view
        setContentView(binding.root)
        // getting playlist position
        currentPlaylistPos = intent.extras?.get("index") as Int
        try{
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        }
        catch(e: Exception){}
        // only fixed size of playlists will be shown(loaded) ie 10 , it saves memory
        binding.playlistDetailsRV.setItemViewCacheSize(10)
        binding.playlistDetailsRV.setHasFixedSize(true)
        // layout manager
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this)
        // setting adapter
        adapter = MusicAdapter(this, PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist, playlistDetails = true)
        // adding adapter
        binding.playlistDetailsRV.adapter = adapter
        // on pressing back button, exit app
        binding.backBtnPD.setOnClickListener{
            finish()
        }

        binding.moreDotsPlaylistDetails.setOnClickListener {
            val popupMenu = PopupMenu(this@PlaylistDetails, it)
            popupMenu.menuInflater.inflate(R.menu.playlist_details_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.addSongs -> {
                        startActivity(Intent(this, SelectionActivity::class.java))
                        true
                    }
                    R.id.editPlaylistInfo -> {
                        customAlertDialog()
                        true
                    }
                    R.id.deletePlaylist -> {
                        val builder = MaterialAlertDialogBuilder(this)
                        builder.setTitle(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name)
                            .setMessage("Do you want to delete playlist?")
                            .setPositiveButton("Yes"){ dialog, _ ->
                                PlaylistActivity.musicPlaylist.ref.removeAt(currentPlaylistPos)
                                //refreshPlaylist()
                                dialog.dismiss()
                                finish()
                            }
                            .setNegativeButton("No"){dialog, _ ->
                                dialog.dismiss()
                            }
                        val customDialog = builder.create()
                        customDialog.show()

                        setDialogBtnBackground(this, customDialog)
                        true
                    }
                    R.id.shufflePlaylist -> {
                        val intent = Intent(this, PlayerActivity::class.java)
                        intent.putExtra("index", 0)
                        intent.putExtra("class", "PlaylistDetailsShuffle")
                        startActivity(intent)
                        true
                    }
                    // Handle other menu items as needed
                    else -> false
                }
            }

            popupMenu.show()
        }
        /*binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }
        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
        }
        binding.removeAllPD.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Remove")
                .setMessage("Do you want to remove all songs from playlist?")
                .setPositiveButton("Yes"){ dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref.removeAt(currentPlaylistPos)
                    adapter.refreshPlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("No"){dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()

            setDialogBtnBackground(this, customDialog)
        }*/
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        // setting playlist name
        binding.playlistNamePD.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name
        // setting more information of playlist such as total songs, created on, created by
        binding.moreInfoPD.text = "${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name}\n\n" +
        "Songs : ${adapter.itemCount}\n" +
                "Created On : ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn}\n"

        // if songs added in playlist are greater than 0
        if(adapter.itemCount > 0)
        {
            // add song image using glide
            Glide.with(this)
                .load(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
                .into(binding.playlistImgPD)
            // make shuffle button visible
            //binding.shuffleBtnPD.visibility = View.VISIBLE
        }
        else{

        }
        adapter.notifyDataSetChanged()
        //for storing favourites data using shared preferences
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
    }

    private fun customAlertDialog(){
        val customDialog = LayoutInflater.from(this@PlaylistDetails).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(this)
        playListName = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name.toString()
        binder.playlistName.setText(playListName)
        val dialog = builder.setView(customDialog)
            //.setTitle("Playlist Title")
            .setNegativeButton("Cancel"){dialog,_ ->
                dialog.dismiss()
            }
            .setPositiveButton("Confirm"){ dialog, _ ->
                val newPlayListName = binder.playlistName.text
                if(newPlayListName != null ){
                    PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name = newPlayListName.toString()
                    binding.playlistNamePD.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name
                }
                dialog.dismiss()
            }.create()
        dialog.show()
        setDialogBtnBackground(this, dialog)
    }
}