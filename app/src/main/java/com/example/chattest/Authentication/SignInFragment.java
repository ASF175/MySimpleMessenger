package com.example.chattest.Authentication;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chattest.Main.RootActivity;
import com.example.chattest.Main.StartFragment;
import com.example.chattest.R;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.fbObjects.AddChatUser;
import com.example.chattest.fbObjects.AddUser;
import com.example.chattest.fbObjects.DialogsHolder;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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

public class SignInFragment extends Fragment {

    NavController navController;
    Context context;
    private Button buttonIn, buttonActReg, buttonForgotPass;
    private EditText pass, mail;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private ProgressDialog waiting;
    private Realm realm;

    private MyConnectionListener.ConnectionHandler connectionHandler;
    private boolean flag;

    private Cipher cipherAES;

    private StartFragment.OnAuthEvent onAuthEvent;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onAuthEvent = (StartFragment.OnAuthEvent) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sign_in_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Realm.init(view.getContext());
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        buttonIn = view.findViewById(R.id.button4);
        buttonActReg = view.findViewById(R.id.button5);
        buttonForgotPass = view.findViewById(R.id.button6);
        pass = view.findViewById(R.id.editText4);
        mail = view.findViewById(R.id.editText5);
        context = view.getContext();

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        navController = NavHostFragment.findNavController(this);

        buttonIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonIn();
            }
        });
        buttonActReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_signInFragment_to_signUpFragment);
            }
        });
        buttonForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonForgotPass();
            }
        });

        waitingInit();
        start();

        super.onViewCreated(view, savedInstanceState);
    }

    public void waitingInit() {
        waiting = new ProgressDialog(context);
        waiting.setCancelable(false);
        waiting.setMessage("Please wait.");
        waiting.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }


    public void start() {
        flag = true;
        buttonIn.setEnabled(RootActivity.listener.isNetworkAvailable());
        buttonForgotPass.setEnabled(RootActivity.listener.isNetworkAvailable());
        if (RootActivity.listener.isNetworkAvailable()) {
            if (mAuth.getCurrentUser() != null) {
                flag = false;
                if (mAuth.getCurrentUser().isEmailVerified()) {
                    firebaseDatabase.getReference("userslist").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                waiting.show();
                                onAuthEvent.onAuth("in", waiting);
                            } else {
                                waiting.setMessage("Completing registration.");
                                waiting.show();
                                completeReg();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    navController.navigate(R.id.action_signInFragment_to_waitFragment);
                }
            }
        }

        if (flag) {
            connectionHandler = new MyConnectionListener.ConnectionHandler() {
                @Override
                public void onNetworkAvailableChange(boolean networkAvailable) {
                    buttonIn.setEnabled(networkAvailable);
                    buttonForgotPass.setEnabled(networkAvailable);

                    if (networkAvailable) {
                        if (mAuth.getCurrentUser() != null) {
                            if (mAuth.getCurrentUser().isEmailVerified()) {
                                firebaseDatabase.getReference("userslist").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.getValue() != null) {
                                            waiting.show();
                                            onAuthEvent.onAuth("in", waiting);
                                        } else {
                                            waiting.setMessage("Completing registration.");
                                            waiting.show();
                                            completeReg();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            } else {
                                navController.navigate(R.id.action_signInFragment_to_waitFragment);
                            }
                        }
                    }
                }
            };
            RootActivity.listener.addConnectionListener(connectionHandler);
        }
    }

    public void setButtonIn() {
        if (mail.getText().toString().isEmpty() | pass.getText().toString().isEmpty()) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
        } else {
            waiting.show();
            if (mAuth.getCurrentUser() != null) {
                Toast.makeText(context, "WUUUUUUUUUUT", Toast.LENGTH_SHORT).show();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        KeysRealm password = new KeysRealm(pass.getText().toString(), "password");
                        realm.copyToRealmOrUpdate(password, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                    }
                });

                onAuthEvent.onAuth("in", waiting);
            } else {
                mAuth.signInWithEmailAndPassword(mail.getText().toString(), pass.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            KeysRealm password = new KeysRealm(pass.getText().toString(), "password");
                                            realm.copyToRealmOrUpdate(password, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                        }
                                    });

                                    if (mAuth.getCurrentUser().isEmailVerified()) {

                                        firebaseDatabase.getReference("userslist").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.getValue() != null) {
                                                    waiting.show();
                                                    onAuthEvent.onAuth("in", waiting);
                                                } else {
                                                    waiting.setMessage("Completing registration.");
                                                    waiting.show();
                                                    completeReg();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                    } else {
                                        waiting.dismiss();

                                        navController.navigate(R.id.action_signInFragment_to_waitFragment);
                                    }
                                } else {
                                    waiting.dismiss();

                                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        }
    }

    public void setButtonForgotPass() {
        if (mail.getText().toString().isEmpty()) {
            mail.setError("Enter email address");
        } else {
            mAuth.sendPasswordResetEmail(mail.getText().toString())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(context, "Email sent", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void completeReg() {
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
                                    cipherAES = Cipher.getInstance("AES");

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

                                            waiting.setMessage("Done, please wait.");
                                            onAuthEvent.onAuth("reg", waiting);
                                        }
                                    });
                                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                waiting.dismiss();
                                waiting.setMessage("Please wait.");

                                Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    waiting.dismiss();
                    waiting.setMessage("Please wait.");

                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        if (realm != null) {
            realm.close();
        }
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);
        super.onDestroyView();
    }
}
