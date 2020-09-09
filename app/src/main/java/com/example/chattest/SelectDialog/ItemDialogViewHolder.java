package com.example.chattest.SelectDialog;

import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.DialogsHolderRealm;

public class ItemDialogViewHolder extends RecyclerView.ViewHolder {

    TextView chatTitle, chatMessage, time, chatMessagesCount;
    ImageView img, userImg;
    CardView cardView;

    public ItemDialogViewHolder(@NonNull View itemView) {
        super(itemView);
        chatTitle = itemView.findViewById(R.id.item_chat_title);
        chatMessage = itemView.findViewById(R.id.item_chat_message);
        chatMessagesCount = itemView.findViewById(R.id.item_chat_messages_count);
        time = itemView.findViewById(R.id.item_chat_time);
        userImg = itemView.findViewById(R.id.image_user_dialog);
        img = itemView.findViewById(R.id.image_dialog_holder);
        cardView = itemView.findViewById(R.id.cardview_chat);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogsHolderRealm itemChat = AdapterDialogSelect.chatList.get(getLayoutPosition());
                AdapterDialogSelect.onChatClickListener.onChatClick(itemChat);
            }
        });
        itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                DialogsHolderRealm dialogsHolder = AdapterDialogSelect.chatList.get(getLayoutPosition());
                AdapterDialogSelect.onChatContextListener.onMessageContextClick(dialogsHolder);
                if (dialogsHolder.type.equals("chat")) {
                    menu.add(0, 0, 0, "Leave chat");
                } else {
                    menu.add(0, 1, 0, "Delete dialog");
                }
            }
        });
    }
}
