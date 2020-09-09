package com.example.chattest.SelectUsersToDialog;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.fbObjects.ItemUserList;

public class SelectUserToDialogViewHolder extends RecyclerView.ViewHolder {

    TextView username;
    CardView cardView;
    ImageView img;

    public SelectUserToDialogViewHolder(@NonNull View itemView) {
        super(itemView);
        username = itemView.findViewById(R.id.usernameText);
        cardView = itemView.findViewById(R.id.card_view_user_to_chat);
        img = itemView.findViewById(R.id.user_image);

        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ItemUserList item = AdapterSelectUserToDialog.userlist.get(getLayoutPosition());
                if (item.selected) {
                    cardView.setCardBackgroundColor(0xA3FFFFFF);
                } else {
                    cardView.setCardBackgroundColor(0x2F868686);
                }
                AdapterSelectUserToDialog.onUserLongClickListener.onUserClick(item, getLayoutPosition());
                return true;
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemUserList item = AdapterSelectUserToDialog.userlist.get(getLayoutPosition());
                AdapterSelectUserToDialog.onUserClickListener.onUserClick(item);
            }
        });

    }
}
