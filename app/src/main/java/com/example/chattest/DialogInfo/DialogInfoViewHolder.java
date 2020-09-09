package com.example.chattest.DialogInfo;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;

public class DialogInfoViewHolder extends RecyclerView.ViewHolder {

    TextView username;
    ImageView img;

    public DialogInfoViewHolder(@NonNull View itemView) {
        super(itemView);

        username = itemView.findViewById(R.id.usernameText);
        img = itemView.findViewById(R.id.user_image);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInfoAdapter.onUserClickListener.onUserClick(DialogInfoAdapter.userlist.get(getLayoutPosition()));
            }
        });
    }

}
