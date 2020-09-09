package com.example.chattest.UserCabinet;

import android.app.ProgressDialog;
import android.content.Context;
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

import com.example.chattest.Main.RootActivity;
import com.example.chattest.R;
import com.example.chattest.RealmObjects.KeysRealm;
import com.example.chattest.tools.CustomToolbar;
import com.example.chattest.tools.MyConnectionListener;
import com.example.chattest.tools.MyEncryptionProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ChangeUserInfoFragment extends Fragment {

    private CustomToolbar toolbar;
    private Context context;
    private String type;
    private EditText editName, editEmail, editPassOld, editPassNew;
    private TextView textInfo;

    private FirebaseUser user;
    private FirebaseDatabase firebaseDatabase;

    private Realm realm;

    private Cipher cipherAES;

    private ProgressDialog waiting;

    private MyConnectionListener.ConnectionHandler connectionHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_user_info_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        context = view.getContext();
        toolbar = new CustomToolbar(context);

        editName = view.findViewById(R.id.user_cabinet_input_name);
        editEmail = view.findViewById(R.id.user_cabinet_input_email);
        editPassNew = view.findViewById(R.id.user_cabinet_input_new_pass);
        editPassOld = view.findViewById(R.id.user_cabinet_input_old_pass);
        textInfo = view.findViewById(R.id.user_cabinet_info);

        user = FirebaseAuth.getInstance().getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();

        type = getArguments().getString("type");
        switch (type) {
            case "name":
                toolbar.setTitle("Changing name");
                textInfo.setText(R.string.info_name);
                editEmail.setVisibility(View.GONE);
                editPassOld.setVisibility(View.GONE);
                editPassNew.setVisibility(View.GONE);
                break;
            case "email":
                toolbar.setTitle("Changing email");
                textInfo.setText(R.string.info_email);
                editName.setVisibility(View.GONE);
                editPassNew.setVisibility(View.GONE);
                break;
            case "pass":
                Realm.init(context);
                RealmConfiguration conf = new RealmConfiguration.Builder()
                        .deleteRealmIfMigrationNeeded()
                        .build();
                realm = Realm.getInstance(conf);

                try {
                    cipherAES = Cipher.getInstance("AES");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }

                toolbar.setTitle("Changing password");
                textInfo.setText(R.string.info_pass);
                editName.setVisibility(View.GONE);
                editEmail.setVisibility(View.GONE);
                break;
        }

        final Button btnConfirm = view.findViewById(R.id.button11);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (type) {
                    case "name":
                        if (editName.getText().toString().isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (editName.getText().toString().length() < 2) {
                            Toast.makeText(context, "Name must contains at least 2 characters.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        waiting.show();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(editName.getText().toString())
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            firebaseDatabase.getReference("userslist").child(user.getUid()).child("userName").setValue(editName.getText().toString())
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            waiting.dismiss();
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(context, "Name updated.", Toast.LENGTH_SHORT).show();
                                                            } else {
                                                                Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                    });
                                        } else {
                                            waiting.dismiss();
                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                        break;
                    case "email":
                        if (editPassOld.getText().toString().isEmpty() || editEmail.getText().toString().isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_LONG).show();
                            return;
                        }

                        waiting.show();
                        AuthCredential credential = EmailAuthProvider
                                .getCredential(user.getEmail(), editPassOld.getText().toString());

                        user.reauthenticate(credential)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            user.updateEmail(editEmail.getText().toString())
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            waiting.dismiss();
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(context, "Email updated.", Toast.LENGTH_SHORT).show();
                                                            } else {
                                                                Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        } else {
                                            waiting.dismiss();
                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                        break;
                    case "pass":
                        if (editPassOld.getText().toString().isEmpty() || editPassNew.getText().toString().isEmpty()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        waiting.show();
                        AuthCredential credential1 = EmailAuthProvider
                                .getCredential(user.getEmail(), editPassOld.getText().toString());

                        user.reauthenticate(credential1)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            user.updatePassword(editPassNew.getText().toString())
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                try {
                                                                    cipherAES.init(Cipher.ENCRYPT_MODE, MyEncryptionProvider.getAesKeyByPassword(editPassNew.getText().toString()));
                                                                    PrivateKey privateKey = MyEncryptionProvider.getPrivateKey(realm.where(KeysRealm.class).equalTo("keyName", user.getUid()).findFirst().getKey());

                                                                    final String encryptedPrivate64 = Base64.encodeToString(cipherAES.doFinal(privateKey.getEncoded()), Base64.DEFAULT);

                                                                    firebaseDatabase.getReference("userslist").child(user.getUid()).child("privateKey").setValue(encryptedPrivate64)
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    waiting.dismiss();
                                                                                    if (task.isSuccessful()) {
                                                                                        Toast.makeText(context, "Password updated.", Toast.LENGTH_SHORT).show();
                                                                                        realm.executeTransaction(new Realm.Transaction() {
                                                                                            @Override
                                                                                            public void execute(Realm realm) {
                                                                                                KeysRealm pass = new KeysRealm(editPassNew.getText().toString(), "password");
                                                                                                realm.copyToRealmOrUpdate(pass, ImportFlag.CHECK_SAME_VALUES_BEFORE_SET);
                                                                                            }
                                                                                        });
                                                                                    } else {
                                                                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            } else {
                                                                waiting.dismiss();
                                                                Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        } else {
                                            waiting.dismiss();
                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                        break;
                }
            }
        });
        waitingInit();

        btnConfirm.setEnabled(RootActivity.listener.isNetworkAvailable());
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                btnConfirm.setEnabled(networkAvailable);
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

    @Override
    public void onDestroyView() {
        if (connectionHandler != null)
            RootActivity.listener.removeConnectionListener(connectionHandler);

        super.onDestroyView();
    }
}
