package com.example.chattest.Main;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chattest.BuildConfig;
import com.example.chattest.R;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.fbObjects.AddChatUser;
import com.example.chattest.fbObjects.AddUser;
import com.example.chattest.fbObjects.DialogsHolder;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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

public class StartFragment extends Fragment {
    final private String version = BuildConfig.VERSION_NAME;

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private NavController navController;

    private ImageView img;
    private ProgressBar bar;
    private TextView text;

    private OnAuthEvent onAuthEvent;

    private Realm realm;
    private Context context;

    private boolean flag;
    private MyConnectionListener.ConnectionHandler connectionHandler;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onAuthEvent = (OnAuthEvent) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.start_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        navController = NavHostFragment.findNavController(this);
        img = view.findViewById(R.id.main_image);
        bar = view.findViewById(R.id.progressBar);
        text = view.findViewById(R.id.textView13);
        firebaseDatabase = FirebaseDatabase.getInstance();
        context = view.getContext();

        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    start();
                }
            });
        } else {
            start();
        }
        super.onViewCreated(view, savedInstanceState);
    }

    public void start() {
        flag = true;
        if (RootActivity.listener.isNetworkAvailable()) {
            redirection();
        } else {
            connectionHandler = new MyConnectionListener.ConnectionHandler() {
                @Override
                public void onNetworkAvailableChange(boolean networkAvailable) {
                    if (networkAvailable) {
                        if (!flag)
                            return;

                        flag = false;
                        redirection();
                    }
                }
            };
            RootActivity.listener.addConnectionListener(connectionHandler);
        }
    }

    private void redirection() {
        firebaseDatabase.getReference("version").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.getValue().toString().equals(version)) {

                    img.setImageResource(R.drawable.lisa_ofigevaet);
                    bar.setVisibility(View.GONE);
                    text.setText("Pls update application");

                } else {

                    if (mAuth.getCurrentUser() != null) {
                        if (mAuth.getCurrentUser().isEmailVerified()) {
                            firebaseDatabase.getReference("userslist").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        onAuthEvent.onAuth("start", null);
                                    } else {
                                        completeReg();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        } else {
                            navController.navigate(R.id.action_startFragment_to_authentication);
                        }
                    } else {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.deleteAll();
                            }
                        });
                        navController.navigate(R.id.action_startFragment_to_authentication);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void completeReg() {
        Toast.makeText(context, "Completing registration.", Toast.LENGTH_SHORT).show();

        AddChatUser addChatUser = new AddChatUser(mAuth.getCurrentUser().getUid(), mAuth.getCurrentUser().getDisplayName());
        firebaseDatabase.getReference("messages/chats/" + getString(R.string.public_chat) + "/chatUsers").child(mAuth.getCurrentUser().getUid()).setValue(addChatUser).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    DialogsHolder dialogsHolder = new DialogsHolder("chat", getString(R.string.public_chat), "", "", "", "", "", "none", "", "");
                    firebaseDatabase.getReference("users/" + mAuth.getCurrentUser().getUid() + "/dialogHolder/" + getString(R.string.public_chat)).setValue(dialogsHolder).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                try {
                                    Cipher cipherAES;

                                    RealmResults<KeysRealm> res = realm.where(KeysRealm.class).equalTo("keyName", "password").findAll();
                                    String pass = res.first().getKey();
                                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                                    KeyPair keyPair = keyPairGenerator.generateKeyPair();
                                    final String public64 = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
                                    final String private64 = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);

                                    final SecretKey secretKey = new SecretKeySpec(MyEncryptionProvider.getAesByPassword(pass).getBytes(StandardCharsets.UTF_8), "AES");
                                    cipherAES = Cipher.getInstance("AES");
                                    cipherAES.init(Cipher.ENCRYPT_MODE, secretKey);
                                    final String encryptedPrivate64 = Base64.encodeToString(cipherAES.doFinal(keyPair.getPrivate().getEncoded()), Base64.DEFAULT);

                                    firebaseDatabase.getReference("userslist").child(mAuth.getCurrentUser().getUid())
                                            .setValue(new AddUser(mAuth.getCurrentUser().getUid(), mAuth.getCurrentUser().getDisplayName(), "", public64, encryptedPrivate64, new Date().getTime())).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            realm.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    RealmResults<KeysRealm> res = realm.where(KeysRealm.class).findAll();
                                                    if (!res.isEmpty())
                                                        res.deleteAllFromRealm();

                                                    KeysRealm privateKey = new KeysRealm(private64, mAuth.getCurrentUser().getUid());
                                                    realm.copyToRealmOrUpdate(privateKey, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                }
                                            });

                                            onAuthEvent.onAuth("regstart", null);
                                        }
                                    });
                                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);
        super.onDestroyView();
    }

    public interface OnAuthEvent {
        void onAuth(String type, ProgressDialog dialog);
    }
}
