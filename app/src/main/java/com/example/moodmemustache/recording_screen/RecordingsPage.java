package com.example.moodmemustache.recording_screen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.moodmemustache.R;
import com.example.moodmemustache.video_db.Video;
import com.example.moodmemustache.video_db.VideoDatabase;
import com.example.moodmemustache.video_screen.CreateVideo;
import com.example.moodmemustache.video_screen.TagDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class RecordingsPage extends AppCompatActivity implements View.OnClickListener, VideoAdapter.OnTagEditClick, TagDialog.TagDialogListener {
    //this activity displays all videos and their data to the user
    private boolean deviceCamera;
    private List<Video> videos;
    private VideoAdapter adapter;
    private RecyclerView videoGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_recordings);
        Button videoButton = (Button) findViewById(R.id.go_video);
        videoButton.setOnClickListener(this);
        deviceCamera = hasCamera();

        videoGrid = (RecyclerView) findViewById(R.id.recordings_list);

        //asynchronously retrieve all videos in database and pass into adapter
        AsyncTask.execute(() -> {
            VideoDatabase videoDb = VideoDatabase.getInstance(this.getApplicationContext());
            videos = videoDb.videoDao().getVideoList();
            adapter = new VideoAdapter(videos, this, this);
            //must be run separately as it affects the views
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoGrid.setAdapter(adapter);
                }
            });
        });

        int columns = 0;
        //choose column count based on orientation of device
        if (getResources().getConfiguration().orientation== Configuration.ORIENTATION_PORTRAIT){
            columns = 2;
        }else{
            columns = 4;
        }
        GridLayoutManager layoutManager = new GridLayoutManager(this, columns);
        videoGrid.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.go_video){
            if (deviceCamera){
                Intent intent = new Intent(this, CreateVideo.class);
                startActivity(intent);
            }else{//device does not have camera so stay in activity
                Toast.makeText(this, "Device does not have camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //ensure that device has camera before taking to recording screen and asking for permission
    private boolean hasCamera() {
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FRONT)){
            return true;
        } else {
            return false;
        }
    }

    private Video curVideo;
    private int pos;
    @Override
    public void onTagEditClick(int p){
        //store which video was clicked and the position
        pos = p;
        curVideo = videos.get(p);
        //open dialog and update tag
        DialogFragment dialog = new TagDialog();
        dialog.show(getSupportFragmentManager(), "TagDialog");
    }

    //after user confirms tag, update the video tag and reflect changes
    @Override
    public void onDialogPositiveClick(String tag) {
        curVideo.setTag(tag);
        //update database asynchronously so that it does not block main thread
        AsyncTask.execute(() -> {
            VideoDatabase appData = VideoDatabase.getInstance(getApplicationContext());
            appData.videoDao().updateVideo(curVideo);
            //put in runOnUiThread because this affects the views
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyItemChanged(pos);
                }
            });
        });
        Toast.makeText(this, "Updated video tag", Toast.LENGTH_LONG);
    }
}