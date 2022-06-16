package com.example.moodmemustache.video_display;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodmemustache.R;
import com.example.moodmemustache.recording_screen.RecordingsPage;

public class VideoDisplay extends AppCompatActivity implements View.OnClickListener{
    //this screen shows up when user wants to view recorded video
    //path of video in storage
    private String path;
    private VideoView screen;
    private ImageButton goBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_video);
        //video path is passed in from recordings page
        Intent intent = getIntent();
        path = intent.getStringExtra("Video path");

        goBack = (ImageButton) findViewById(R.id.return_button_display);
        goBack.setOnClickListener(this);

        //create a media controller for the videoview
        screen = findViewById(R.id.videoView);
        MediaController mediaController=new MediaController(this);
        mediaController.setAnchorView(screen);

        //get video from path and play it on screen
        Uri video=Uri.parse(path);
        screen.setVideoURI(video);
        screen.setMediaController(mediaController);
        screen.start();
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, RecordingsPage.class);
        startActivity(intent);
    }
}
