package com.example.chattest.Dialog;

import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.AdvancedMessageRealm;
import com.google.firebase.auth.FirebaseAuth;

public class MessageViewHolder extends RecyclerView.ViewHolder {

    TextView name, message, time, date;
    CardView cardView;
    ImageView img;
    LinearLayout layout;

    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);

        layout = itemView.findViewById(R.id.item_message_layout);
        name = itemView.findViewById(R.id.textView);
        message = itemView.findViewById(R.id.textView2);
        time = itemView.findViewById(R.id.item_message_time);
        date = itemView.findViewById(R.id.item_message_date);
        cardView = itemView.findViewById(R.id.item_message_card_view);
        img = itemView.findViewById(R.id.item_message_image);

        cardView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                AdvancedMessageRealm msg = AdapterMessage.messages.get(getLayoutPosition());
                if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(msg.userId)) {
                    AdapterMessage.onMessageContextListener.onMessageContext(msg, getLayoutPosition());
                    menu.add(0, 0, 0, "Delete");
                    menu.add(0, 1, 0, "Edit");
                } else {
                    Toast.makeText(v.getContext(), "ШО ТЫ ТЫКАЕШЬ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
