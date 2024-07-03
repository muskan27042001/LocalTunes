package com.example.musicplayerapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayerapp.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //requireContext().theme.applyStyle(MainActivity.currentTheme[MainActivity.themeIndex], true)
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)

        // initialize binding
        binding = FragmentNowPlayingBinding.bind(view)

        // if song is not playing then it should not be visible in now playing
        binding.root.visibility = View.INVISIBLE

        // if song is not pause then play it , if song is playing then pause it
        binding.playPauseBtnNP.setOnClickListener {
            if(PlayerActivity.isPlaying) pauseMusic() else playMusic()
        }

        // NEXT BUTTON
        binding.nextBtnNP.setOnClickListener {
            // incrementing song position
            setSongPosition(increment = true)
            // creating media player again
            PlayerActivity.musicService!!.createMediaPlayer()
            // setting image of next song
            Glide.with(requireContext())
                .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
                .into(binding.songImgNP)
            // setting title of next song
            binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
            // showing pause button icon in notification because next sog starts playing
            PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
            // start playing music
            playMusic()
        }
        // when user clicks on now playing then he should reach current song
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("index", PlayerActivity.songPosition)
            intent.putExtra("class", "NowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if(PlayerActivity.musicService != null){
            // if music service is not null then make now playing song visible
            binding.root.visibility = View.VISIBLE

            // for moving title
            binding.songNameNP.isSelected = true

            // setting song img using glide in now playing layout
            Glide.with(requireContext())
                .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music).centerCrop())
                .into(binding.songImgNP)

            // setting title of now playing song
            binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

            // if song is playing then set icon of pause song
            if(PlayerActivity.isPlaying)
                binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
            // if song is not playing then set icon of play song
            else
                binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
        }
    }

    private fun playMusic(){
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
        PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
    }
    private fun pauseMusic(){
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
        PlayerActivity.musicService!!.showNotification(R.drawable.play_icon)
    }
}

