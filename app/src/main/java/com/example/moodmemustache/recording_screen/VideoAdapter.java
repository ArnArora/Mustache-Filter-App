package com.example.moodmemustache.recording_screen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmemustache.R;
import com.example.moodmemustache.video_db.Video;
import com.example.moodmemustache.video_display.VideoDisplay;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder>{

    //an interface that the host activity can implement so that TagDialog can pop up on click
    public interface OnTagEditClick {
        void onTagEditClick(int pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        //display and edit tag
        public Button tagView;
        public TextView durationView;
        //preview image of video
        public ImageButton preview;
        public OnTagEditClick tagEditor;

        public ViewHolder(@NonNull View itemView, OnTagEditClick t) {
            super(itemView);
            //set the components
            tagEditor = t;
            tagView = (Button) itemView.findViewById(R.id.tag);
            durationView = (TextView) itemView.findViewById(R.id.duration);
            preview = (ImageButton) itemView.findViewById(R.id.session_video);
            tagView.setOnClickListener(this);
        }

        //pass in adapter position so that host activity knows what video was clicked on
        @Override
        public void onClick(View view) {
            tagEditor.onTagEditClick(this.getAdapterPosition());
        }
    }

    //listens for what image is clicked on
    //plays the video that corresponds to that image in a new activity (VideoDisplay)
    public class DisplayClickListener implements View.OnClickListener {
        private int position;
        public DisplayClickListener(int pos){
            position = pos;
        }
        @Override
        public void onClick(View view) {
            Video curVideo = videos.get(position);
            //open new intent and pass in string path of the video
            Intent intent = new Intent(context, VideoDisplay.class);
            intent.putExtra("Video path", curVideo.getVideoPath());
            context.startActivity(intent);
        }
    }

    public List<Video> getVideos() {
        return videos;
    }

    private List<Video> videos;
    private Context context;
    private OnTagEditClick tagListener;

    public VideoAdapter(List<Video> v, Context c, OnTagEditClick t){
        videos = v;
        context = c;
        tagListener = t;
    }

    private View.OnClickListener displayListener;
    @NonNull
    @Override
    public VideoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflating = LayoutInflater.from(context);
        View videoView = inflating.inflate(R.layout.recording_card, parent, false);
        ViewHolder holder = new ViewHolder(videoView, tagListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VideoAdapter.ViewHolder holder, int position) {
        Video curVideo = videos.get(position);

        Button tag = holder.tagView;
        TextView duration = holder.durationView;
        ImageButton previewView = holder.preview;

        //update information
        duration.setText("Duration: "+displayMilli(curVideo.getDuration()));
        tag.setText(curVideo.getTag());
        //get image from storage and display it to user
        Bitmap bitmap = BitmapFactory.decodeFile(curVideo.getPreviewPath());
        previewView.setImageBitmap(bitmap);
        //image takes to video when clicked
        displayListener = new DisplayClickListener(position);
        previewView.setOnClickListener(displayListener);
    }

    //take time in milliseconds and display in mm:ss format
    public String displayMilli(int milliseconds){
        int seconds = milliseconds/1000;
        int minutes = seconds/60;
        seconds %= 60;
        if (seconds<10) {
            return minutes+":0"+seconds;
        }else{
            return minutes+":"+seconds;
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }
}
