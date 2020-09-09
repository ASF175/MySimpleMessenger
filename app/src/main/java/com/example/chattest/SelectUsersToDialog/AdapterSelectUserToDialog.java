package com.example.chattest.SelectUsersToDialog;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.fbObjects.ItemUserList;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AdapterSelectUserToDialog extends RecyclerView.Adapter<SelectUserToDialogViewHolder> {

    public static ArrayList<ItemUserList> userlist = new ArrayList<>();
    public static OnUserClickListener onUserClickListener;
    public static OnUserLongClickListener onUserLongClickListener;

    public AdapterSelectUserToDialog(OnUserClickListener onUserClickListener, OnUserLongClickListener onUserLongClickListener) {
        AdapterSelectUserToDialog.onUserClickListener = onUserClickListener;
        AdapterSelectUserToDialog.onUserLongClickListener = onUserLongClickListener;
    }

    @NonNull
    @Override
    public SelectUserToDialogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new SelectUserToDialogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectUserToDialogViewHolder holder, int position) {
        ItemUserList user = userlist.get(position);
        holder.username.setText(user.userName);
        if (user.avaUrl.isEmpty()) {
            Picasso.get().load(R.drawable.user_placeholder).fit().into(holder.img);
        } else {
            Picasso.get().load(Uri.parse(user.avaUrl)).fit().into(holder.img);
        }
    }

    @Override
    public int getItemCount() {
        return userlist.size();
    }

    public void addItem(ItemUserList item) {
        userlist.add(item);
        notifyItemInserted(userlist.size() - 1);
    }

    public void deleteItem(int item) {
        if (userlist.size() <= item)
            return;

        userlist.remove(item);
        notifyItemRemoved(item);
    }

    public void updateItem(ItemUserList item) {
        for (int i = 0; i < userlist.size(); i++) {
            if (userlist.get(i).userId.equals(item.userId)) {
                userlist.set(i, item);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void clearItems() {
        int count = userlist.size();
        userlist.clear();
        notifyItemRangeRemoved(0, count);
    }

    public void updateList(ArrayList<ItemUserList> newList) {
        if (!userlist.isEmpty())
            clearItems();

        userlist.addAll(newList);
        notifyItemRangeInserted(0, userlist.size());
    }

    public interface OnUserClickListener {
        void onUserClick(ItemUserList itemUserList);
    }

    public interface OnUserLongClickListener {
        void onUserClick(ItemUserList itemUserList, int position);
    }
}
