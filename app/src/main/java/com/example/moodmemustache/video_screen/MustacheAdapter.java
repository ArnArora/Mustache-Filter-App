package com.example.moodmemustache.video_screen;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmemustache.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MustacheAdapter extends RecyclerView.Adapter<MustacheAdapter.ViewHolder>{

    /*create interface that host activity implements,
    allows host activity to load models and textures with methods it already has
     */
    public interface OnMustacheClick {

        void onMustacheClickEdit(int pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        //component of each mustache
        public ImageButton mustacheCard;
        public OnMustacheClick mustacheListener;

        public ViewHolder(@NonNull View itemView, OnMustacheClick mustacheListener) {
            super(itemView);
            //set the components
            mustacheCard = (ImageButton) itemView.findViewById(R.id.mustache_image);
            mustacheCard.setOnClickListener(this);
            this.mustacheListener = mustacheListener;
        }

        //tell the activity implementing the interface which mustache has been clicked on
        @Override
        public void onClick(View view) {
            mustacheListener.onMustacheClickEdit(this.getAdapterPosition());
        }
    }

    public List<Mustache> getMustaches() {
        return mustaches;
    }

    private List<Mustache> mustaches;
    private OnMustacheClick mustacheListener;
    private Context context;

    //pass in context so that getAssets() can be used to retrieve images
    public MustacheAdapter(Context context, List<Mustache> mustaches, OnMustacheClick mustacheListener){
        this.context = context;
        this.mustaches = mustaches;
        this.mustacheListener = mustacheListener;
    }

    @NonNull
    @Override
    public MustacheAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflating = LayoutInflater.from(context);
        View videoView = inflating.inflate(R.layout.mustache_card, parent, false);
        ViewHolder holder = new ViewHolder(videoView, mustacheListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MustacheAdapter.ViewHolder holder, int position) {
        Mustache curMustache = mustaches.get(position);
        ImageButton mustacheView = holder.mustacheCard;
        //get image that corresponds to mustache and create drawable to display
        try
        {
            InputStream inputStream = context.getAssets().open("textures/"+curMustache.getImagePath());
            Drawable d = Drawable.createFromStream(inputStream, null);
            mustacheView.setImageDrawable(d);
            inputStream.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            return;
        }
    }

    @Override
    public int getItemCount() {
        return mustaches.size();
    }
}
