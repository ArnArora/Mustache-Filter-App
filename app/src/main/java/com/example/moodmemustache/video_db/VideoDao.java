package com.example.moodmemustache.video_db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VideoDao {
    //used for recyclerview adapter
    @Query("Select * from video")
    List<Video> getVideoList();
    @Insert
    void insertVideo(Video video);
    @Update
    void updateVideo(Video video);
}
