package com.example.chattest.UserCabinet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chattest.Main.RootActivity;
import com.example.chattest.R;
import com.example.chattest.fbObjects.ChatHolder;
import com.example.chattest.tools.MyConnectionListener;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static android.app.Activity.RESULT_OK;

public class UserCabinetFragment extends Fragment {

    private final int PICK_IMAGE = 1;
    private final int PIC_CROP = 2;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private Realm realm;
    private Context context;
    private ImageView img;
    private Uri downloadUrl;
    private NavController navController;
    private ProgressDialog pd;

    private TextView textName, textEmail;
    private LinearLayout groupName, groupEmail, groupPass;

    private MyConnectionListener.ConnectionHandler connectionHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_cabinet_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        context = view.getContext();
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        navController = NavHostFragment.findNavController(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        Button btnrealm = view.findViewById(R.id.button9);
        Button btnDB = view.findViewById(R.id.button10);
        btnDB.setVisibility(firebaseUser.getEmail().equals("voievali@mail.ru") ? View.VISIBLE : View.GONE);
        Button deleteAccount = view.findViewById(R.id.delete_account);
        final Button btnLoadPhoto = view.findViewById(R.id.btnLoadPhoto);
        img = view.findViewById(R.id.userCabinetImage);

        textName = view.findViewById(R.id.textName);
        textEmail = view.findViewById(R.id.textEmail);
        groupName = view.findViewById(R.id.group_name);
        groupEmail = view.findViewById(R.id.group_email);
        groupPass = view.findViewById(R.id.group_pass);

        if (firebaseUser.getPhotoUrl() != null) {
            Picasso.get().load(firebaseUser.getPhotoUrl()).fit().into(img);
        } else {
            Picasso.get().load(R.drawable.user_placeholder).fit().into(img);
        }

        textName.setText(firebaseUser.getDisplayName());
        textEmail.setText(firebaseUser.getEmail());

        groupName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "name");
                navController.navigate(R.id.action_userCabinetFragment_to_changeUserInfoFragment, bundle);
            }
        });
        groupEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "email");
                navController.navigate(R.id.action_userCabinetFragment_to_changeUserInfoFragment, bundle);
            }
        });
        groupPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "pass");
                navController.navigate(R.id.action_userCabinetFragment_to_changeUserInfoFragment, bundle);
            }
        });

        deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Not ready now.", Toast.LENGTH_SHORT).show();
            }
        });
        btnLoadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_IMAGE);
            }
        });

        btnrealm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setCancelable(true)
                        .setTitle("Wipe local DB?")
                        .setMessage(R.string.wipe_alert)
                        .setPositiveButton("Yep", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.deleteAll();
                                    }
                                });
                                firebaseAuth.signOut();
                                getActivity().finish();
                            }
                        })
                        .setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });

        btnDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatHolder chatHolder = new ChatHolder("chat", getString(R.string.public_chat), "Public chat", "", "", "", "", "none");
                firebaseDatabase.getReference("messages/chatHolder").child(getString(R.string.public_chat)).child(getString(R.string.public_chat)).setValue(chatHolder);
            }
        });

        btnLoadPhoto.setEnabled(RootActivity.listener.isNetworkAvailable());
        connectionHandler = new MyConnectionListener.ConnectionHandler() {
            @Override
            public void onNetworkAvailableChange(boolean networkAvailable) {
                btnLoadPhoto.setEnabled(networkAvailable);
            }
        };
        RootActivity.listener.addConnectionListener(connectionHandler);

        progressDialogInit();
        super.onViewCreated(view, savedInstanceState);
    }

    private void progressDialogInit() {
        pd = new ProgressDialog(context);
        pd.setTitle("Uploading...");
        pd.setMessage("Progress:");
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMax(100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Intent intent = CropImage.activity(data.getData())
                            .setAspectRatio(1, 1)
                            .setCropShape(CropImageView.CropShape.OVAL)
                            .getIntent(context);
                    startActivityForResult(intent, PIC_CROP);
                }
                break;
            case PIC_CROP:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    final StorageReference ref = firebaseStorage.getReference("users/" + firebaseUser.getUid() + "/ava");
                    Uri uri = result.getUri();
                    Bitmap bitmap;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                        int[] props = new int[2];
                        int[] newProps;
                        props[0] = bitmap.getWidth();
                        props[1] = bitmap.getHeight();

                        Bitmap newBitmap;
                        StorageMetadata metadata = new StorageMetadata.Builder()
                                .setContentType("image/jpg")
                                .build();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        UploadTask task;
                        if (props[0] > 800 || props[1] > 800) {
                            int maxSideIndex = props[0] >= props[1] ? 0 : 1;
                            double otn = (double) props[maxSideIndex] / (double) props[maxSideIndex == 0 ? 1 : 0];
                            newProps = new int[2];
                            newProps[0] = (int) (maxSideIndex == 0 ? 800 : (double) 800 / otn);
                            newProps[1] = (int) (maxSideIndex == 1 ? 800 : (double) 800 / otn);
                            newBitmap = Bitmap.createScaledBitmap(bitmap, newProps[0], newProps[1], true);
                            bitmap.recycle();

                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
                            newBitmap.recycle();
                            byte[] bytes = baos.toByteArray();

                            task = ref.putBytes(bytes, metadata);
                        } else {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
                            bitmap.recycle();
                            byte[] bytes = baos.toByteArray();

                            task = ref.putBytes(bytes, metadata);
                        }

                        pd.setProgress(0);
                        pd.show();

                        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                pd.setProgress((int) progress);
                            }
                        });

                        Task<Uri> urlTasks = task.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                pd.dismiss();
                                return ref.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    downloadUrl = task.getResult();
                                    Picasso.get().load(downloadUrl).fit().into(img);
                                    UserProfileChangeRequest request = new UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build();
                                    firebaseUser.updateProfile(request);
                                    firebaseDatabase.getReference("userslist/" + firebaseUser.getUid() + "/avaUrl").setValue(downloadUrl.toString());
                                } else {
                                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
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
