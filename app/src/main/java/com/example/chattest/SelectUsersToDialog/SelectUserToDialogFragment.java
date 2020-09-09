package com.example.chattest.SelectUsersToDialog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.Main.RootActivity;
import com.example.chattest.R;
import com.example.chattest.RealmObjects.AddUserRealm;
import com.example.chattest.RealmObjects.DialogsHolderRealm;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.fbObjects.AddChatUser;
import com.example.chattest.fbObjects.ChatHolder;
import com.example.chattest.fbObjects.DialogsHolder;
import com.example.chattest.fbObjects.ItemUserList;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class SelectUserToDialogFragment extends Fragment {

    Realm realm;
    RealmResults<AddUserRealm> usersRealm;
    AdapterSelectUserToDialog.OnUserLongClickListener onUserLongClickListener;
    AdapterSelectUserToDialog.OnUserClickListener onUserClickListener;
    private RecyclerView user_recycler;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private AdapterSelectUserToDialog adapterSelectUserToDialog;
    private String currentId, currentLogin, chatName;
    private ProgressDialog waiting;
    private ArrayList<ItemUserList> selectedUsers;
    private Context context;
    private NavController navController;
    private int dialogCounter, chatCounter;
    private Signature signature;
    private Cipher cipherAES, cipherRSA;
    private KeyGenerator keyGenerator;
    private PrivateKey myPrivateKey;
    private String encryption;
    private MyConnectionListener.ConnectionHandler connectionHandler;
    private boolean connectionFlag;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.select_user_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Button button = view.findViewById(R.id.button2);
        context = view.getContext();
        user_recycler = view.findViewById(R.id.user_recycler);
        navController = NavHostFragment.findNavController(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonCreateChat();
            }
        });
        waitingInit();
        onClickListenersInit();
        recyclerInit();
        initDB();

        connectionFlag = RootActivity.listener.isNetworkAvailable();
        button.setEnabled(RootActivity.listener.isNetworkAvailable());
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                button.setEnabled(networkAvailable);
                connectionFlag = networkAvailable;
            }
        };
        RootActivity.listener.addConnectionListener(connectionHandler);

        super.onViewCreated(view, savedInstanceState);
    }

    public void buttonCreateChat() {
        selectedUsers = new ArrayList<>();
        for (int i = 0; i < AdapterSelectUserToDialog.userlist.size(); i++) {
            if (AdapterSelectUserToDialog.userlist.get(i).selected) {
                selectedUsers.add(AdapterSelectUserToDialog.userlist.get(i));
            }
        }
        selectedUsers.add(new ItemUserList(currentLogin, currentId, "", true));
        if (selectedUsers.size() < 3) {
            Toast.makeText(context, "Please select at least 2 users to create chat.", Toast.LENGTH_LONG).show();
        } else {

            LayoutInflater li = LayoutInflater.from(context);
            View createChatView = li.inflate(R.layout.create_chat, null);
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
            final EditText userInput = createChatView.findViewById(R.id.input_text);
            final RadioGroup radioGroup = createChatView.findViewById(R.id.radio_select_encryption);
            final RadioButton btnNone = createChatView.findViewById(R.id.radio_none);
            btnNone.setChecked(true);

            mDialogBuilder.setView(createChatView)
                    .setCancelable(true)
                    .setPositiveButton("Create chat",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    chatName = userInput.getText().toString().equals("") ? "Chat" : userInput.getText().toString();
                                    switch (radioGroup.getCheckedRadioButtonId()) {
                                        case R.id.radio_none:
                                            encryption = "none";
                                            generateChat();
                                            break;
                                        case R.id.radio_cloud:
                                            encryption = "cloudencrypted";
                                            generateChat();
                                            break;
                                        case R.id.radio_local:
                                            encryption = "localencrypted";
                                            if (selectedUsers.size() > 6)
                                                Toast.makeText(context, "Please select no more than 5 users for local storage type.", Toast.LENGTH_LONG).show();
                                            else
                                                generateChat();
                                            break;
                                    }
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    public void generateChat() {
        waiting.show();

        final String chatId = firebaseDatabase.getReference("messages/chats").push().getKey();
        assert chatId != null;

        chatCounter = 0;

        if (encryption.equals("none")) {


            ChatHolder chatHolder = new ChatHolder("chat", chatId, chatName, "", currentId, "", "", encryption);
            firebaseDatabase.getReference("messages/chatHolder").child(chatId).child(chatId).setValue(chatHolder)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            chatCounter++;
                            if (chatCounter == selectedUsers.size() * 2 + 1) {
                                openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, "", "");
                            }
                        }
                    });

            for (int i = 0; i < selectedUsers.size(); i++) {
                AddChatUser addChatUser = new AddChatUser(selectedUsers.get(i).userId, selectedUsers.get(i).userName);
                firebaseDatabase.getReference("messages/chats/" + chatId + "/chatUsers/" + selectedUsers.get(i).userId).setValue(addChatUser)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                chatCounter++;
                                if (chatCounter == selectedUsers.size() * 2 + 1) {
                                    openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, "", "");
                                }
                            }
                        });

                DialogsHolder dialogsHolder = new DialogsHolder("chat", chatId, "", "", "", "", "", encryption, "", "");
                firebaseDatabase.getReference("users/" + selectedUsers.get(i).userId + "/dialogHolder/" + chatId).setValue(dialogsHolder)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                chatCounter++;
                                if (chatCounter == selectedUsers.size() * 2 + 1) {
                                    openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, "", "");
                                }
                            }
                        });
            }
        } else {

            try {
                keyGenerator = KeyGenerator.getInstance("AES");
                final SecretKey key = keyGenerator.generateKey();

                cipherAES = Cipher.getInstance("AES");
                cipherRSA = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
                signature = Signature.getInstance("SHA256withRSA");

                cipherAES.init(Cipher.ENCRYPT_MODE, key);

                myPrivateKey = MyEncryptionProvider.getPrivateKey(realm.where(KeysRealm.class).equalTo("keyName", currentId).findFirst().getKey());


                final String chatNameK = Base64.encodeToString(cipherAES.doFinal(chatName.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
                final String message = Base64.encodeToString(cipherAES.doFinal("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                signature.initSign(myPrivateKey);
                signature.update(key.getEncoded());
                signature.update(firebaseAuth.getCurrentUser().getUid().getBytes(StandardCharsets.UTF_8));
                final String sign = Base64.encodeToString(signature.sign(), Base64.DEFAULT);


                ChatHolder chatHolder = new ChatHolder("chat", chatId, chatNameK, message, currentId, "", "", encryption);
                firebaseDatabase.getReference("messages/chatHolder").child(chatId).child(chatId).setValue(chatHolder)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                chatCounter++;
                                if (chatCounter == selectedUsers.size() * 2 + 1) {
                                    openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT), sign);
                                }
                            }
                        });

                for (int i = 0; i < selectedUsers.size(); i++) {
                    PublicKey publicKey = MyEncryptionProvider.getPublicKey(realm.where(AddUserRealm.class).equalTo("userId", selectedUsers.get(i).userId).findFirst().publicKey);
                    AddChatUser addChatUser = new AddChatUser(selectedUsers.get(i).userId, selectedUsers.get(i).userName);


                    cipherRSA.init(Cipher.ENCRYPT_MODE, publicKey);
                    final String encryptedAES = Base64.encodeToString(cipherRSA.doFinal(key.getEncoded()), Base64.DEFAULT);

                    firebaseDatabase.getReference("messages/chats/" + chatId + "/chatUsers/" + selectedUsers.get(i).userId).setValue(addChatUser)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    chatCounter++;
                                    if (chatCounter == selectedUsers.size() * 2 + 1) {
                                        openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT), sign);
                                    }
                                }
                            });

                    DialogsHolder dialogsHolder = new DialogsHolder("chat", chatId, "", "", "", "", "", encryption, encryptedAES, sign);
                    firebaseDatabase.getReference("users/" + selectedUsers.get(i).userId + "/dialogHolder/" + chatId).setValue(dialogsHolder)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    chatCounter++;
                                    if (chatCounter == selectedUsers.size() * 2 + 1) {
                                        openDialog("chat", chatId, chatName, "", currentId, "", "true", encryption, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT), sign);
                                    }
                                }
                            });
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateDialog(final String userId, final String userName, final String avaURL, final String dialogId) {
        waiting.show();

        selectedUsers = new ArrayList<>();
        selectedUsers.add(new ItemUserList(currentLogin, currentId, firebaseAuth.getCurrentUser().getPhotoUrl() == null ? "" : firebaseAuth.getCurrentUser().getPhotoUrl().toString(), true));
        selectedUsers.add(new ItemUserList(userName, userId, avaURL, true));

        dialogCounter = 0;

        if (encryption.equals("none")) {

            for (int i = 0; i < selectedUsers.size(); i++) {

                final ItemUserList user2;
                if (i == 0) {
                    user2 = selectedUsers.get(1);
                } else {
                    user2 = selectedUsers.get(0);
                }

                AddChatUser addChatUser = new AddChatUser(selectedUsers.get(i).userId, selectedUsers.get(i).userName);
                firebaseDatabase.getReference("messages/dialogs/" + dialogId + "/users/" + selectedUsers.get(i).userId).setValue(addChatUser)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialogCounter++;
                                if (dialogCounter == selectedUsers.size() * 2) {
                                    openDialog("dialog", dialogId, userName, userId, currentId, avaURL, "true", encryption, "", "");
                                }
                            }
                        });

                DialogsHolder dialogsHolder = new DialogsHolder("dialog", dialogId, user2.userName, "", user2.userId, currentId, "", "none", "", "");
                firebaseDatabase.getReference("users/" + selectedUsers.get(i).userId + "/dialogHolder/" + dialogId).setValue(dialogsHolder)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialogCounter++;
                                if (dialogCounter == selectedUsers.size() * 2) {
                                    openDialog("dialog", dialogId, userName, userId, currentId, avaURL, "true", encryption, "", "");
                                }
                            }
                        });
            }
        } else {

            try {
                keyGenerator = KeyGenerator.getInstance("AES");
                final SecretKey key = keyGenerator.generateKey();

                cipherAES = Cipher.getInstance("AES");
                cipherRSA = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
                signature = Signature.getInstance("SHA256withRSA");

                cipherAES.init(Cipher.ENCRYPT_MODE, key);

                myPrivateKey = MyEncryptionProvider.getPrivateKey(realm.where(KeysRealm.class).equalTo("keyName", currentId).findFirst().getKey());


                signature.initSign(myPrivateKey);
                signature.update(key.getEncoded());
                signature.update(firebaseAuth.getCurrentUser().getUid().getBytes(StandardCharsets.UTF_8));
                final String sign = Base64.encodeToString(signature.sign(), Base64.DEFAULT);

                for (int i = 0; i < selectedUsers.size(); i++) {

                    final ItemUserList user2;
                    if (i == 0) {
                        user2 = selectedUsers.get(1);
                    } else {
                        user2 = selectedUsers.get(0);
                    }


                    final String chatNameK = Base64.encodeToString(cipherAES.doFinal(user2.userName.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
                    final String message = Base64.encodeToString(cipherAES.doFinal("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                    PublicKey publicKey = MyEncryptionProvider.getPublicKey(realm.where(AddUserRealm.class).equalTo("userId", selectedUsers.get(i).userId).findFirst().publicKey);
                    cipherRSA.init(Cipher.ENCRYPT_MODE, publicKey);
                    final String encryptedAES = Base64.encodeToString(cipherRSA.doFinal(key.getEncoded()), Base64.DEFAULT);

                    DialogsHolder dialogsHolder = new DialogsHolder("dialog", dialogId, chatNameK, message, user2.userId, currentId, "", encryption, encryptedAES, sign);
                    firebaseDatabase.getReference("users/" + selectedUsers.get(i).userId + "/dialogHolder/" + dialogId).setValue(dialogsHolder)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    dialogCounter++;
                                    if (dialogCounter == selectedUsers.size() * 2) {
                                        openDialog("dialog", dialogId, userName, userId, currentId, avaURL, "true", encryption, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT), sign);
                                    }
                                }
                            });

                    AddChatUser addChatUser = new AddChatUser(selectedUsers.get(i).userId, selectedUsers.get(i).userName);
                    firebaseDatabase.getReference("messages/dialogs/" + dialogId + "/users/" + selectedUsers.get(i).userId).setValue(addChatUser)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    dialogCounter++;
                                    if (dialogCounter == selectedUsers.size() * 2) {
                                        openDialog("dialog", dialogId, userName, userId, currentId, avaURL, "true", encryption, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT), sign);
                                    }
                                }
                            });
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDialog(String type, String dialogId, String dialogName, String userIdOpponent, String userIdAuthor, String avaUri, String canWrite, String cryptionType, String decryptedAES, String sign) {
        waiting.dismiss();
        Bundle bundle = new Bundle();
        bundle.putString("dialogId", dialogId);
        bundle.putString("type", type);
        bundle.putString("title", dialogName);
        bundle.putString("url", avaUri);
        bundle.putString("cryptionType", cryptionType);
        bundle.putString("decryptedAES", decryptedAES);
        bundle.putString("sign", sign);
        bundle.putString("canWrite", canWrite);
        bundle.putString("userIdOpponent", userIdOpponent);
        bundle.putString("userIdAuthor", userIdAuthor == null ? "" : userIdAuthor);

        navController.navigate(R.id.action_selectUserToDialogFragment_to_dialogFragment, bundle);
    }

    public void waitingInit() {
        waiting = new ProgressDialog(context);
        waiting.setCancelable(false);
        waiting.setMessage("Please wait.");
        waiting.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    void initDB() {
        currentId = firebaseAuth.getCurrentUser().getUid();
        currentLogin = firebaseAuth.getCurrentUser().getDisplayName();


        usersRealm = realm.where(AddUserRealm.class).sort("lastOnline", Sort.DESCENDING).findAll();
        ArrayList<ItemUserList> list = new ArrayList<>();
        for (AddUserRealm usr : usersRealm) {
            if (!usr.userId.equals(currentId))
                list.add(new ItemUserList(usr.userName, usr.userId, usr.avaUrl, false));
        }
        adapterSelectUserToDialog.updateList(list);
    }

    void recyclerInit() {
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        user_recycler.setLayoutManager(manager);
        user_recycler.setItemViewCacheSize(20);
        user_recycler.setDrawingCacheEnabled(true);
        user_recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(user_recycler.getContext(),
                manager.getOrientation());
        user_recycler.addItemDecoration(dividerItemDecoration);


        adapterSelectUserToDialog = new AdapterSelectUserToDialog(onUserClickListener, onUserLongClickListener);
        user_recycler.setAdapter(adapterSelectUserToDialog);
    }

    void onClickListenersInit() {
        onUserLongClickListener = new AdapterSelectUserToDialog.OnUserLongClickListener() {
            @Override
            public void onUserClick(ItemUserList itemUserList, int position) {
                if (!itemUserList.selected) {
                    ItemUserList item = new ItemUserList(itemUserList.userName, itemUserList.userId, itemUserList.avaUrl, true);
                    AdapterSelectUserToDialog.userlist.set(position, item);
                } else {
                    ItemUserList item = new ItemUserList(itemUserList.userName, itemUserList.userId, itemUserList.avaUrl, false);
                    AdapterSelectUserToDialog.userlist.set(position, item);
                }
            }
        };

        onUserClickListener = new AdapterSelectUserToDialog.OnUserClickListener() {
            @Override
            public void onUserClick(final ItemUserList itemUserList) {
                RealmResults<DialogsHolderRealm> results = realm.where(DialogsHolderRealm.class)
                        .equalTo("userIdOpponent", itemUserList.userId).findAll();

                if (!results.isEmpty()) {
                    DialogsHolderRealm findedDialog = results.first();

                    openDialog(findedDialog.type, findedDialog.dialogId, findedDialog.chatName, findedDialog.userIdOpponent, findedDialog.userIdAuthor, findedDialog.avaUri, findedDialog.canWrite, findedDialog.cryptionType, findedDialog.decryptedAES, findedDialog.sign);
                } else {
                    if (!connectionFlag) {
                        Toast.makeText(context, "No intentet connection, cant create dialog, sry.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    LayoutInflater li = LayoutInflater.from(context);
                    View createChatView = li.inflate(R.layout.create_chat, null);
                    AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                    final TextView text = createChatView.findViewById(R.id.tv);
                    text.setVisibility(View.GONE);
                    final EditText userInput = createChatView.findViewById(R.id.input_text);
                    userInput.setVisibility(View.GONE);
                    final RadioGroup radioGroup = createChatView.findViewById(R.id.radio_select_encryption);
                    final RadioButton btnNone = createChatView.findViewById(R.id.radio_none);
                    btnNone.setChecked(true);

                    final String dialogId = firebaseDatabase.getReference("messages").child("dialogs").push().getKey();
                    mDialogBuilder.setView(createChatView)
                            .setCancelable(true)
                            .setPositiveButton("Create dialog",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            switch (radioGroup.getCheckedRadioButtonId()) {
                                                case R.id.radio_none:
                                                    encryption = "none";
                                                    generateDialog(itemUserList.userId, itemUserList.userName, itemUserList.avaUrl, dialogId);
                                                    break;
                                                case R.id.radio_cloud:
                                                    encryption = "cloudencrypted";
                                                    generateDialog(itemUserList.userId, itemUserList.userName, itemUserList.avaUrl, dialogId);
                                                    break;
                                                case R.id.radio_local:
                                                    encryption = "localencrypted";
                                                    generateDialog(itemUserList.userId, itemUserList.userName, itemUserList.avaUrl, dialogId);
                                                    break;
                                            }
                                            dialog.cancel();
                                        }
                                    })
                            .setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).show();
                }
            }
        };
    }

    @Override
    public void onDestroyView() {
        adapterSelectUserToDialog.clearItems();
        realm.close();
        usersRealm.removeAllChangeListeners();
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);
        super.onDestroyView();
    }
}
