package com.example.chattest.Main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.chattest.R;
import com.example.chattest.RealmObjects.AddUserRealm;
import com.example.chattest.RealmObjects.AdvancedMessageRealm;
import com.example.chattest.RealmObjects.DialogUserRealm;
import com.example.chattest.RealmObjects.DialogsHolderRealm;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.fbObjects.AddChatUser;
import com.example.chattest.fbObjects.AddUser;
import com.example.chattest.fbObjects.ChatHolder;
import com.example.chattest.fbObjects.DialogsHolder;
import com.example.chattest.fbObjects.Message;
import com.example.chattest.tools.CustomToolbar;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class RootActivity extends AppCompatActivity implements StartFragment.OnAuthEvent {

    public static MyConnectionListener listener;
    NavController navController;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference dialogHolderRef, usersListRef;
    private String currentId, currentLogin;
    private ArrayList<String> chatListenersPath = new ArrayList<>();
    private ArrayList<String> messageListenersPath = new ArrayList<>();
    private ArrayList<String> dialogUsersListenersPath = new ArrayList<>();
    private ChildEventListener dialogHolderEventListener, chatEventListener, userEventListener, messagesEventListener, dialogUsersEventListener;
    private Realm realm;
    private RealmResults<DialogsHolderRealm> dialogsRealm;
    private RealmResults<AddUserRealm> usersRealm;
    private ArrayList<String> initialDialogsList;
    private ArrayList<String> initialUsersList;
    private Signature signature;
    private Cipher cipherAES, cipherRSA;
    private String type;
    private ProgressDialog progressDialog;
    private long currentIterationDialogs;
    private long currentIterationMessages;
    private PrivateKey myPrivateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final CustomToolbar customToolbar = new CustomToolbar(this);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationView navigationView = findViewById(R.id.nav_view);
        drawerLayout = findViewById(R.id.drawer_layout);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        try {
            cipherAES = Cipher.getInstance("AES");
            cipherRSA = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding");
            signature = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration
                .Builder(R.id.dialogSelectFragment, R.id.userCabinetFragment).setDrawerLayout(drawerLayout).build();
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                switch (destination.getId()) {
                    case R.id.startFragment:
                        customToolbar.setTitle("Messenger");
                        customToolbar.setSubtitle("");
                        break;
                    case R.id.signInFragment:
                        toolbar.setNavigationIcon(null);
                        customToolbar.setTitle("Sign in");
                        customToolbar.setSubtitle("");
                        break;
                    case R.id.signUpFragment:
                        customToolbar.setTitle("Sign up");
                        customToolbar.setSubtitle("");
                        break;
                    case R.id.waitFragment:
                        customToolbar.setTitle("Confirmation");
                        customToolbar.setSubtitle("");
                        break;
                    case R.id.dialogSelectFragment:
                        customToolbar.setTitle("Messenger");
                        customToolbar.setSubtitle("");
                        customToolbar.clearLogo();
                        customToolbar.clearEncrypt();
                        customToolbar.getEncryptViewInfo().setOnClickListener(null);
                        customToolbar.getClickableSection().setOnClickListener(null);
                        break;
                    case R.id.dialogFragment:
                        customToolbar.setTitle("");
                        break;
                    case R.id.selectUserToDialogFragment:
                        customToolbar.setTitle("Select users");
                        customToolbar.setSubtitle("");
                        break;
                    case R.id.userCabinetFragment:
                        customToolbar.setTitle("Account");
                        customToolbar.setSubtitle("");
                        customToolbar.clearLogo();
                        customToolbar.clearEncrypt();
                        customToolbar.getEncryptView().setOnClickListener(null);
                        customToolbar.getClickableSection().setOnClickListener(null);
                        break;
                    case R.id.dialogInfoFragment:
                        customToolbar.setTitle("Dialog information");
                        customToolbar.setSubtitle("");
                        customToolbar.clearLogo();

                        customToolbar.getClickableSection().setOnClickListener(null);
                        break;
                    case R.id.userInfoFragment:
                        customToolbar.setTitle("User information");
                        customToolbar.setSubtitle("");
                        customToolbar.clearLogo();
                        customToolbar.getClickableSection().setOnClickListener(null);
                        break;
                }
            }
        });

        listener = new MyConnectionListener();
        listener.setConnectionListener();
        listener.addConnectionListener(new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                Snackbar.make(findViewById(R.id.drawer_layout), networkAvailable ? "Connection established." : "Conection lost. Trying to reconect.", Snackbar.LENGTH_SHORT).show();

            }
        });


    }


    @Override
    public void onAuth(String type, ProgressDialog dialog) {
        this.type = type;
        this.progressDialog = dialog;
        setAuth();
    }

    private void setAuth() {
        currentId = firebaseAuth.getUid();
        currentLogin = firebaseAuth.getCurrentUser().getDisplayName();
        dialogHolderRef = firebaseDatabase.getReference("users").child(currentId).child("dialogHolder");
        usersListRef = firebaseDatabase.getReference("userslist");
        usersListRef.keepSynced(true);
        dialogHolderRef.keepSynced(true);
        RealmResults<KeysRealm> keyR = realm.where(KeysRealm.class).equalTo("keyName", currentId).findAll();

        if (keyR.isEmpty()) {


            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<KeysRealm> res = realm.where(KeysRealm.class).notEqualTo("keyName", "password").findAll();
                    if (!res.isEmpty())
                        res.deleteAllFromRealm();
                }
            });


            RealmResults<KeysRealm> pass = realm.where(KeysRealm.class).equalTo("keyName", "password").findAll();
            if (pass.isEmpty()) {
                Toast.makeText(this, "Oops, cant recreate your private key. Pls sign in again.", Toast.LENGTH_LONG).show();
                firebaseAuth.signOut();
                navController.navigate(R.id.action_startFragment_to_authentication);
            } else {
                final String password = pass.first().getKey();

                usersListRef.child(currentId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        AddUser user = dataSnapshot.getValue(AddUser.class);
                        try {

                            String encryptedKey = user.privateKey;
                            SecretKey secretKey = new SecretKeySpec(MyEncryptionProvider.getAesByPassword(password).getBytes(StandardCharsets.UTF_8), "AES");
                            cipherAES.init(Cipher.DECRYPT_MODE, secretKey);
                            byte[] decryptedKey = cipherAES.doFinal(Base64.decode(encryptedKey, Base64.DEFAULT));
                            final KeysRealm gettedPrivateKey = new KeysRealm(Base64.encodeToString(decryptedKey, Base64.DEFAULT), currentId);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealmOrUpdate(gettedPrivateKey, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            });

                            try {
                                myPrivateKey = MyEncryptionProvider.getPrivateKey(gettedPrivateKey.getKey());
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                e.printStackTrace();
                            }

                            setInitialUsers();
                        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        } else {
            try {
                myPrivateKey = MyEncryptionProvider.getPrivateKey(keyR.first().getKey());
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }

            setInitialUsers();
        }
    }


    private void setInitialUsers() {
        initialUsersList = new ArrayList<>();

        usersRealm = realm.where(AddUserRealm.class).findAll();

        usersListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    final AddUser item = data.getValue(AddUser.class);
                    final AddUserRealm user = new AddUserRealm(item.userName, item.avaUrl, item.publicKey, item.userId, item.lastOnline);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(user, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                        }
                    });
                    initialUsersList.add(item.userId);
                }
                for (final AddUserRealm userRealm : usersRealm) {
                    boolean flag = false;
                    for (String userId : initialUsersList) {
                        if (userRealm.userId.equals(userId)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                RealmResults<AddUserRealm> res = realm.where(AddUserRealm.class).equalTo("userId", userRealm.userId).findAll();
                                res.deleteAllFromRealm();
                            }
                        });
                    }
                }
                setInitialDialogs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void preloadImages() {
        Thread loadImages = new Thread(new Runnable() {
            @Override
            public void run() {
                Realm.init(getBaseContext());
                RealmConfiguration config = new RealmConfiguration.Builder()
                        .deleteRealmIfMigrationNeeded()
                        .build();
                Realm realm = Realm.getInstance(config);

                RealmResults<AddUserRealm> users = realm.where(AddUserRealm.class).findAll();
                for (AddUserRealm user : users) {
                    if (!user.avaUrl.equals(""))
                        Picasso.get().load(user.avaUrl).fetch();
                }
                RealmResults<DialogsHolderRealm> dialogs = realm.where(DialogsHolderRealm.class).findAll();
                for (DialogsHolderRealm dialog : dialogs) {
                    if (!dialog.getAvaUri().equals(""))
                        Picasso.get().load(dialog.getAvaUri()).fetch();
                }
                Picasso.get().load(R.drawable.user_placeholder).fetch();
                Picasso.get().load(R.drawable.user_placeholder_inversed).fetch();
                Picasso.get().load(R.drawable.icon_local_encrypted).fetch();
                Picasso.get().load(R.drawable.icon_none).fetch();
                Picasso.get().load(R.drawable.icon_cloud_encrypted).fetch();
                Picasso.get().load(R.drawable.img_download_placeholder).fetch();
                Picasso.get().load(R.drawable.img_download_placeholder_inversed).fetch();
                Picasso.get().load(R.drawable.message).fetch();
                Picasso.get().load(R.drawable.message_inversed).fetch();
                Picasso.get().load(R.drawable.user_placeholder).fetch();
                Picasso.get().load(R.drawable.user_placeholder_inversed).fetch();

                realm.close();
                realm = null;
            }
        });
        loadImages.setDaemon(true);
        loadImages.start();
    }


    private void setInitialDialogs() {
        initialDialogsList = new ArrayList<>();

        dialogsRealm = realm.where(DialogsHolderRealm.class).findAll();

        dialogHolderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final long dialogsCount = dataSnapshot.getChildrenCount();
                if (dialogsCount == 0) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).findAll();
                            if (!res.isEmpty())
                                res.deleteAllFromRealm();

                            RealmResults<AdvancedMessageRealm> res1 = realm.where(AdvancedMessageRealm.class).findAll();
                            if (!res1.isEmpty())
                                res1.deleteAllFromRealm();
                        }
                    });
                    setInitialMessages();
                    preloadImages();
                    return;
                }

                currentIterationDialogs = 0;
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    final DialogsHolder data = dataSnapshot1.getValue(DialogsHolder.class);
                    assert data != null;


                    initialDialogsList.add(data.dialogId);


                    if (data.type.equals("chat")) {
                        if (!data.cryptionType.equals("none")) {

                            RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                            if (res.isEmpty()) {


                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        DialogsHolderRealm dialog = new DialogsHolderRealm();
                                        dialog.setCryptionType(data.cryptionType);
                                        dialog.setSign(data.sign);
                                        dialog.setDecryptedAES(data.encryptedAES);
                                        dialog.setType(data.type);
                                        dialog.setCanWrite("");
                                        dialog.setDialogId(data.dialogId);
                                        dialog.setTime();
                                        realm.copyToRealmOrUpdate(dialog, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });
                            }
                        }

                        firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        RealmResults<DialogUserRealm> res = realm.where(DialogUserRealm.class)
                                                .equalTo("dialogId", data.dialogId).findAll();

                                        ArrayList<DialogUserRealm> userlist = new ArrayList<>();
                                        for (DataSnapshot data1 : dataSnapshot.getChildren()) {
                                            AddChatUser chatUser = data1.getValue(AddChatUser.class);
                                            final DialogUserRealm user = new DialogUserRealm(data.dialogId, chatUser.userId);
                                            userlist.add(user);
                                            realm.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    realm.copyToRealmOrUpdate(user, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                }
                                            });
                                        }

                                        for (final DialogUserRealm userR : res) {
                                            boolean flag = false;
                                            for (DialogUserRealm userL : userlist) {
                                                if (userR.getUserId().equals(userL.getUserId())) {
                                                    flag = true;
                                                    break;
                                                }
                                            }
                                            if (!flag) {
                                                realm.executeTransaction(new Realm.Transaction() {
                                                    @Override
                                                    public void execute(Realm realm) {
                                                        RealmResults<DialogUserRealm> res = realm.where(DialogUserRealm.class)
                                                                .equalTo("dialogId", data.dialogId).equalTo("userId", userR.getUserId()).findAll();
                                                        if (!res.isEmpty())
                                                            res.deleteAllFromRealm();
                                                    }
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });


                        DatabaseReference ref = firebaseDatabase.getReference("messages").child("chatHolder").child(data.dialogId).child(data.dialogId);
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                currentIterationDialogs++;

                                handleDatasnapshotDialogs(dataSnapshot, true, false);

                                if (currentIterationDialogs == dialogsCount) {
                                    for (final DialogsHolderRealm dialogRealm : dialogsRealm) {
                                        boolean flag = false;
                                        for (String dialogId : initialDialogsList) {
                                            if (dialogRealm.dialogId.equals(dialogId)) {
                                                flag = true;
                                                break;
                                            }
                                        }
                                        if (!flag) {
                                            realm.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    String dialogId = dialogRealm.dialogId;
                                                    RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findAll();
                                                    if (!res.isEmpty())
                                                        res.deleteAllFromRealm();

                                                    RealmResults<AdvancedMessageRealm> res1 = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", dialogId).findAll();
                                                    if (!res1.isEmpty())
                                                        res1.deleteAllFromRealm();


                                                    RealmResults<DialogUserRealm> res2 = realm.where(DialogUserRealm.class).equalTo("dialogId", dialogId).findAll();
                                                    if (!res2.isEmpty())
                                                        res2.deleteAllFromRealm();
                                                }
                                            });
                                        }
                                    }
                                    setInitialMessages();
                                    preloadImages();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } else {

                        firebaseDatabase.getReference("messages/dialogs/" + data.dialogId + "/users")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        RealmResults<DialogUserRealm> res = realm.where(DialogUserRealm.class)
                                                .equalTo("dialogId", data.dialogId).findAll();

                                        ArrayList<DialogUserRealm> userlist = new ArrayList<>();
                                        for (DataSnapshot data1 : dataSnapshot.getChildren()) {
                                            AddChatUser chatUser = data1.getValue(AddChatUser.class);
                                            final DialogUserRealm user = new DialogUserRealm(data.dialogId, chatUser.userId);
                                            userlist.add(user);
                                            realm.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    realm.copyToRealmOrUpdate(user, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                }
                                            });
                                        }

                                        for (final DialogUserRealm userR : res) {
                                            boolean flag = false;
                                            for (DialogUserRealm userL : userlist) {
                                                if (userR.getUserId().equals(userL.getUserId())) {
                                                    flag = true;
                                                    break;
                                                }
                                            }
                                            if (!flag) {
                                                realm.executeTransaction(new Realm.Transaction() {
                                                    @Override
                                                    public void execute(Realm realm) {
                                                        RealmResults<DialogUserRealm> res = realm.where(DialogUserRealm.class)
                                                                .equalTo("dialogId", data.dialogId).equalTo("userId", userR.getUserId()).findAll();
                                                        if (!res.isEmpty())
                                                            res.deleteAllFromRealm();
                                                    }
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                        currentIterationDialogs++;

                        handleDatasnapshotDialogs(dataSnapshot1, false, false);
                    }

                    if (currentIterationDialogs == dialogsCount) {
                        for (final DialogsHolderRealm dialogRealm : dialogsRealm) {
                            boolean flag = false;
                            for (String dialogId : initialDialogsList) {
                                if (dialogRealm.dialogId.equals(dialogId)) {
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        String dialogId = dialogRealm.dialogId;
                                        RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findAll();
                                        if (!res.isEmpty())
                                            res.deleteAllFromRealm();

                                        RealmResults<AdvancedMessageRealm> res1 = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", dialogId).findAll();
                                        if (!res1.isEmpty())
                                            res1.deleteAllFromRealm();


                                        RealmResults<DialogUserRealm> res2 = realm.where(DialogUserRealm.class).equalTo("dialogId", dialogId).findAll();
                                        if (!res2.isEmpty())
                                            res2.deleteAllFromRealm();
                                    }
                                });
                            }
                        }
                        setInitialMessages();
                        preloadImages();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void setInitialMessages() {

        RealmResults<DialogsHolderRealm> dialogsR = realm.where(DialogsHolderRealm.class).findAll();
        final int dialogsCount = dialogsR.size();
        currentIterationMessages = 0;

        if (dialogsCount == 0) {
            navToDialog();
            return;
        }

        for (final DialogsHolderRealm dialog : dialogsR) {
            if (dialog.canWrite.equals("false")) {
                currentIterationMessages++;


                if (currentIterationMessages == dialogsCount) {
                    navToDialog();
                }
                continue;
            }

            final DatabaseReference messagesRef;
            final RealmResults<AdvancedMessageRealm> messagesRealm = realm
                    .where(AdvancedMessageRealm.class).equalTo("dialogId", dialog.dialogId).findAll();

            final ArrayList<String> initialMessageList = new ArrayList<>();

            if (dialog.cryptionType.equals("localencrypted")) {
                messagesRef = firebaseDatabase.getReference("users").child(currentId)
                        .child("privateDialogs").child(dialog.dialogId).child("messages");
            } else {
                if (dialog.type.equals("chat")) {
                    messagesRef = firebaseDatabase.getReference("messages/chats/" + dialog.dialogId + "/messages");
                } else {
                    messagesRef = firebaseDatabase.getReference("messages/dialogs/" + dialog.dialogId + "/messages");
                }
            }

            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        final Message data = dataSnapshot1.getValue(Message.class);
                        final String id = dataSnapshot1.getKey();
                        assert data != null;

                        handleDataSnapshotMessages(dataSnapshot1, dialog.dialogId, dialog.cryptionType, dialog.decryptedAES);

                        initialMessageList.add(id);
                    }
                    for (final AdvancedMessageRealm msgRealm : messagesRealm) {
                        boolean flag = false;
                        for (String id : initialMessageList) {
                            if (msgRealm.messageId.equals(id)) {
                                flag = true;
                                break;
                            }
                        }

                        if (!flag & !dialog.cryptionType.equals("localencrypted")) {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("messageId", msgRealm.messageId).findAll();
                                    res.deleteAllFromRealm();
                                }
                            });
                        }
                    }


                    if (dialog.cryptionType.equals("localencrypted")) {
                        messagesRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                currentIterationMessages++;


                                if (currentIterationMessages == dialogsCount) {
                                    navToDialog();
                                }
                            }
                        });
                    } else {
                        currentIterationMessages++;


                        if (currentIterationMessages == dialogsCount) {
                            navToDialog();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void navToDialog() {
        setDialogsListeners();
        setUserslistListeners();
        setMyOnlineUpdater();

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        switch (type) {
            case "in":
                Toast.makeText(RootActivity.this, "Welcome back, " + currentLogin + "!", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.action_global_dialogSelectFragment);
                break;
            case "reg":
                Toast.makeText(RootActivity.this, currentLogin + ", you are successfully registered.", Toast.LENGTH_LONG).show();
                navController.navigate(R.id.action_global_dialogSelectFragment);
                break;
            case "start":
                Toast.makeText(RootActivity.this, "Welcome back, " + currentLogin + "!", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.action_startFragment_to_dialogSelectFragment);
                break;
            case "regstart":
                Toast.makeText(RootActivity.this, currentLogin + ", you are successfully registered.", Toast.LENGTH_LONG).show();
                navController.navigate(R.id.action_startFragment_to_dialogSelectFragment);
                break;
        }
    }

    private void setMyOnlineUpdater() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                firebaseDatabase.getReference("userslist").child(currentId).child("lastOnline").setValue(new Date().getTime());
            }
        };
        Thread onlineUpdater = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        handler.sendEmptyMessage(0);
                    }
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        onlineUpdater.setDaemon(true);
        onlineUpdater.start();
    }


    private void setMessagesListener(@NotNull String path, final String ref, @NotNull final String cryptionType, final String decryptedAES) {
        messagesEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                handleDataSnapshotMessages(dataSnapshot, ref, cryptionType, decryptedAES);


                if (cryptionType.equals("localencrypted")) {
                    firebaseDatabase.getReference("users").child(currentId).child("privateDialogs").child(ref)
                            .child("messages").child(dataSnapshot.getKey()).removeValue();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                handleDataSnapshotMessages(dataSnapshot, ref, cryptionType, decryptedAES);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                if (!cryptionType.equals("localencrypted")) {
                    final String id = dataSnapshot.getKey();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("messageId", id).findAll();
                            if (!res.isEmpty()) {
                                res.deleteAllFromRealm();
                            }
                        }
                    });
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };


        if (cryptionType.equals("localencrypted")) {
            firebaseDatabase.getReference("users").child(currentId).child("privateDialogs").child(ref).child("messages")
                    .addChildEventListener(messagesEventListener);
            messageListenersPath.add("users/" + currentId + "/privateDialogs/" + ref + "/messages");
        } else {

            firebaseDatabase.getReference(path).addChildEventListener(messagesEventListener);
            messageListenersPath.add(path);
        }
    }


    private void setDialogsListeners() {
        chatEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                final ChatHolder data = dataSnapshot.getValue(ChatHolder.class);
                handleDatasnapshotDialogs(dataSnapshot, true, true);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                handleDatasnapshotDialogs(dataSnapshot, true, false);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                final ChatHolder data = dataSnapshot.getValue(ChatHolder.class);
                assert data != null;

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {

                        RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                        if (!res.isEmpty())
                            res.deleteAllFromRealm();

                        RealmResults<AdvancedMessageRealm> res1 = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", data.dialogId).findAll();
                        if (!res1.isEmpty())
                            res1.deleteAllFromRealm();
                    }
                });
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        dialogHolderEventListener = new ChildEventListener() {


            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                final DialogsHolder data = dataSnapshot.getValue(DialogsHolder.class);
                assert data != null;
                if (data.type.equals("chat")) {


                    if (!data.cryptionType.equals("none")) {
                        RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                        if (res.isEmpty()) {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    DialogsHolderRealm emptyDialog = new DialogsHolderRealm();
                                    emptyDialog.setCryptionType(data.cryptionType);
                                    emptyDialog.setSign(data.sign);
                                    emptyDialog.setDecryptedAES(data.encryptedAES);
                                    emptyDialog.setType(data.type);
                                    emptyDialog.setCanWrite("");
                                    emptyDialog.setTime();
                                    emptyDialog.setDialogId(data.dialogId);
                                    realm.copyToRealmOrUpdate(emptyDialog, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            });
                        }
                    }


                    dialogUsersListenersPath.add("messages/chats/" + data.dialogId + "/chatUsers");
                    setDialogUsersListener(data.dialogId, true);


                    firebaseDatabase.getReference("messages/chatHolder/" + data.dialogId).
                            addChildEventListener(chatEventListener);
                    chatListenersPath.add("messages/chatHolder/" + data.dialogId);
                } else {

                    dialogUsersListenersPath.add("messages/dialogs/" + data.dialogId + "/users");
                    setDialogUsersListener(data.dialogId, false);

                    handleDatasnapshotDialogs(dataSnapshot, false, data.canWrite.equals("true") ? true : false);
                }
            }


            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                DialogsHolder data = dataSnapshot.getValue(DialogsHolder.class);
                if (data.type.equals("dialog"))
                    handleDatasnapshotDialogs(dataSnapshot, false, false);


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                final DialogsHolder data = dataSnapshot.getValue(DialogsHolder.class);
                assert data != null;

                if (data.type.equals("chat")) {

                    firebaseDatabase.getReference("messages/chatHolder/" + data.dialogId)
                            .removeEventListener(chatEventListener);
                    chatListenersPath.remove("messages/chatHolder/" + data.dialogId);


                    if (data.cryptionType.equals("localencrypted"))
                        firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId).removeValue();


                    if (data.cryptionType.equals("localencrypted")) {
                        firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages").removeEventListener(messagesEventListener);
                        messageListenersPath.remove("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                    } else {
                        firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/messages").removeEventListener(messagesEventListener);
                        messageListenersPath.remove("messages/chats/" + data.dialogId + "/messages");
                    }
                } else {

                    if (data.cryptionType.equals("localencrypted")) {

                        firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId).removeValue();
                        firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages").removeEventListener(messagesEventListener);
                        messageListenersPath.remove("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                    } else {

                        firebaseDatabase.getReference("messages/dialogs/" + data.dialogId).removeValue();
                        firebaseDatabase.getReference("messages/dialogs/" + data.dialogId + "/messages").removeEventListener(messagesEventListener);
                        messageListenersPath.remove("messages/dialogs/" + data.dialogId + "/messages");
                    }
                }

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                        if (!res.isEmpty())
                            res.deleteAllFromRealm();

                        RealmResults<AdvancedMessageRealm> res1 = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", data.dialogId).findAll();
                        if (!res1.isEmpty())
                            res1.deleteAllFromRealm();
                    }
                });
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        dialogHolderRef.addChildEventListener(dialogHolderEventListener);
    }


    private void setUserslistListeners() {
        userEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                AddUser item = dataSnapshot.getValue(AddUser.class);
                final AddUserRealm user = new AddUserRealm(item.userName, item.avaUrl, item.publicKey, item.userId, item.lastOnline);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(user, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                AddUser item = dataSnapshot.getValue(AddUser.class);
                final AddUserRealm user = new AddUserRealm(item.userName, item.avaUrl, item.publicKey, item.userId, item.lastOnline);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(user, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                final AddUser item = dataSnapshot.getValue(AddUser.class);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmResults<AddUserRealm> res = realm.where(AddUserRealm.class).equalTo("userId", item.userId).findAll();
                        res.deleteAllFromRealm();
                    }
                });
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        usersListRef.addChildEventListener(userEventListener);
    }


    private void setDialogUsersListener(final String dialogId, boolean chat) {
        dialogUsersEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                AddChatUser user = dataSnapshot.getValue(AddChatUser.class);
                final DialogUserRealm userRealm = new DialogUserRealm(dialogId, user.userId);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(userRealm, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                AddChatUser user = dataSnapshot.getValue(AddChatUser.class);
                final DialogUserRealm userRealm = new DialogUserRealm(dialogId, user.userId);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(userRealm, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                final AddChatUser user = dataSnapshot.getValue(AddChatUser.class);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmResults<DialogUserRealm> res = realm.where(DialogUserRealm.class).equalTo("dialogId", dialogId)
                                .equalTo("userId", user.userId).findAll();
                        if (!res.isEmpty())
                            res.deleteAllFromRealm();
                    }
                });
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        if (chat) {
            firebaseDatabase.getReference("messages/chats/" + dialogId + "/chatUsers")
                    .addChildEventListener(dialogUsersEventListener);
        } else {
            firebaseDatabase.getReference("messages/dialogs/" + dialogId + "/users")
                    .addChildEventListener(dialogUsersEventListener);
        }
    }


    private void handleDatasnapshotDialogs(@NotNull DataSnapshot dataSnapshot, boolean chat, boolean setListener) {
        if (dataSnapshot.getValue() == null)
            return;

        if (chat) {
            final ChatHolder data = dataSnapshot.getValue(ChatHolder.class);
            RealmResults<DialogsHolderRealm> ress = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
            long unreadedMessagesCount = ress.isEmpty() ? 0 : ress.first().unreadedMessagesCount;

            AddUserRealm userAuthor, messageUser;
            RealmResults<AddUserRealm> results = realm.where(AddUserRealm.class).equalTo("userId", data.userIdAuthor).findAll();
            if (results.isEmpty()) {


                userAuthor = new AddUserRealm("", "", "", "", 0);
            } else {
                userAuthor = results.first();
            }

            RealmResults<AddUserRealm> results1 = realm.where(AddUserRealm.class).equalTo("userId", data.userIdMessage).findAll();
            if (results1.isEmpty()) {


                messageUser = new AddUserRealm("", "", "", "", 0);
            } else {
                messageUser = results1.first();
            }

            switch (data.cryptionType) {
                case "none":

                    final DialogsHolderRealm dialog1 = new DialogsHolderRealm(data.type, data.dialogId, data.chatName, messageUser.userName, data.text, "", data.userIdAuthor, data.time, messageUser.userId, data.avaUrl, data.cryptionType, "", "", unreadedMessagesCount, messageUser.avaUrl);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(dialog1, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                        }
                    });

                    if (setListener) {

                        setMessagesListener("messages/chats/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, "");
                        messageListenersPath.add("messages/chats/" + data.dialogId + "/messages");
                    }
                    break;
                case "cloudencrypted":


                    RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                    if (!res.isEmpty()) {
                        DialogsHolderRealm findedDialog = res.first();


                        if (findedDialog.getCanWrite().equals("")) {
                            try {
                                PublicKey publicKey = MyEncryptionProvider.getPublicKey(userAuthor.publicKey);

                                cipherRSA.init(Cipher.DECRYPT_MODE, myPrivateKey);

                                byte[] aes_byte = cipherRSA.doFinal(Base64.decode(findedDialog.decryptedAES, Base64.DEFAULT));

                                signature.initVerify(publicKey);
                                signature.update(aes_byte);
                                signature.update(data.userIdAuthor.getBytes(StandardCharsets.UTF_8));


                                if (signature.verify(Base64.decode(findedDialog.sign, Base64.DEFAULT))) {
                                    if (setListener) {

                                        setMessagesListener("messages/chats/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT));
                                        messageListenersPath.add("messages/chats/" + data.dialogId + "/messages");
                                    }

                                    cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(aes_byte));
                                    String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                    String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);


                                    final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, "", data.userIdAuthor, data.time, messageUser.userId, data.avaUrl, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT), findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });

                                } else {
                                    firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).removeValue();
                                    firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers/" + currentId).removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {

                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (!data.dialogId.equals(getString(R.string.public_chat))) {

                                                        firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers")
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        if (!dataSnapshot.hasChildren()) {
                                                                            firebaseDatabase.getReference("messages/chats/" + data.dialogId).removeValue();
                                                                            firebaseDatabase.getReference("messages/chatHolder/" + data.dialogId).removeValue();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });
                                                    }
                                                }
                                            });

                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", data.dialogId).findAll();
                                            res.deleteAllFromRealm();
                                        }
                                    });
                                    Toast.makeText(RootActivity.this, "Chat sign is not valid, unable to create chat.", Toast.LENGTH_LONG).show();
                                }
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                if (setListener) {

                                    setMessagesListener("messages/chats/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, findedDialog.decryptedAES);
                                    messageListenersPath.add("messages/chats/" + data.dialogId + "/messages");
                                }

                                cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(findedDialog.decryptedAES));
                                String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);

                                final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, "", data.userIdAuthor, data.time, messageUser.userId, data.avaUrl, data.cryptionType, findedDialog.decryptedAES, findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;


                case "localencrypted":


                    RealmResults<DialogsHolderRealm> res1 = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                    if (!res1.isEmpty()) {


                        DialogsHolderRealm findedDialog = res1.first();

                        if (findedDialog.getCanWrite().equals("")) {


                            if (findedDialog.decryptedAES.equals("readed") & findedDialog.sign.equals("readed")) {

                                firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).removeValue();
                                firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers/" + currentId).removeValue()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {

                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (!data.dialogId.equals(getString(R.string.public_chat))) {

                                                    firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers")
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                    if (!dataSnapshot.hasChildren()) {
                                                                        firebaseDatabase.getReference("messages/chats/" + data.dialogId).removeValue();
                                                                        firebaseDatabase.getReference("messages/chatHolder/" + data.dialogId).removeValue();
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                }
                                                            });
                                                }
                                            }
                                        });

                                firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId).removeValue();

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll().deleteAllFromRealm();
                                    }
                                });

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", data.dialogId).findAll();
                                        res.deleteAllFromRealm();
                                    }
                                });
                                Toast.makeText(RootActivity.this, "Private chat already has been initialized. Unable to recreate it.", Toast.LENGTH_LONG).show();
                            } else {

                                try {
                                    PublicKey publicKey = MyEncryptionProvider.getPublicKey(userAuthor.publicKey);

                                    cipherRSA.init(Cipher.DECRYPT_MODE, myPrivateKey);

                                    byte[] aes_byte = cipherRSA.doFinal(Base64.decode(findedDialog.decryptedAES, Base64.DEFAULT));

                                    signature.initVerify(publicKey);
                                    signature.update(aes_byte);
                                    signature.update(data.userIdAuthor.getBytes(StandardCharsets.UTF_8));


                                    if (signature.verify(Base64.decode(findedDialog.sign, Base64.DEFAULT))) {
                                        if (setListener) {

                                            setMessagesListener("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT));
                                            messageListenersPath.add("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                                        }

                                        cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(aes_byte));
                                        String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                        String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);


                                        final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, "", data.userIdAuthor, data.time, messageUser.userId, data.avaUrl, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT), findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                            }
                                        });


                                        firebaseDatabase.getReference("users").child(currentId).child("dialogHolder").child(data.dialogId)
                                                .setValue(new DialogsHolder(data.type, data.dialogId, data.chatName, data.text, "", data.userIdAuthor, data.time, data.userIdMessage, data.cryptionType, "readed", "readed"));


                                    } else {
                                        firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).removeValue();
                                        firebaseDatabase.getReference("users").child(currentId).child("privateDialogs").child(data.dialogId)
                                                .child("messages").removeValue();
                                        firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers/" + currentId).removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {

                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (!data.dialogId.equals(getString(R.string.public_chat))) {

                                                            firebaseDatabase.getReference("messages/chats/" + data.dialogId + "/chatUsers")
                                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                            if (!dataSnapshot.hasChildren()) {


                                                                                firebaseDatabase.getReference("messages/chatHolder/" + data.dialogId).removeValue();
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });

                                        firebaseDatabase.getReference("users/" + currentId + "/privateDialogs/" + data.dialogId).removeValue();

                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll().deleteAllFromRealm();
                                            }
                                        });

                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                RealmResults<AdvancedMessageRealm> res = realm.where(AdvancedMessageRealm.class).equalTo("dialogId", data.dialogId).findAll();
                                                res.deleteAllFromRealm();
                                            }
                                        });
                                        Toast.makeText(RootActivity.this, "Chat sign is not valid, unable to create chat.", Toast.LENGTH_LONG).show();
                                    }
                                } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            try {
                                if (setListener) {

                                    setMessagesListener("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, findedDialog.decryptedAES);
                                    messageListenersPath.add("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                                }


                                cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(findedDialog.decryptedAES));
                                String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);

                                final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, "", data.userIdAuthor, data.time, messageUser.userId, data.avaUrl, data.cryptionType, findedDialog.decryptedAES, findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });
                            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
        } else {

            final DialogsHolder data = dataSnapshot.getValue(DialogsHolder.class);
            RealmResults<DialogsHolderRealm> ress = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
            final long unreadedMessagesCount = ress.isEmpty() ? 0 : ress.first().unreadedMessagesCount;

            final AddUserRealm authorUser, messageUser, opponentUser;
            RealmResults<AddUserRealm> results = realm.where(AddUserRealm.class).equalTo("userId", data.userIdAuthor).findAll();
            if (results.isEmpty()) {

                authorUser = new AddUserRealm("", "", "", "", 0);
            } else {
                authorUser = results.first();
            }

            RealmResults<AddUserRealm> results1 = realm.where(AddUserRealm.class).equalTo("userId", data.userIdMessage).findAll();
            if (results1.isEmpty()) {


                messageUser = new AddUserRealm("", "", "", "", 0);
            } else {
                messageUser = results1.first();
            }

            RealmResults<AddUserRealm> results2 = realm.where(AddUserRealm.class).equalTo("userId", data.userIdOpponent).findAll();
            if (results2.isEmpty()) {


                opponentUser = new AddUserRealm("", "", "", "", 0);
            } else {
                opponentUser = results2.first();
            }

            switch (data.cryptionType) {
                case "none":

                    final DialogsHolderRealm dialog1 = new DialogsHolderRealm(data.type, data.dialogId, data.chatName, messageUser.userName, data.text, data.userIdOpponent, data.userIdAuthor, data.time, messageUser.userId, opponentUser.avaUrl, data.cryptionType, data.encryptedAES, data.sign, unreadedMessagesCount, messageUser.avaUrl);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(dialog1, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                        }
                    });
                    if (setListener) {

                        setMessagesListener("messages/dialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, "");
                        messageListenersPath.add("messages/dialogs/" + data.dialogId + "/messages");
                    }
                    break;
                case "cloudencrypted":


                    RealmResults<DialogsHolderRealm> res = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                    if (!res.isEmpty()) {

                        DialogsHolderRealm findedDialog = res.first();

                        if (setListener) {

                            setMessagesListener("messages/dialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, findedDialog.decryptedAES);
                            messageListenersPath.add("messages/dialogs/" + data.dialogId + "/messages");
                        }

                        try {

                            cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(findedDialog.decryptedAES));
                            String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                            String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);

                            final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, data.userIdOpponent, data.userIdAuthor, data.time, messageUser.userId, opponentUser.avaUrl, data.cryptionType, findedDialog.decryptedAES, findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            });
                        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    } else {

                        try {
                            PublicKey publicKey = MyEncryptionProvider.getPublicKey(authorUser.publicKey);

                            cipherRSA.init(Cipher.DECRYPT_MODE, myPrivateKey);
                            byte[] aes_byte = cipherRSA.doFinal(Base64.decode(data.encryptedAES, Base64.DEFAULT));

                            signature.initVerify(publicKey);
                            signature.update(aes_byte);
                            signature.update(data.userIdAuthor.getBytes(StandardCharsets.UTF_8));

                            if (signature.verify(Base64.decode(data.sign, Base64.DEFAULT))) {
                                if (setListener) {

                                    setMessagesListener("messages/dialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT));
                                    messageListenersPath.add("messages/dialogs/" + data.dialogId + "/messages");
                                }


                                cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(aes_byte));
                                String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);


                                final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, data.userIdOpponent, data.userIdAuthor, data.time, messageUser.userId, opponentUser.avaUrl, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT), data.sign, unreadedMessagesCount, messageUser.avaUrl);
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });


                            } else {

                                firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                        dialogsHolder.setCanWrite("false");
                                        firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                        dialogsHolder.setCanWrite("false");
                                        firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, opponentUser.userName, messageUser.userName, "Invalid dialog", data.userIdOpponent, data.userIdAuthor, data.time, opponentUser.userId, opponentUser.avaUrl, data.cryptionType, "", data.sign, unreadedMessagesCount, messageUser.avaUrl);
                                        dialog2.setCanWrite("false");
                                        realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                    }
                                });
                                Toast.makeText(RootActivity.this, "Dialog sign is not valid, unable to handle dialog.", Toast.LENGTH_LONG).show();
                            }
                        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                    break;


                case "localencrypted":


                    RealmResults<DialogsHolderRealm> res1 = realm.where(DialogsHolderRealm.class).equalTo("dialogId", data.dialogId).findAll();
                    if (!res1.isEmpty()) {


                        if (res1.first().getDecryptedAES().equals(""))
                            return;


                        DialogsHolderRealm findedDialog = res1.first();


                        if (setListener) {

                            setMessagesListener("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, findedDialog.decryptedAES);
                            messageListenersPath.add("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                        }
                        try {
                            cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(findedDialog.decryptedAES));
                            String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                            String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);

                            final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, data.userIdOpponent, data.userIdAuthor, data.time, messageUser.userId, opponentUser.avaUrl, data.cryptionType, findedDialog.decryptedAES, findedDialog.sign, unreadedMessagesCount, messageUser.avaUrl);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    dialog2.setCanWrite(data.canWrite);
                                    realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            });
                        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }

                    } else {

                        if (data.encryptedAES.equals("readed") & data.sign.equals("readed")) {


                            firebaseDatabase.getReference("users").child(currentId).child("privateDialogs").child(data.dialogId)
                                    .child("messages").removeValue();
                            firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                    dialogsHolder.setCanWrite("false");
                                    firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                    dialogsHolder.setCanWrite("false");
                                    firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, opponentUser.userName, messageUser.userName, "Invalid private dialog", data.userIdOpponent, data.userIdAuthor, data.time, opponentUser.userId, opponentUser.avaUrl, data.cryptionType, "", data.sign, unreadedMessagesCount, messageUser.avaUrl);
                                    dialog2.setCanWrite("false");
                                    realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            });
                            Toast.makeText(RootActivity.this, "Private dialog already has been initialized. Unable to recreate it.", Toast.LENGTH_LONG).show();
                        } else {

                            try {
                                PublicKey publicKey = MyEncryptionProvider.getPublicKey(authorUser.publicKey);

                                cipherRSA.init(Cipher.DECRYPT_MODE, myPrivateKey);
                                byte[] aes_byte = cipherRSA.doFinal(Base64.decode(data.encryptedAES, Base64.DEFAULT));

                                signature.initVerify(publicKey);
                                signature.update(aes_byte);
                                signature.update(data.userIdAuthor.getBytes(StandardCharsets.UTF_8));


                                if (signature.verify(Base64.decode(data.sign, Base64.DEFAULT))) {
                                    if (setListener) {

                                        setMessagesListener("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages", data.dialogId, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT));
                                        messageListenersPath.add("users/" + currentId + "/privateDialogs/" + data.dialogId + "/messages");
                                    }

                                    cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(aes_byte));
                                    String text = new String(cipherAES.doFinal(Base64.decode(data.text, Base64.DEFAULT)), StandardCharsets.UTF_8);
                                    String chatName = new String(cipherAES.doFinal(Base64.decode(data.chatName, Base64.DEFAULT)), StandardCharsets.UTF_8);


                                    final DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, chatName, messageUser.userName, text, data.userIdOpponent, data.userIdAuthor, data.time, messageUser.userId, opponentUser.avaUrl, data.cryptionType, Base64.encodeToString(aes_byte, Base64.DEFAULT), data.sign, unreadedMessagesCount, messageUser.avaUrl);
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            dialog2.setCanWrite(data.canWrite);
                                            realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });


                                    DialogsHolder dialog = new DialogsHolder(data.type, data.dialogId, data.chatName, data.text, data.userIdOpponent, data.userIdAuthor, data.time, data.userIdMessage, data.cryptionType, "readed", "readed");
                                    dialog.setCanWrite(data.canWrite);
                                    firebaseDatabase.getReference("users").child(currentId).child("dialogHolder").child(data.dialogId)
                                            .setValue(dialog);


                                } else {


                                    firebaseDatabase.getReference("users").child(currentId).child("privateDialogs").child(data.dialogId)
                                            .child("messages").removeValue();
                                    firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                            dialogsHolder.setCanWrite("false");
                                            firebaseDatabase.getReference("users/" + data.userIdOpponent + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            DialogsHolder dialogsHolder = dataSnapshot.getValue(DialogsHolder.class);
                                            dialogsHolder.setCanWrite("false");
                                            firebaseDatabase.getReference("users/" + currentId + "/dialogHolder/" + data.dialogId).setValue(dialogsHolder);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            DialogsHolderRealm dialog2 = new DialogsHolderRealm(data.type, data.dialogId, opponentUser.userName, messageUser.userName, "Invalid private dialog", data.userIdOpponent, data.userIdAuthor, data.time, opponentUser.userId, opponentUser.avaUrl, data.cryptionType, "", data.sign, unreadedMessagesCount, messageUser.avaUrl);
                                            dialog2.setCanWrite("false");
                                            realm.copyToRealmOrUpdate(dialog2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });
                                    Toast.makeText(RootActivity.this, "Dialog sign is not valid, unable to create dialog.", Toast.LENGTH_LONG).show();
                                }
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
        }
    }


    private void handleDataSnapshotMessages(@NonNull DataSnapshot dataSnapshot, final String dialogId, @NotNull final String cryptionType, final String decryptedAES) {
        final Message data = dataSnapshot.getValue(Message.class);
        assert data != null;


        final boolean chat = data.type.equals("chat");
        AddUserRealm user;
        RealmResults<AddUserRealm> res = realm.where(AddUserRealm.class).equalTo("userId", data.userId).findAll();
        user = res.first();

        final String id = dataSnapshot.getKey();
        RealmResults<AdvancedMessageRealm> findedMessage = realm.where(AdvancedMessageRealm.class).equalTo("messageId", id).findAll();

        final boolean readed = !findedMessage.isEmpty() ? findedMessage.first().readed : false;

        try {
            switch (cryptionType) {
                case "none":
                    final AdvancedMessageRealm adv = new AdvancedMessageRealm(data.type, user.userName, data.mess, id, data.userId, dialogId, data.time, chat ? user.avaUrl : "", readed);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(adv, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);

                            DialogsHolderRealm findedDialog = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findFirst();
                            if (!readed) {
                                findedDialog.setUnreadedMessagesCount(findedDialog.unreadedMessagesCount + 1);
                                realm.copyToRealmOrUpdate(findedDialog, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                            }
                        }
                    });
                    break;
                case "cloudencrypted":

                    cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(decryptedAES));
                    String mess = new String(cipherAES.doFinal(Base64.decode(data.mess, Base64.DEFAULT)), StandardCharsets.UTF_8);
                    final AdvancedMessageRealm adv1 = new AdvancedMessageRealm(data.type, user.userName, mess, id, data.userId, dialogId, data.time, chat ? user.avaUrl : "", readed);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(adv1, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);

                            DialogsHolderRealm findedDialog = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findFirst();
                            if (!readed) {
                                findedDialog.setUnreadedMessagesCount(findedDialog.unreadedMessagesCount + 1);
                                realm.copyToRealmOrUpdate(findedDialog, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                            }
                        }
                    });
                    break;
                case "localencrypted":
                    PublicKey publicKey = MyEncryptionProvider.getPublicKey(user.publicKey);
                    cipherRSA.init(Cipher.DECRYPT_MODE, myPrivateKey);
                    byte[] aes_byte = cipherRSA.doFinal(Base64.decode(data.encryptedAES, Base64.DEFAULT));

                    cipherAES.init(Cipher.DECRYPT_MODE, MyEncryptionProvider.getSecretKey(aes_byte));
                    byte[] mess_byte = cipherAES.doFinal(Base64.decode(data.mess, Base64.DEFAULT));

                    signature.initVerify(publicKey);
                    signature.update(aes_byte);
                    signature.update(mess_byte);
                    if (signature.verify(Base64.decode(data.sign, Base64.DEFAULT))) {
                        String mess2 = new String(mess_byte, StandardCharsets.UTF_8);

                        final AdvancedMessageRealm adv2 = new AdvancedMessageRealm(data.type, user.userName, mess2, id, data.userId, dialogId, data.time, chat ? user.avaUrl : "", readed);
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealmOrUpdate(adv2, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);

                                DialogsHolderRealm findedDialog = realm.where(DialogsHolderRealm.class).equalTo("dialogId", dialogId).findFirst();
                                if (!readed) {
                                    findedDialog.setUnreadedMessagesCount(findedDialog.unreadedMessagesCount + 1);
                                    realm.copyToRealmOrUpdate(findedDialog, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                }
                            }
                        });
                    } else {
                        Toast.makeText(this, "An error was occurred while adding private message", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        int id = navController.getCurrentDestination().getId();
        switch (id) {
            case R.id.waitFragment:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage("Sign out or exit?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                firebaseAuth.signOut();
                                navController.navigate(R.id.action_waitFragment_to_signInFragment);
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();

            case R.id.dialogSelectFragment:
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Sign out or exit?")
                            .setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    if (dialogHolderEventListener != null) {
                                        usersListRef.keepSynced(false);
                                        dialogHolderRef.keepSynced(false);

                                        dialogHolderRef.removeEventListener(dialogHolderEventListener);
                                        for (int i = 0; i < chatListenersPath.size(); i++) {
                                            firebaseDatabase.getReference(chatListenersPath.get(i)).removeEventListener(chatEventListener);
                                        }
                                    }
                                    if (messagesEventListener != null) {
                                        for (String path : messageListenersPath) {
                                            firebaseDatabase.getReference(path).removeEventListener(messagesEventListener);
                                        }
                                    }
                                    if (dialogUsersEventListener != null) {
                                        for (String path : dialogUsersListenersPath) {
                                            firebaseDatabase.getReference(path).removeEventListener(dialogUsersEventListener);
                                        }
                                    }

                                    if (userEventListener != null)
                                        usersListRef.removeEventListener(userEventListener);
                                    if (dialogsRealm != null)
                                        dialogsRealm.removeAllChangeListeners();
                                    if (usersRealm != null)
                                        usersRealm.removeAllChangeListeners();

                                    Toast.makeText(RootActivity.this, "Bye!", Toast.LENGTH_SHORT).show();
                                    firebaseAuth.signOut();

                                    navController.navigate(R.id.action_dialogSelectFragment_to_authentication);
                                }
                            })
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            }).show();
                }
                break;
            case R.id.signInFragment:
                finish();
                break;


            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (dialogHolderEventListener != null) {
            usersListRef.keepSynced(false);
            dialogHolderRef.keepSynced(false);

            dialogHolderRef.removeEventListener(dialogHolderEventListener);
            for (int i = 0; i < chatListenersPath.size(); i++) {
                firebaseDatabase.getReference(chatListenersPath.get(i)).removeEventListener(chatEventListener);
            }
        }
        if (messagesEventListener != null) {
            for (String path : messageListenersPath) {
                firebaseDatabase.getReference(path).removeEventListener(messagesEventListener);
            }
        }
        if (dialogUsersEventListener != null) {
            for (String path : dialogUsersListenersPath) {
                firebaseDatabase.getReference(path).removeEventListener(dialogUsersEventListener);
            }
        }

        if (userEventListener != null)
            usersListRef.removeEventListener(userEventListener);
        if (dialogsRealm != null)
            dialogsRealm.removeAllChangeListeners();
        if (usersRealm != null)
            usersRealm.removeAllChangeListeners();
        if (realm != null)
            realm.close();
        if (listener != null)
            listener.removeAllConnectionListeners();
        super.onDestroy();
    }
}