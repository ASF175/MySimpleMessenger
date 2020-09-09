package com.example.chattest.Authentication;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

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

public class SignUpFragment extends Fragment {

    private EditText mail, pass, log, confpass;
    private Button buttonReg;
    private FirebaseDatabase firebaseDatabase;
    private Context context;
    private NavController navController;
    private FirebaseAuth mAuth;
    private Realm realm;
    private ProgressDialog waiting;

    private Cipher cipherAES;

    private StartFragment.OnAuthEvent onAuthEvent;
    private MyConnectionListener.ConnectionHandler connectionHandler;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onAuthEvent = (StartFragment.OnAuthEvent) context;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sign_up_fragment, container, false);
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

        mail = view.findViewById(R.id.editText6);
        log = view.findViewById(R.id.editText7);
        pass = view.findViewById(R.id.editText8);
        confpass = view.findViewById(R.id.editText2);
        buttonReg = view.findViewById(R.id.button4);
        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonReg();
            }
        });

        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        context = view.getContext();
        navController = NavHostFragment.findNavController(this);

        confpass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!pass.getText().toString().equals(confpass.getText().toString())) {
                    confpass.setError("Passwords don`t match");
                }
            }
        });
        waitingInit();

        buttonReg.setEnabled(RootActivity.listener.isNetworkAvailable());
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                buttonReg.setEnabled(networkAvailable);
            }
        };
        RootActivity.listener.addConnectionListener(connectionHandler);

        super.onViewCreated(view, savedInstanceState);
    }

    public void waitingInit() {
        waiting = new ProgressDialog(context);
        waiting.setCancelable(false);
        waiting.setMessage("Please wait.");
        waiting.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    public void setButtonReg() {
        mAuth = FirebaseAuth.getInstance();
        if (mail.getText().toString().isEmpty() | log.getText().toString().isEmpty() | pass.getText().toString().isEmpty()) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
        } else {
            if (log.getText().toString().length() < 2) {
                Toast.makeText(context, "Name must contains at least 2 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            waiting.show();
            mAuth.createUserWithEmailAndPassword(mail.getText().toString(), pass.getText().toString())
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(log.getText().toString())
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                mAuth.getCurrentUser().sendEmailVerification();

                                                if (mAuth.getCurrentUser().isEmailVerified()) {
                                                    AddChatUser addChatUser = new AddChatUser(mAuth.getCurrentUser().getUid(), mAuth.getCurrentUser().getDisplayName());
                                                    firebaseDatabase.getReference("messages/chats/" + getString(R.string.public_chat) + "/chatUsers").child(mAuth.getCurrentUser().getUid()).setValue(addChatUser);

                                                    DialogsHolder dialogsHolder = new DialogsHolder("chat", getString(R.string.public_chat), "", "", "", "", "", "none", "", "");
                                                    firebaseDatabase.getReference("users/" + mAuth.getCurrentUser().getUid() + "/dialogHolder/" + getString(R.string.public_chat)).setValue(dialogsHolder);

                                                    try {
                                                        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                                                        KeyPair keyPair = keyPairGenerator.generateKeyPair();
                                                        final String public64 = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
                                                        final String private64 = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);

                                                        final SecretKey secretKey = new SecretKeySpec(MyEncryptionProvider.getAesByPassword(pass.getText().toString()).getBytes(StandardCharsets.UTF_8), "AES");
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

                                                                        KeysRealm password = new KeysRealm(pass.getText().toString(), "password");
                                                                        realm.copyToRealmOrUpdate(password, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                                    }
                                                                });

                                                                onAuthEvent.onAuth("reg", waiting);
                                                            }
                                                        });
                                                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    waiting.dismiss();
                                                    realm.executeTransaction(new Realm.Transaction() {
                                                        @Override
                                                        public void execute(Realm realm) {
                                                            KeysRealm password = new KeysRealm(pass.getText().toString(), "password");
                                                            realm.copyToRealmOrUpdate(password, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                        }
                                                    });

                                                    navController.navigate(R.id.action_signUpFragment_to_waitFragment);
                                                }
                                            }
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        if (realm != null)
            realm.close();
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);
        super.onDestroyView();
    }
}
