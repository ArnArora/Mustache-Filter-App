package com.example.moodmemustache.video_screen;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.moodmemustache.R;

public class TagDialog extends DialogFragment {
    //pops up when setting or editing tag for each video
    public interface TagDialogListener {
        public void onDialogPositiveClick(String tag);
    }

    TagDialogListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (TagDialogListener) context;
        } catch (ClassCastException e) {
            //interface is not implemented by activity
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.tag_popup, null);

        builder.setTitle(R.string.tag_title);
        builder.setView(view);

        //this is where the user inputs the tag name
        EditText inputTag = (EditText) view.findViewById(R.id.tag_input);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Send event to host activity
                listener.onDialogPositiveClick(inputTag.getText().toString());
            }
        });
        return builder.create();
    }
}
