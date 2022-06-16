package com.example.moodmemustache.video_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName="video")
public class Video {
    //video object that stores data about each recorded video and is stored through ORM
    //path to preview image that is displayed on recordings page
    @ColumnInfo(name="previewPath")
    private String previewPath;

    //path to video in storage
    @ColumnInfo(name="videoPath")
    private String videoPath;
    @ColumnInfo(name="tag")
    private String tag;

    //duration in milliseconds
    @ColumnInfo(name="duration")
    private int duration;
    @PrimaryKey(autoGenerate=true)
    private int id;

    public Video(int id, String previewPath, String videoPath, String tag, int duration){
        this.previewPath = previewPath;
        this.videoPath = videoPath;
        this.tag = tag;
        this.duration = duration;
        this.id = id;
    }
    @Ignore
    public Video(String previewPath, String videoPath, String tag, int duration){
        this.previewPath = previewPath;
        this.videoPath = videoPath;
        this.tag = tag;
        this.duration = duration;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    public int getId() { return id; }
}
