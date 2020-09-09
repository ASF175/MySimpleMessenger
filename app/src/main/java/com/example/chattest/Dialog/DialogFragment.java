package com.example.chattest.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chattest.Main.RootActivity;
import com.example.chattest.R;
import com.example.chattest.RealmObjects.AddUserRealm;
import com.example.chattest.RealmObjects.AdvancedMessageRealm;
import com.example.chattest.RealmObjects.DialogUserRealm;
import com.example.chattest.RealmObjects.DialogsHolderRealm;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.fbObjects.ChatHolder;
import com.example.chattest.fbObjects.Message;
import com.example.chattest.tools.CustomToolbar;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import io.realm.ImportFlag;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class DialogFragment extends Fragment {
    private DatabaseReference messagesRef, dialogHolderRef, dialogHolderRef1, dialogHolderRef2, privateDialogRef1, privateDialogRef2;
    private Realm realm;
    private RealmResults<AdvancedMessageRealm> messagesRealm;
    private RealmResults<DialogsHolderRealm> currentDialog;
    private RealmResults<DialogUserRealm> dialogUsersRealms;
    private RealmResults<AddUserRealm> dialogOpponentRealm;
    private String dialogId, chatTitle, userIdOpponent, userIdAuthor, decryptedAES, cryptionType, sign;
    private String currentLogin, currentId;
    private String avaString;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private NavController navController;

    private TextView message;
    private Button btnSend;
    private CustomToolbar toolbar;

    private Signature signature;
    private Cipher cipherAES, cipherRSA;
    private PrivateKey myPrivateKey;
    private KeyGenerator keyGenerator;

    private AdapterMessage adapterMessage;
    private AdvancedMessageRealm editingMessage;
    private int editingPosition;

    private boolean editing = false;
    private boolean typeChat = false;

    private RecyclerView mess_rec;

    private ChildEventListener childEventListener;
    private Context context;

    private boolean canWrite;

    private AlertDialog dialogInfo;

    private Toast dataToast;

    private Thread onlineThread;
    private MyConnectionListener.ConnectionHandler connectionHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Realm.init(getContext());
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        message = view.findViewById(R.id.editText);
        btnSend = view.findViewById(R.id.button);
        mess_rec = view.findViewById(R.id.messages_recycler);
        context = view.getContext();
        toolbar = new CustomToolbar(getContext());

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        navController = NavHostFragment.findNavController(this);

        startInit();
        recyclerInit();
        dbInit();
        btnSendInit();

        super.onViewCreated(view, savedInstanceState);
    }

    private void startInit() {
        assert getArguments() != null;
        dialogId = getArguments().getString("dialogId");
        typeChat = getArguments().getString("type").equals("chat");
        chatTitle = getArguments().getString("title");
        avaString = getArguments().getString("url");
        decryptedAES = getArguments().getString("decryptedAES");
        cryptionType = getArguments().getString("cryptionType");
        sign = getArguments().getString("sign");
        canWrite = getArguments().getString("canWrite").equals("true");
        userIdOpponent = getArguments().getString("userIdOpponent");
        userIdAuthor = getArguments().getString("userIdAuthor");

        currentDialog = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findAll();

        if (!canWrite) {
            btnSend.setEnabled(false);
            message.setError("Sending messages denied. Recreate dialog for future use.");
        }

        currentId = firebaseAuth.getCurrentUser().getUid();
        currentLogin = firebaseAuth.getCurrentUser().getDisplayName();

        if (cryptionType.equals("localencrypted") & canWrite) {
            RealmResults<KeysRealm> keyR = realm.where(KeysRealm.class).equalTo("keyName", currentId).findAll();
            try {
                keyGenerator = KeyGenerator.getInstance("AES");
                myPrivateKey = MyEncryptionProvider.getPrivateKey(keyR.first().getKey());
                cipherRSA = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
                signature = Signature.getInstance("SHA256withRSA");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException e) {
                e.printStackTrace();
            }


            messagesRef = firebaseDatabase.getReference("users").child(userIdOpponent)
                    .child("privateDialogs").child(dialogId).child("messages");
        } else {
            if (typeChat) {
                messagesRef = firebaseDatabase.getReference("messages/chats/" + dialogId + "/messages");
            } else {
                messagesRef = firebaseDatabase.getReference("messages/dialogs/" + dialogId + "/messages");
            }
        }

        if (!cryptionType.equals("none") & canWrite) {
            try {
                cipherAES = Cipher.getInstance("AES");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                e.printStackTrace();
            }
        }

        if (typeChat) {
            dialogHolderRef = firebaseDatabase.getReference("messages/chatHolder/" + dialogId + "/" + dialogId);
        } else {
            dialogHolderRef1 = firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + dialogId);
            dialogHolderRef2 = firebaseDatabase.getReference("users/" + userIdOpponent + "/dialogHolder/" + dialogId);
        }


        if (currentDialog.isEmpty()) {
            navController.navigate(R.id.action_dialogFragment_to_dialogSelectFragment);
            Toast.makeText(context, "Something went wrong, current dialog was deleted.", Toast.LENGTH_LONG).show();
        } else {
            currentDialog.addChangeListener(new RealmChangeListener<RealmResults<DialogsHolderRealm>>() {
                @Override
                public void onChange(RealmResults<DialogsHolderRealm> dialogsHolderRealms) {
                    if (dialogsHolderRealms.isEmpty()) {
                        Toast.makeText(context, "Something went wrong, current dialog was deleted.", Toast.LENGTH_LONG).show();
                        if (childEventListener != null) {
                            messagesRef.removeEventListener(childEventListener);
                            adapterMessage.clearItems();
                        }
                        if (messagesRealm != null)
                            messagesRealm.removeAllChangeListeners();
                        if (currentDialog != null)
                            currentDialog.removeAllChangeListeners();
                        if (dialogUsersRealms != null)
                            dialogUsersRealms.removeAllChangeListeners();
                        if (dialogOpponentRealm != null)
                            dialogOpponentRealm.removeAllChangeListeners();
                        if (onlineThread != null) {
                            onlineThread.interrupt();
                            onlineThread = null;
                        }
                        if (connectionHandler != null)
                            RootActivity.listener.removeConnectionListener(connectionHandler);

                        realm.close();
                        navController.navigate(R.id.action_dialogFragment_to_dialogSelectFragment);
                        return;
                    }
                    if (dialogsHolderRealms.first().getCanWrite().equals("false")) {
                        btnSend.setVisibility(View.GONE);
                        message.setError("Sending messages denied. Recreate dialog for future use.");
                    }
                }
            });
        }

        if (avaString.isEmpty()) {
            if (typeChat) {
                Picasso.get().load(R.drawable.chat_plaseholder_inversed).fit().into(toolbar.getLogoView());
            } else {
                Picasso.get().load(R.drawable.user_placeholder_inversed).fit().into(toolbar.getLogoView());
            }
        } else {
            Picasso.get().load(avaString).fit().into(toolbar.getLogoView());
        }
        toolbar.setTitle(chatTitle);
        switch (cryptionType) {
            case "none":
                Picasso.get().load(R.drawable.icon_none).fit().into(toolbar.getEncryptView());
                break;
            case "cloudencrypted":
                Picasso.get().load(R.drawable.icon_cloud_encrypted).fit().into(toolbar.getEncryptView());
                break;
            case "localencrypted":
                Picasso.get().load(R.drawable.icon_local_encrypted).fit().into(toolbar.getEncryptView());
                break;
        }
        toolbar.getEncryptView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogInfo == null) {
                    dialogInfo = createDialog(cryptionType);
                }
                dialogInfo.show();
            }
        });

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<AdvancedMessageRealm> findedMessages = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", dialogId).findAll();
                findedMessages.setBoolean("readed", true);
                realm.copyToRealmOrUpdate(findedMessages, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);

                if (!currentDialog.isEmpty())
                    currentDialog.first().setUnreadedMessagesCount(0);
                realm.copyToRealmOrUpdate(currentDialog.first(), ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
            }
        });

        btnSend.setEnabled(RootActivity.listener.isNetworkAvailable() & canWrite);
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                btnSend.setEnabled(networkAvailable & canWrite);
            }
        };
        RootActivity.listener.addConnectionListener(connectionHandler);

    }

    private AlertDialog createDialog(String cryptionType) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setMessage("Encryption info");
        switch (cryptionType) {
            case "none":
                adb.setMessage(R.string.none_encryption_description);
                break;
            case "cloudencrypted":
                adb.setMessage(R.string.cloud_encryption_description);
                break;
            case "localencrypted":
                adb.setMessage(R.string.local_encryption_description);
                break;
        }
        adb.setIcon(android.R.drawable.ic_dialog_info);
        adb.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return adb.create();
    }

    private void recyclerInit() {
        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        mLayoutManager.setStackFromEnd(true);

        mess_rec.setLayoutManager(mLayoutManager);
        mess_rec.setItemViewCacheSize(20);
        mess_rec.setDrawingCacheEnabled(true);
        mess_rec.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        AdapterMessage.OnMessageContextListener onMessageContextListener = new AdapterMessage.OnMessageContextListener() {
            @Override
            public void onMessageContext(AdvancedMessageRealm msg, int position) {
                editingMessage = msg;
                editingPosition = position;
            }
        };


        adapterMessage = new AdapterMessage(onMessageContextListener, width);
        mess_rec.setAdapter(adapterMessage);
        mess_rec.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mess_rec.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mess_rec.getAdapter().getItemCount() > 0) {
                                mess_rec.smoothScrollToPosition(adapterMessage.getItemCount() - 1);
                            }
                        }
                    }, 200);
                }
            }
        });
    }

    void dbInit() {

        messagesRealm = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", dialogId).sort("time").findAll();
        ArrayList<AdvancedMessageRealm> list = new ArrayList<>(messagesRealm);
        adapterMessage.updateList(list);
        if (adapterMessage.getItemCount() > 0) {
            mess_rec.smoothScrollToPosition(adapterMessage.getItemCount() - 1);
        }


        messagesRealm.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<AdvancedMessageRealm>>() {
            @Override
            public void onChange(RealmResults<AdvancedMessageRealm> advancedMessageRealms, OrderedCollectionChangeSet changeSet) {
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterMessage.removeItem(j);
                    }
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterMessage.addItem(advancedMessageRealms.get(j));
                        if (adapterMessage.getItemCount() > 0)
                            mess_rec.smoothScrollToPosition(adapterMessage.getItemCount() - 1);
                    }
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapterMessage.updateItem(advancedMessageRealms.get(j));
                    }
                }
            }
        });

        dialogUsersRealms = realm.where(DialogUserRealm.class).equalTo("dialogId", dialogId).findAll();


        if (typeChat) {
            toolbar.setSubtitle(dialogUsersRealms.size() + (dialogUsersRealms.size() == 1 ? " user" : " users"));
            dialogUsersRealms.addChangeListener(new RealmChangeListener<RealmResults<DialogUserRealm>>() {
                @Override
                public void onChange(RealmResults<DialogUserRealm> dialogUserRealms) {
                    toolbar.setSubtitle(dialogUsersRealms.size() + (dialogUsersRealms.size() == 1 ? " user" : " users"));
                }
            });
        } else {
            dialogOpponentRealm = realm.where(AddUserRealm.class).equalTo("userId", userIdOpponent).findAll();
            dialogOpponentRealm.addChangeListener(new RealmChangeListener<RealmResults<AddUserRealm>>() {
                @Override
                public void onChange(RealmResults<AddUserRealm> addUserRealms) {
                    handleTime(addUserRealms.first().lastOnline);
                }
            });

            final Handler onlineHandler = new Handler();
            onlineThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        onlineHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleTime(dialogOpponentRealm.first().lastOnline);
                            }
                        });
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            });
            onlineThread.start();
        }

        toolbar.getClickableSection().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typeChat) {
                    Bundle bundle = new Bundle();
                    bundle.putString("dialogId", dialogId);
                    bundle.putString("dialogName", chatTitle);
                    bundle.putString("dialogUri", avaString);
                    bundle.putString("type", cryptionType);
                    bundle.putString("authorId", userIdAuthor);
                    navController.navigate(R.id.action_dialogFragment_to_dialogInfoFragment, bundle);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", userIdOpponent);
                    bundle.putString("userName", chatTitle);
                    bundle.putString("userUri", avaString);
                    bundle.putLong("lastOnline", dialogOpponentRealm.first().lastOnline);
                    bundle.putString("type", "1");
                    navController.navigate(R.id.action_dialogFragment_to_userInfoFragment, bundle);
                }
            }
        });
    }

    private void handleTime(long lastOnline) {
        Calendar current = new GregorianCalendar();
        Calendar usertime = new GregorianCalendar();
        usertime.setTimeInMillis(lastOnline);
        if (current.get(Calendar.DAY_OF_YEAR) != usertime.get(Calendar.DAY_OF_YEAR) | current.get(Calendar.YEAR) != usertime.get(Calendar.YEAR))
            toolbar.setSubtitle("last seen " + DateFormat.format("d MMM", lastOnline) + " at " + DateFormat.format("HH:mm", lastOnline));
        else
            toolbar.setSubtitle(new Date().getTime() - lastOnline < 60000 ? "Online" : "last seen at " + DateFormat.format("HH:mm", lastOnline));
    }

    private void btnSendInit() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (message.getText().toString().equals(""))
                    return;
                if (typeChat) {
                    switch (cryptionType) {
                        case "none":
                            String smessage1 = DialogFragment.this.message.getText().toString();

                            Message message1 = new Message("chat", smessage1, currentId, "", "");
                            if (editing) {
                                HashMap<String, Object> updm = new HashMap<>();
                                updm.put("mess", smessage1);
                                messagesRef.child(editingMessage.messageId).updateChildren(updm);
                                if (editingPosition == adapterMessage.getItemCount() - 1) {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage1);
                                    dialogHolderRef.updateChildren(upd);
                                }
                                editing = false;
                            } else {
                                HashMap<String, Object> upd = new HashMap<>();
                                upd.put("text", smessage1);
                                upd.put("userIdMessage", currentId);
                                upd.put("time", new Date().getTime());
                                dialogHolderRef.updateChildren(upd);
                                messagesRef.push().setValue(message1);
                            }
                            DialogFragment.this.message.setText("");
                            break;
                        case "cloudencrypted":
                            try {

                                cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                String smessage2 = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                        .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                Message message2 = new Message("chat", smessage2, currentId, "", "");
                                if (editing) {
                                    HashMap<String, Object> updm = new HashMap<>();
                                    updm.put("mess", smessage2);
                                    messagesRef.child(editingMessage.messageId).updateChildren(updm);
                                    if (editingPosition == adapterMessage.getItemCount() - 1) {
                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", smessage2);
                                        dialogHolderRef.updateChildren(upd);
                                    }
                                    editing = false;
                                } else {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage2);
                                    upd.put("userIdMessage", currentId);
                                    upd.put("time", new Date().getTime());
                                    dialogHolderRef.updateChildren(upd);
                                    messagesRef.push().setValue(message2);
                                }
                                DialogFragment.this.message.setText("");
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "localencrypted":
                            try {
                                String id;

                                if (editing) {


                                    final AdvancedMessageRealm msg = new AdvancedMessageRealm("chat", currentLogin, message.getText().toString(), editingMessage.messageId, editingMessage.userId, editingMessage.dialogId, firebaseAuth.getCurrentUser().getPhotoUrl() == null ? "" : firebaseAuth.getCurrentUser().getPhotoUrl().toString(), true);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealmOrUpdate(msg, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });
                                    if (editingPosition == adapterMessage.getItemCount() - 1) {

                                        cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                        String smessage3Holder = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                                .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", smessage3Holder);
                                        dialogHolderRef.updateChildren(upd);
                                    }
                                    editing = false;
                                } else {


                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                    String smessage3Holder = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                            .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage3Holder);
                                    upd.put("userIdMessage", currentId);
                                    upd.put("time", new Date().getTime());
                                    dialogHolderRef.updateChildren(upd);


                                    id = messagesRef.push().getKey();


                                    final AdvancedMessageRealm msg = new AdvancedMessageRealm("chat", currentLogin, message.getText().toString(), id, currentId, dialogId, firebaseAuth.getCurrentUser().getPhotoUrl() == null ? "" : firebaseAuth.getCurrentUser().getPhotoUrl().toString(), true);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealmOrUpdate(msg, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });


                                    for (DialogUserRealm userId : dialogUsersRealms) {

                                        if (!userId.getUserId().equals(currentId)) {
                                            AddUserRealm user = realm.where(AddUserRealm.class).equalTo("userId", userId.getUserId()).findFirst();

                                            SecretKey newAESkey = keyGenerator.generateKey();
                                            cipherAES.init(Cipher.ENCRYPT_MODE, newAESkey);


                                            signature.initSign(myPrivateKey);

                                            signature.update(newAESkey.getEncoded());
                                            signature.update(message.getText().toString().getBytes(StandardCharsets.UTF_8));
                                            String messageSign = Base64.encodeToString(signature.sign(), Base64.DEFAULT);


                                            String smessage3 = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                                    .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                            cipherRSA.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getPublicKey(user.publicKey));
                                            String encryptedAES = Base64.encodeToString(cipherRSA.doFinal(newAESkey.getEncoded()), Base64.DEFAULT);


                                            Message message3 = new Message("chat", smessage3, currentId, encryptedAES, messageSign);
                                            firebaseDatabase.getReference("users").child(user.getUserId()).child("privateDialogs")
                                                    .child(dialogId).child("messages").child(id).setValue(message3);
                                        }
                                    }
                                }
                                DialogFragment.this.message.setText("");
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                } else {

                    switch (cryptionType) {
                        case "none":
                            String smessage1 = DialogFragment.this.message.getText().toString();


                            Message message1 = new Message("dialog", smessage1, currentId, "", "");
                            if (editing) {
                                HashMap<String, Object> updm = new HashMap<>();
                                updm.put("mess", smessage1);
                                messagesRef.child(editingMessage.messageId).updateChildren(updm);
                                if (editingPosition == adapterMessage.getItemCount() - 1) {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage1);
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);
                                }
                                editing = false;
                            } else {
                                HashMap<String, Object> upd = new HashMap<>();
                                upd.put("text", smessage1);
                                upd.put("userIdMessage", currentId);
                                upd.put("time", new Date().getTime());
                                dialogHolderRef1.updateChildren(upd);
                                dialogHolderRef2.updateChildren(upd);

                                messagesRef.push().setValue(message1);
                            }
                            DialogFragment.this.message.setText("");
                            break;
                        case "cloudencrypted":
                            try {

                                cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                String smessage2 = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                        .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                Message message2 = new Message("dialog", smessage2, currentId, "", "");
                                if (editing) {
                                    HashMap<String, Object> updm = new HashMap<>();
                                    updm.put("mess", smessage2);
                                    messagesRef.child(editingMessage.messageId).updateChildren(updm);
                                    if (editingPosition == adapterMessage.getItemCount() - 1) {
                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", smessage2);
                                        dialogHolderRef1.updateChildren(upd);
                                        dialogHolderRef2.updateChildren(upd);
                                    }
                                    editing = false;
                                } else {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage2);
                                    upd.put("userIdMessage", currentId);
                                    upd.put("time", new Date().getTime());
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);

                                    messagesRef.push().setValue(message2);
                                }
                                DialogFragment.this.message.setText("");
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "localencrypted":
                            try {

                                SecretKey newAESkey = keyGenerator.generateKey();
                                cipherAES.init(Cipher.ENCRYPT_MODE, newAESkey);


                                signature.initSign(myPrivateKey);

                                signature.update(newAESkey.getEncoded());
                                signature.update(message.getText().toString().getBytes(StandardCharsets.UTF_8));
                                String messageSign = Base64.encodeToString(signature.sign(), Base64.DEFAULT);


                                String smessage3 = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                        .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                String smessage3Holder = Base64.encodeToString(cipherAES.doFinal(DialogFragment.this
                                        .message.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);


                                RealmResults<AddUserRealm> res = realm.where(AddUserRealm.class)
                                        .equalTo("userId", userIdOpponent).findAll();
                                PublicKey userKey = MyEncryptionProvider.getPublicKey(res.first().publicKey);
                                cipherRSA.init(Cipher.ENCRYPT_MODE, userKey);
                                String encryptedAES = Base64.encodeToString(cipherRSA.doFinal(newAESkey.getEncoded()), Base64.DEFAULT);


                                Message message3 = new Message("dialog", smessage3, currentId, encryptedAES, messageSign);
                                if (editing) {

                                    final AdvancedMessageRealm msg = new AdvancedMessageRealm("dialog", editingMessage.name, message.getText().toString(), editingMessage.messageId, editingMessage.userId, editingMessage.dialogId, "", true);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealmOrUpdate(msg, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });
                                    if (editingPosition == adapterMessage.getItemCount() - 1) {

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", smessage3Holder);
                                        dialogHolderRef1.updateChildren(upd);
                                    }
                                    editing = false;
                                } else {


                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", smessage3Holder);
                                    upd.put("userIdMessage", currentId);
                                    upd.put("time", new Date().getTime());
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);


                                    String id = messagesRef.push().getKey();
                                    messagesRef.child(id).setValue(message3);

                                    final AdvancedMessageRealm msg = new AdvancedMessageRealm("dialog", currentLogin, message.getText().toString(), id, currentId, dialogId, "", true);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealmOrUpdate(msg, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });
                                }
                                DialogFragment.this.message.setText("");
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SignatureException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:

                if (cryptionType.equals("localencrypted")) {

                    if (editingPosition == adapterMessage.getItemCount() - 1) {

                        if (adapterMessage.getItemCount() > 1) {
                            AdvancedMessageRealm tmp1 = adapterMessage.getItem(adapterMessage.getItemCount() - 2);

                            if (typeChat) {
                                try {
                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                    String messageEn = Base64.encodeToString(cipherAES.doFinal
                                            (tmp1.mess.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", messageEn);
                                    upd.put("userIdMessage", tmp1.userId);
                                    upd.put("time", tmp1.time);
                                    dialogHolderRef.updateChildren(upd);
                                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                    String messageEn = Base64.encodeToString(cipherAES.doFinal
                                            (tmp1.mess.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", messageEn);
                                    upd.put("userIdMessage", tmp1.userId);
                                    upd.put("time", tmp1.time);
                                    dialogHolderRef1.updateChildren(upd);
                                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {

                            if (typeChat) {
                                try {
                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                    String messageEn = Base64.encodeToString(cipherAES.doFinal
                                            ("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", messageEn);
                                    upd.put("userIdMessage", "");
                                    dialogHolderRef.updateChildren(upd);
                                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                    String messageEn = Base64.encodeToString(cipherAES.doFinal
                                            ("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", messageEn);
                                    upd.put("userIdMessage", "");
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);
                                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("messageId", editingMessage.messageId).findAll();
                            res.deleteAllFromRealm();
                        }
                    });
                } else {

                    if (cryptionType.equals("cloudencrypted")) {

                        if (editingPosition == adapterMessage.getItemCount() - 1) {

                            if (adapterMessage.getItemCount() > 1) {
                                AdvancedMessageRealm tmp = adapterMessage.getItem(adapterMessage.getItemCount() - 2);
                                if (typeChat) {
                                    try {
                                        cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                        String messageEn = Base64.encodeToString(cipherAES.doFinal
                                                (tmp.mess.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", messageEn);
                                        upd.put("userIdMessage", tmp.userId);
                                        upd.put("time", tmp.time);
                                        dialogHolderRef.updateChildren(upd);
                                    } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                        String messageEn = Base64.encodeToString(cipherAES.doFinal
                                                (tmp.mess.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", messageEn);
                                        upd.put("userIdMessage", tmp.userId);
                                        upd.put("time", tmp.time);
                                        dialogHolderRef1.updateChildren(upd);
                                        dialogHolderRef2.updateChildren(upd);
                                    } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {

                                if (typeChat) {
                                    try {
                                        cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                        String messageEn = Base64.encodeToString(cipherAES.doFinal
                                                ("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", messageEn);
                                        upd.put("userIdMessage", "");
                                        dialogHolderRef.updateChildren(upd);
                                    } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                                        String messageEn = Base64.encodeToString(cipherAES.doFinal
                                                ("".getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

                                        HashMap<String, Object> upd = new HashMap<>();
                                        upd.put("text", messageEn);
                                        upd.put("userIdMessage", "");
                                        dialogHolderRef1.updateChildren(upd);
                                        dialogHolderRef2.updateChildren(upd);
                                    } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        messagesRef.child(editingMessage.messageId).removeValue();
                    } else {


                        if (editingPosition == adapterMessage.getItemCount() - 1) {

                            if (adapterMessage.getItemCount() > 1) {
                                AdvancedMessageRealm tmp = adapterMessage.getItem(adapterMessage.getItemCount() - 2);
                                if (typeChat) {
                                    ChatHolder chatHolder = new ChatHolder("chat", tmp.dialogId, chatTitle, tmp.mess, userIdAuthor, tmp.userId, tmp.time, avaString, cryptionType);
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", tmp.mess);
                                    upd.put("userIdMessage", tmp.userId);
                                    upd.put("time", tmp.time);
                                    dialogHolderRef.updateChildren(upd);
                                } else {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", tmp.mess);
                                    upd.put("userIdMessage", tmp.userId);
                                    upd.put("time", tmp.time);
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);
                                }
                            } else {

                                if (typeChat) {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", "");
                                    upd.put("userIdMessage", "");
                                    dialogHolderRef.updateChildren(upd);
                                } else {
                                    HashMap<String, Object> upd = new HashMap<>();
                                    upd.put("text", "");
                                    upd.put("userIdMessage", "");
                                    dialogHolderRef1.updateChildren(upd);
                                    dialogHolderRef2.updateChildren(upd);
                                }
                            }
                        }
                    }
                    messagesRef.child(editingMessage.messageId).removeValue();
                }
                break;
            case 1:
                editing = true;
                message.setText(editingMessage.mess);
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        if (childEventListener != null) {
            messagesRef.removeEventListener(childEventListener);
            adapterMessage.clearItems();
        }
        if (messagesRealm != null)
            messagesRealm.removeAllChangeListeners();
        if (currentDialog != null)
            currentDialog.removeAllChangeListeners();
        if (dialogUsersRealms != null)
            dialogUsersRealms.removeAllChangeListeners();
        if (dialogOpponentRealm != null)
            dialogOpponentRealm.removeAllChangeListeners();
        if (onlineThread != null) {
            onlineThread.interrupt();
            onlineThread = null;
        }
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);

        if (!realm.isClosed()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<AdvancedMessageRealm> findedMessages = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", dialogId).findAll();
                    findedMessages.setBoolean("readed", true);
                    realm.copyToRealmOrUpdate(findedMessages, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);

                    if (!currentDialog.isEmpty()) {
                        currentDialog.first().setUnreadedMessagesCount(0);
                        realm.copyToRealmOrUpdate(currentDialog.first(), ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                }
            });
        }

        realm.close();
        super.onDestroyView();
    }
}