package com.example.chattest.SelectDialog;

import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.DialogsHolderRealm;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class AdapterDialogSelect extends RecyclerView.Adapter<ItemDialogViewHolder> {

    public static OnChatClickListener onChatClickListener;
    public static OnChatContextListener onChatContextListener;
    static ArrayList<DialogsHolderRealm> chatList = new ArrayList<>();
    private String userId;

    public AdapterDialogSelect(OnChatClickListener onChatClickListener, OnChatContextListener onChatContextListener) {
        AdapterDialogSelect.onChatClickListener = onChatClickListener;
        AdapterDialogSelect.onChatContextListener = onChatContextListener;
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ItemDialogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ItemDialogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemDialogViewHolder holder, int position) {
        if (chatList.get(position).getCanWrite().equals("")) {
            holder.cardView.setVisibility(View.GONE);
        } else {
            if (holder.cardView.getVisibility() == View.GONE)
                holder.cardView.setVisibility(View.VISIBLE);

            int MAX_LENGHT = 38;


            holder.chatTitle.setText(chatList.get(position).chatName);
            holder.time.setText(handleTime(chatList.get(position).time));
            if (chatList.get(position).unreadedMessagesCount == 0)
                holder.chatMessagesCount.setText("");
            else
                holder.chatMessagesCount.setText(chatList.get(position).unreadedMessagesCount == 1 ? "new" : Long.toString(chatList.get(position).unreadedMessagesCount));

            if (chatList.get(position).type.equals("chat")) {
                if (!chatList.get(position).avaUri.equals("")) {
                    Picasso.get().load(Uri.parse(chatList.get(position).avaUri)).fit().into(holder.img);
                } else {
                    Picasso.get().load(R.drawable.chat_plaseholder).fit().into(holder.img);
                }

                if (chatList.get(position).text.isEmpty()) {
                    holder.userImg.setVisibility(View.GONE);
                    holder.chatMessage.setText("You joined the chat.");
                } else {
                    holder.chatMessage.setText(chatList.get(position).text.length() > MAX_LENGHT ? (chatList.get(position).text.substring(0, MAX_LENGHT) + "...") : chatList.get(position).text);
                    if (chatList.get(position).userIdMessage.equals(userId)) {
                        holder.userImg.setVisibility(View.VISIBLE);
                        if ((chatList.get(position).lastMessageUri.equals("")))
                            Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.userImg);
                        else
                            Picasso.get().load((chatList.get(position).lastMessageUri)).fit().into(holder.userImg);
                    } else {
                        holder.userImg.setVisibility(View.VISIBLE);
                        if (chatList.get(position).lastMessageUri.equals(""))
                            Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.userImg);
                        else
                            Picasso.get().load(chatList.get(position).lastMessageUri).fit().into(holder.userImg);
                    }
                }
            } else {
                if (!chatList.get(position).avaUri.equals("")) {
                    Picasso.get().load(Uri.parse(chatList.get(position).avaUri)).fit().into(holder.img);
                } else {
                    Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.img);
                }

                if (chatList.get(position).text.isEmpty()) {
                    holder.userImg.setVisibility(View.GONE);
                    holder.chatMessage.setText("Started a dialogue with you.");
                } else {
                    holder.chatMessage.setText(chatList.get(position).text.length() > MAX_LENGHT ? (chatList.get(position).text.substring(0, MAX_LENGHT) + "...") : chatList.get(position).text);
                    if (chatList.get(position).userIdMessage.equals(userId)) {
                        holder.userImg.setVisibility(View.VISIBLE);
                        if (chatList.get(position).lastMessageUri.equals(""))
                            Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.userImg);
                        else
                            Picasso.get().load(chatList.get(position).lastMessageUri).fit().into(holder.userImg);
                    } else {
                        holder.userImg.setVisibility(View.GONE);
                        holder.userImg.setImageDrawable(null);
                    }
                }
            }
        }
    }

    private String handleTime(long lastOnline) {
        Calendar current = new GregorianCalendar();
        Calendar usertime = new GregorianCalendar();
        usertime.setTimeInMillis(lastOnline);
        if (current.get(Calendar.DAY_OF_YEAR) != usertime.get(Calendar.DAY_OF_YEAR) | current.get(Calendar.YEAR) != usertime.get(Calendar.YEAR))
            return (String) DateFormat.format("d MMM", lastOnline);
        else
            return (String) DateFormat.format("HH:mm", lastOnline);
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public long getItemId(int position) {
        return chatList.get(position).getDialogId().hashCode();

    }

    public void updateList(final ArrayList<DialogsHolderRealm> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCb(chatList, newList));
        diffResult.dispatchUpdatesTo(this);
        chatList.clear();
        chatList.addAll(newList);
    }

    public void addItem(DialogsHolderRealm item) {
        chatList.add(item);
        notifyItemInserted(chatList.size() - 1);
    }

    public void clearChatList() {
        int count = chatList.size();
        chatList.clear();
        notifyItemRangeRemoved(0, count);
    }

    public void clear() {
        chatList.clear();
    }

    public void updateItem(DialogsHolderRealm item) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).dialogId.equals(item.dialogId)) {
                chatList.set(i, item);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeItem(int i) {
        if (chatList.size() <= i)
            return;

        chatList.remove(i);
        notifyItemRemoved(i);
    }

    public interface OnChatClickListener {
        void onChatClick(DialogsHolderRealm item);
    }

    public interface OnChatContextListener {
        void onMessageContextClick(DialogsHolderRealm item);
    }

    private class DiffCb extends DiffUtil.Callback {

        private final ArrayList<DialogsHolderRealm> mOldEmployeeList;
        private final ArrayList<DialogsHolderRealm> mNewEmployeeList;

        public DiffCb(ArrayList<DialogsHolderRealm> oldEmployeeList, ArrayList<DialogsHolderRealm> newEmployeeList) {
            this.mOldEmployeeList = oldEmployeeList;
            this.mNewEmployeeList = newEmployeeList;
        }

        @Override
        public int getOldListSize() {
            return mOldEmployeeList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewEmployeeList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldEmployeeList.get(oldItemPosition).getDialogId() == mNewEmployeeList.get(
                    newItemPosition).getDialogId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final DialogsHolderRealm oldEmployee = mOldEmployeeList.get(oldItemPosition);
            final DialogsHolderRealm newEmployee = mNewEmployeeList.get(newItemPosition);

            return oldEmployee.equals(newEmployee);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {

            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
