package com.example.chattest.Authentication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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

public class WaitFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private VerificationWaiting waiting;
    private NavController navController;
    private Context context;
    private String newEmail;
    private TextView text;
    private EditText userInput;
    private Realm realm;

    private Cipher cipherAES;

    private StartFragment.OnAuthEvent onAuthEvent;
    private MyConnectionListener.ConnectionHandler connectionHandler;

    private boolean pause = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onAuthEvent = (StartFragment.OnAuthEvent) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wait_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Button signoutBtn = view.findViewById(R.id.button7);
        final Button resendBtn = view.findViewById(R.id.button8);
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        text = view.findViewById(R.id.textView4);
        context = view.getContext();
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        navController = NavHostFragment.findNavController(this);

        Button emailChange = view.findViewById(R.id.button3);
        emailChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.input_new_email, null);
                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                userInput = promptsView.findViewById(R.id.input_email);
                userInput.setText(mAuth.getCurrentUser().getEmail());
                mDialogBuilder.setView(promptsView)
                        .setCancelable(true)
                        .setPositiveButton("Change email",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        newEmail = userInput.getText().toString();
                                        updateEmail();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                }).show();

            }
        });

        text.setText(String.format("Please confirm your email. We sent an email to\n%s\nIt contains a link to verify your account.", mAuth.getCurrentUser().getEmail()));
        resendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAuth.getCurrentUser() != null) {
                    resendBtn.setClickable(false);
                    mAuth.getCurrentUser().sendEmailVerification()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(context, "Email sent", Toast.LENGTH_SHORT).show();
                                        resendBtn.setClickable(true);
                                    } else {
                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        resendBtn.setClickable(true);
                                    }
                                }
                            });
                } else {
                    Toast.makeText(context, "Error, no current user found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        signoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waiting != null)
                    waiting.cancel(false);

                if (mAuth.getCurrentUser() != null) {
                    mAuth.signOut();
                    navController.navigate(R.id.action_waitFragment_to_signInFragment);
                } else {
                    Toast.makeText(context, "Error, no current user found", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_waitFragment_to_signInFragment);
                }
            }
        });
        waiting = new VerificationWaiting();
        waiting.execute();

        resendBtn.setEnabled(RootActivity.listener.isNetworkAvailable());
        signoutBtn.setEnabled(RootActivity.listener.isNetworkAvailable());
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                resendBtn.setEnabled(networkAvailable);
                signoutBtn.setEnabled(networkAvailable);
            }
        };
        RootActivity.listener.addConnectionListener(connectionHandler);

        super.onViewCreated(view, savedInstanceState);
    }

    private void updateEmail() {
        mAuth.getCurrentUser().updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(context, "Email updated, new verification sent", Toast.LENGTH_SHORT).show();
                            text.setText(String.format("Please confirm your email.\nWe sent an email to %s.\nIt contains a link to verify your account.", newEmail));
                        }
                    });
                } else {
                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        pause = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        pause = false;
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

    private class VerificationWaiting extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while (!mAuth.getCurrentUser().isEmailVerified() || pause) {
                mAuth.getCurrentUser().reload();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
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

                                                onAuthEvent.onAuth("reg", null);
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
                        navController.navigate(R.id.action_waitFragment_to_signInFragment);
                    }
                }
            });
        }
    }
}
