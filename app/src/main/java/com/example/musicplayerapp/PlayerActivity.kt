package com.example.musicplayerapp

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayerapp.databinding.ActivityPlayerBinding
import com.example.musicplayerapp.databinding.AudioBoosterBinding
import com.example.musicplayerapp.databinding.DetailsViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.io.Files.append

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {


    companion object {
        lateinit var musicListPA : ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying:Boolean = false
        var musicService: MusicService? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeat: Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        lateinit var loudnessEnhancer: LoudnessEnhancer
    }



    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(intent.data?.scheme.contentEquals("content")){
            // setting song position to 0 because in this activity there is only one song which we
            // have selected
            songPosition = 0
            // starting service
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)

            musicListPA = ArrayList()
            // getting details of the song
            musicListPA.add(getMusicDetails(intent.data!!))
            // loading song image
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
                .into(binding.songImgPA)
            // setting song title
            binding.songNamePA.text = musicListPA[songPosition].title
            binding.artistNamePA.text = musicListPA[songPosition].artist
        }
        else initializeLayout()

        // more info button
        binding.moreDotsPlayerActivity.setOnClickListener {
            val popupMenu = PopupMenu(this@PlayerActivity, it)
            popupMenu.menuInflater.inflate(R.menu.player_activity_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.timer -> {
                        val timer = min15 || min30 || min60
                        if(!timer) showBottomSheetDialog()
                        else {
                            val builder = MaterialAlertDialogBuilder(this)
                            builder.setTitle("Stop Timer")
                                .setMessage("Do you want to stop timer?")
                                .setPositiveButton("Yes"){ _, _ ->
                                    min15 = false
                                    min30 = false
                                    min60 = false
                                   // binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
                                }
                                .setNegativeButton("No"){dialog, _ ->
                                    dialog.dismiss()
                                }
                            val customDialog = builder.create()
                            customDialog.show()
                            setDialogBtnBackground(this, customDialog)
                        }
                        true
                    }
                    R.id.song_info -> {
                        Toast.makeText(baseContext,  "Song Info", Toast.LENGTH_SHORT).show()
                        val detailsDialog = LayoutInflater.from(this).inflate(R.layout.details_view, binding.root, false)
                        val binder = DetailsViewBinding.bind(detailsDialog)
                        binder.detailsTV.setTextColor(Color.WHITE)
                        binder.root.setBackgroundColor(Color.TRANSPARENT)
                        val dDialog = MaterialAlertDialogBuilder(this)
//                        .setBackground(ColorDrawable(0x99000000.toInt()))
                            .setView(detailsDialog)
                            .setPositiveButton("OK"){self, _ -> self.dismiss()}
                            .setCancelable(false)
                            .create()
                        dDialog.show()
                        dDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                        setDialogBtnBackground(this, dDialog)
                        dDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
                        val str = SpannableStringBuilder()
                            .bold { append("Song Info\n\nSong: ") }
                            .append(musicListPA[songPosition].title)
                            .bold { append("\n\nArtist\n") }
                            .append(musicListPA[songPosition].artist)
                            .bold { append("\n\nAlbum\n") }
                            .append(musicListPA[songPosition].album)
                            .bold { append("\n\nDuration: ") }
                            .append(DateUtils.formatElapsedTime(musicListPA[songPosition].duration/1000))
                            .bold { append("\n\nLocation: ") }
                            .append(musicListPA[songPosition].path)
                        binder.detailsTV.text = str
                        true
                    }
                    R.id.share -> {
                        Toast.makeText(baseContext,  "Share", Toast.LENGTH_SHORT).show()
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "audio/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
                        startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
                        true
                    }
                    R.id.repeatBtnPA -> {
                        if(!repeat){
                            repeat = true
                            binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
                        }else{
                            repeat = false
                            binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
                        }
                        true
                    }

                    // Handle other menu items as needed
                    else -> false
                }
            }

            popupMenu.show()
        }

        //audio booster feature
       /* binding.boosterBtnPA.setOnClickListener {
            val customDialogB = LayoutInflater.from(this).inflate(R.layout.audio_booster, binding.root, false)
            val bindingB = AudioBoosterBinding.bind(customDialogB)
            val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                .setOnCancelListener { playMusic() }
                .setPositiveButton("OK"){self, _ ->
                    loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                    playMusic()
                    self.dismiss()
                }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialogB.show()

            bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt()/100
            bindingB.progressText.text = "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt()/10} %"
            bindingB.verticalBar.setOnProgressChangeListener {
                bindingB.progressText.text = "Audio Boost\n\n${it*10} %"
            }
            setDialogBtnBackground(this, dialogB)
        }*/

        binding.backBtnPA.setOnClickListener { finish() }
        binding.playPauseBtnPA.setOnClickListener{ if(isPlaying) pauseMusic() else playMusic() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if(isPlaying) R.drawable.pause_icon else R.drawable.play_icon)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.repeatBtnPA.setOnClickListener {
            if(!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 13)
            }catch (e: Exception){Toast.makeText(this,  "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()}
        }
       /* binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if(!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes"){ _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
                    }
                    .setNegativeButton("No"){dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                setDialogBtnBackground(this, customDialog)
            }
        }*/

        // Favourite Button
        binding.favouriteBtnPA.setOnClickListener {
            // checking if song is already fav or not
            // if already in fav then getting the index of that song
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if(isFavourite){
                // if already fav then mark it un fav
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                // removing this song from the list of fav songs
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else{
                // if not already fav then mark it fav
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
                // adding song in fav songs list
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
            FavouriteActivity.favouritesChanged = true
        }
    }
    //Important Function
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index", 0)
        when(intent.getStringExtra("class")){
            "NowPlaying"->{
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"-> initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            "MainActivity"-> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = true)
            "FavouriteShuffle"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
            "PlaylistDetailsAdapter"->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = false)
            "PlaylistDetailsShuffle"->
                initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = true)
            "PlayNext"->initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)
        }
        if (musicService!= null && !isPlaying) playMusic()
    }

    private fun setLayout(){
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        binding.artistNamePA.text = musicListPA[songPosition].artist
        //if(repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext, R.color.purple_500))
        //if(min15 || min30 || min60) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext, R.color.purple_500))
        if(isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

        val img = getImgArt(musicListPA[songPosition].path)
        val image = if (img != null) {
            BitmapFactory.decodeByteArray(img, 0, img.size)
        }
        else {
            BitmapFactory.decodeResource(
                resources,
                R.drawable.music
            )
        }
        val bgColor = getMainColor(image)
        val topColor = Color.parseColor("#FEFEFE") // Example: Slightly off-white
        val gradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(topColor, bgColor))
        binding.root.background = gradient
        window?.statusBarColor = bgColor
    }

    private fun createMediaPlayer(){
        try {
            // Checking if mediaPlayer is not initialized, initialize it
            if (musicService!!.mediaPlayer == null)
                musicService!!.mediaPlayer = MediaPlayer()

            // Resetting mediaPlayer
            musicService!!.mediaPlayer!!.reset()

            // Setting data source to mediaPlayer from musicListPA at songPosition
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)

            // Prepare mediaPlayer
            musicService!!.mediaPlayer!!.prepare()

            // Update UI elements with current media player information
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())

            // Initialize and set up seekBarPA
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration

            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
            playMusic()
            loudnessEnhancer = LoudnessEnhancer(musicService!!.mediaPlayer!!.audioSessionId)
            loudnessEnhancer.enabled = true
        }catch (e: Exception){Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()}
    }

    private fun playMusic(){
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
    }

    private fun pauseMusic(){
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)


    }
    private fun prevNextSong(increment: Boolean){
        if(increment)
        {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        }
        else{
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }



    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if(musicService == null){
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        createMediaPlayer()
        musicService!!.seekBarSetup()


    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        //for refreshing now playing image & text on song completion
        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showMoreBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.more_bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.timer)?.setOnClickListener {
            Toast.makeText(baseContext,  "Timer", Toast.LENGTH_SHORT).show()
            showBottomSheetDialog()
        }
        dialog.findViewById<LinearLayout>(R.id.song_info)?.setOnClickListener {
            Toast.makeText(baseContext,  "Song Info", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                val detailsDialog = LayoutInflater.from(this).inflate(R.layout.details_view, binding.root, false)
                val binder = DetailsViewBinding.bind(detailsDialog)
                binder.detailsTV.setTextColor(Color.WHITE)
                binder.root.setBackgroundColor(Color.TRANSPARENT)
                val dDialog = MaterialAlertDialogBuilder(this)
//                        .setBackground(ColorDrawable(0x99000000.toInt()))
                    .setView(detailsDialog)
                    .setPositiveButton("OK"){self, _ -> self.dismiss()}
                    .setCancelable(false)
                    .create()
                dDialog.show()
                dDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                setDialogBtnBackground(this, dDialog)
                dDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
                val str = SpannableStringBuilder()
                    .bold { append("Song Info\n\nSong: ") }
                    .append(musicListPA[songPosition].title)
                    .bold { append("\n\nArtist\n") }
                    .append(musicListPA[songPosition].artist)
                    .bold { append("\n\nAlbum\n") }
                    .append(musicListPA[songPosition].album)
                    .bold { append("\n\nDuration: ") }
                    .append(DateUtils.formatElapsedTime(musicListPA[songPosition].duration/1000))
                    .bold { append("\n\nLocation: ") }
                    .append(musicListPA[songPosition].path)
                binder.detailsTV.text = str
        }
        dialog.findViewById<LinearLayout>(R.id.share)?.setOnClickListener {
            Toast.makeText(baseContext,  "Share", Toast.LENGTH_SHORT).show()
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))


        }
    }


    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 15 minutes", Toast.LENGTH_SHORT).show()
           // binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min15 = true
            // After the sleep period,it checks if min 15 is still true,if true it exists app
            Thread{Thread.sleep((15 * 60000).toLong())
                if(min15) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            //binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if(min30) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 60 minutes", Toast.LENGTH_SHORT).show()
            //binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
            min60 = true
            Thread{Thread.sleep((60 * 60000).toLong())
                if(min60) exitApplication()}.start()
            dialog.dismiss()
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMusicDetails(contentUri: Uri): Music {
        var cursor: Cursor? = null
        try {
            // Define the columns we want to retrieve from the MediaStore
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)

            // Query the MediaStore content resolver for the specified contentUri
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)

            // Get the column indices for the data and duration columns
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            // Move the cursor to the first result
            cursor!!.moveToFirst()

            // Retrieve the path and duration from the cursor based on column indices
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!

            // Create and return a Music object with the retrieved details
            return Music(
                id = "Unknown",                           // Placeholder for ID (not retrieved)
                title = path.toString(),                  // Title based on path (file name)
                album = "Unknown",                        // Placeholder for album (not retrieved)
                artist = "Unknown",                       // Placeholder for artist (not retrieved)
                duration = duration,                      // Duration in milliseconds
                artUri = "Unknown",                       // Placeholder for art URI (not retrieved)
                path = path.toString()                    // Full path of the music file
            )
        } finally {
            // Close the cursor to release resources
            cursor?.close()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if(musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }
    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false){
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if(shuffle) musicListPA.shuffle()
        setLayout()
        if(!playNext) PlayNext.playNextList = ArrayList()
    }
}