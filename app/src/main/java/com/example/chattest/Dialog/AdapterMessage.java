package com.example.chattest.Dialog;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.AdvancedMessageRealm;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static android.view.Gravity.END;
import static android.view.Gravity.START;

public class AdapterMessage extends RecyclerView.Adapter<MessageViewHolder> {

    public static OnMessageContextListener onMessageContextListener;
    static ArrayList<AdvancedMessageRealm> messages = new ArrayList<>();
    private int LARGE_WIDTH, SMALL_WIDTH;
    private FirebaseAuth mAuth;
    private GregorianCalendar currentTime;

    public AdapterMessage(OnMessageContextListener onMessageContextListener, int width) {
        AdapterMessage.onMessageContextListener = onMessageContextListener;
//        LARGE_WIDTH = (int)(width*0.69);
//        SMALL_WIDTH = (int)(width*0.55);
        LARGE_WIDTH = (int) (width * 0.80);
        SMALL_WIDTH = (int) (width * 0.66);
        mAuth = FirebaseAuth.getInstance();
        currentTime = new GregorianCalendar();
    }

    public void updateList(ArrayList<AdvancedMessageRealm> list) {
        if (!messages.isEmpty())
            clearItems();

        messages.addAll(list);
        notifyItemRangeInserted(0, messages.size());
    }

    public AdvancedMessageRealm getItem(int id) {
        return messages.get(id);
    }

    public void addItem(AdvancedMessageRealm msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateItem(AdvancedMessageRealm msg) {
        for (int i = 0; i < messages.size(); i++) {
            if (msg.messageId.equals(messages.get(i).messageId)) {
                messages.set(i, msg);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeItem(int id) {
        if (messages.size() <= id)
            return;

        messages.remove(id);
        notifyItemRemoved(id);
    }

    public void clearItems() {
        int count = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, count);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MessageViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        setMessagesGravity(holder, position);
    }

    void setMessagesGravity(@NonNull MessageViewHolder holder, int position) {
        AdvancedMessageRealm msg = messages.get(position);
        holder.name.setText(msg.name);
        holder.message.setText(msg.mess);
        holder.time.setText(DateFormat.format("HH:mm", msg.time));
        GregorianCalendar messTime = new GregorianCalendar();
        messTime.setTimeInMillis(msg.time);
        if (currentTime.get(Calendar.DAY_OF_YEAR) != messTime.get(Calendar.DAY_OF_YEAR)) {
            holder.date.setText(DateFormat.format("d MMM", msg.time));
        } else {
            holder.date.setVisibility(View.GONE);
        }
        if (msg.userId.equals(mAuth.getCurrentUser().getUid())) {
            holder.cardView.setCardBackgroundColor(0xFFBBDEFB);
            holder.message.setMaxWidth(LARGE_WIDTH);
            holder.img.setVisibility(View.GONE);
            holder.name.setGravity(END);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            holder.layout.setGravity(END);
            holder.name.setLayoutParams(params);
        } else {
            holder.cardView.setCardBackgroundColor(0x2F868686);
            if (msg.type.equals("chat")) {
                holder.message.setMaxWidth(SMALL_WIDTH);
                holder.img.setVisibility(View.VISIBLE);
                if (msg.avaUrl.isEmpty()) {
                    Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.img);
                } else {
                    Picasso.get().load(msg.avaUrl).fit().into(holder.img);
                }
            } else {
                holder.message.setMaxWidth(LARGE_WIDTH);
                holder.img.setVisibility(View.GONE);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = START;
            holder.message.setLayoutParams(params);
            holder.layout.setGravity(START);
            holder.name.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public interface OnMessageContextListener {
        void onMessageContext(AdvancedMessageRealm msg, int position);
    }
}
