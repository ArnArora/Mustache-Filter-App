package com.example.moodmemustache.video_db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = Video.class, exportSchema=false, version=1)
public abstract class VideoDatabase extends RoomDatabase {
    private static final String DB_NAME = "video_db";
    private static VideoDatabase instance;

    //the database in which the video objects are stored
    public static synchronized  VideoDatabase getInstance(Context context){
        if (instance==null){
            instance = Room.databaseBuilder(context.getApplicationContext(), VideoDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration().build();
        }
        return instance;
    }
    public abstract VideoDao videoDao();
}
