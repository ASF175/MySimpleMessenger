package com.example.chattest.SelectDialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.DialogsHolderRealm;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class DialogSelectFragment extends Fragment {

    private RecyclerView recyclerDialogs;
    private AdapterDialogSelect adapterDialogsSelect;

    private String currentId;

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private NavController navController;
    private Context context;
    private Realm realm;
    private RealmResults<DialogsHolderRealm> dialogsRealm;
    private DialogsHolderRealm deletingDialog;

    private Toolbar toolbar;
    private ImageView drawer_image;
    private TextView drawer_title;
    private TextView drawer_subtitle;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Realm.init(view.getContext());
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        navController = NavHostFragment.findNavController(this);
        recyclerDialogs = view.findViewById(R.id.recycler_chats);
        context = view.getContext();
        toolbar = getActivity().findViewById(R.id.toolbar);

        drawer_image = getActivity().findViewById(R.id.drawer_icon);
        drawer_title = getActivity().findViewById(R.id.drawer_title);
        drawer_subtitle = getActivity().findViewById(R.id.drawer_subtitle);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(Navigation.createNavigateOnClickListener
                (R.id.action_dialogSelectFragment_to_selectUserToDialogFragment));
        getAuth();
        recyclerInit();
        dbInit();

        drawerInit();

        super.onViewCreated(view, savedInstanceState);
    }

    private void drawerInit() {
        if (firebaseAuth.getCurrentUser().getPhotoUrl() == null) {
            Picasso.get().load(R.drawable.user_placeholder).fit().into(drawer_image);
        } else {
            Picasso.get().load(firebaseAuth.getCurrentUser().getPhotoUrl()).fit().into(drawer_image);
        }
        drawer_title.setText(firebaseAuth.getCurrentUser().getDisplayName());
        drawer_subtitle.setText(firebaseAuth.getCurrentUser().getEmail());
    }

    private void getAuth() {
        toolbar.setLogo(null);
        toolbar.setSubtitle(null);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentId = firebaseAuth.getCurrentUser().getUid();
    }

    private void recyclerInit() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        mLayoutManager.setStackFromEnd(true);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);


        recyclerDialogs.setLayoutManager(mLayoutManager);
        recyclerDialogs.setItemViewCacheSize(20);
        recyclerDialogs.setDrawingCacheEnabled(true);
        recyclerDialogs.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerDialogs.getContext(),
                mLayoutManager.getOrientation());
        recyclerDialogs.addItemDecoration(dividerItemDecoration);

        AdapterDialogSelect.OnChatClickListener onChatClickListener = new AdapterDialogSelect.OnChatClickListener() {
            @Override
            public void onChatClick(@NotNull DialogsHolderRealm item) {
                Bundle bundle = new Bundle();
                bundle.putString("dialogId", item.dialogId);
                bundle.putString("type", item.type);
                bundle.putString("title", item.chatName);
                bundle.putString("url", item.avaUri);
                bundle.putString("cryptionType", item.cryptionType);
                bundle.putString("decryptedAES", item.decryptedAES);
                bundle.putString("sign", item.sign);
                bundle.putString("canWrite", item.canWrite);
                bundle.putString("userIdOpponent", item.userIdOpponent);
                bundle.putString("userIdAuthor", item.userIdAuthor == null ? "" : item.userIdAuthor);
                navController.navigate(R.id.action_dialogSelectFragment_to_dialogFragment, bundle);
            }
        };

        AdapterDialogSelect.OnChatContextListener onChatContextListener = new AdapterDialogSelect.OnChatContextListener() {
            @Override
            public void onMessageContextClick(DialogsHolderRealm item) {
                deletingDialog = item;
            }
        };
        adapterDialogsSelect = new AdapterDialogSelect(onChatClickListener, onChatContextListener);
        recyclerDialogs.setAdapter(adapterDialogsSelect);
    }

    void dbInit() {
        dialogsRealm = realm.where(DialogsHolderRealm.class).sort("time").findAll();
        ArrayList<DialogsHolderRealm> list = new ArrayList<>(dialogsRealm);
        adapterDialogsSelect.updateList(list);


        dialogsRealm.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<DialogsHolderRealm>>() {
            @Override
            public void onChange(RealmResults<DialogsHolderRealm> dialogsHolderRealms, OrderedCollectionChangeSet changeSet) {
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterDialogsSelect.removeItem(j);
                    }
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterDialogsSelect.addItem(dialogsHolderRealms.get(j));
                    }
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterDialogsSelect.updateItem(dialogsHolderRealms.get(j));
                    }
                }
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                final String dialogId = deletingDialog.dialogId;
                final String cryptionType = deletingDialog.cryptionType;


                firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + dialogId).removeValue();
                firebaseDatabase.getReference("messages/chats/" + dialogId + "/chatUsers/" + currentId).removeValue()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {

                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (!dialogId.equals(getString(R.string.public_chat))) {

                                    firebaseDatabase.getReference("messages/chats/" + dialogId + "/chatUsers")
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (!dataSnapshot.hasChildren()) {
                                                        firebaseDatabase.getReference("messages/chatHolder/" + dialogId).removeValue();
                                                        if (cryptionType.equals("localencrypted"))
                                                            firebaseDatabase.getReference("messages/chats/" + dialogId).removeValue();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                }
                            }
                        });
                break;
            case 1:

                firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + deletingDialog.dialogId).removeValue();
                firebaseDatabase.getReference("users/" + deletingDialog.userIdOpponent + "/dialogHolder/" + deletingDialog.dialogId).removeValue();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        if (dialogsRealm != null)
            dialogsRealm.removeAllChangeListeners();
        if (adapterDialogsSelect != null)
            adapterDialogsSelect.clear();
        if (firebaseAuth.getCurrentUser() == null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                }
            });
        }
        if (realm != null)
            realm.close();
        super.onDestroyView();
    }
}
