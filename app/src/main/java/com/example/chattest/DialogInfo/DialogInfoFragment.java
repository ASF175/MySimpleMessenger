package com.example.chattest.DialogInfo;

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

import com.example.chattest.R;
import com.example.chattest.RealmObjects.AddUserRealm;
import com.example.chattest.RealmObjects.DialogUserRealm;
import com.example.chattest.fbObjects.ItemUserList;
import com.example.chattest.tools.CustomToolbar;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.ArrayList;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;

public class DialogInfoFragment extends Fragment {

    private final int PICK_IMAGE = 1;
    private final int PIC_CROP = 2;
    private DialogInfoAdapter adapter;
    private RecyclerView recycler;
    private Realm realm;
    private Context context;
    private String dialogId;
    private NavController navController;
    private ImageView img;
    private Uri downloadUrl;
    private String authorId, type;
    private FirebaseUser currentUser;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private AlertDialog dialogInfo;
    private ProgressDialog pd;
    private CustomToolbar toolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_info_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();
        toolbar = new CustomToolbar(context);

        recycler = view.findViewById(R.id.recycler_dialog_info);
        navController = NavHostFragment.findNavController(this);
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        firebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();

        dialogId = getArguments().getString("dialogId");
        String dialogName = getArguments().getString("dialogName");
        String dialogUri = getArguments().getString("dialogUri");
        type = getArguments().getString("type");
        authorId = getArguments().getString("authorId");

        toolbar.getEncryptView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogInfo == null) {
                    dialogInfo = createDialog(type);
                }
                dialogInfo.show();
            }
        });

        if (toolbar.getEncryptViewInfo().getVisibility() != View.GONE) {
            switch (type) {
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
        }

        Button changePhoto = view.findViewById(R.id.button_change_photo);
        Button addUser = view.findViewById(R.id.button_add_user);
        TextView userAuthorSplash = view.findViewById(R.id.textView16);
        TextView userAuthor = view.findViewById(R.id.dialog_author_info);

        if (currentUser.getUid().equals(authorId)) {
            userAuthor.setVisibility(View.GONE);
            userAuthorSplash.setVisibility(View.GONE);
            changePhoto.setVisibility(View.VISIBLE);
            changePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, PICK_IMAGE);
                }
            });

            addUser.setVisibility(View.VISIBLE);
            addUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Not ready now.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            userAuthor.setVisibility(View.VISIBLE);
            userAuthorSplash.setVisibility(View.VISIBLE);
            changePhoto.setVisibility(View.GONE);
            addUser.setVisibility(View.GONE);

            if (!authorId.equals("")) {
                String name = realm.where(AddUserRealm.class).equalTo("userId", authorId).findFirst().userName;
                userAuthor.setText(name);
            } else {
                userAuthorSplash.setVisibility(View.GONE);
                userAuthor.setText("Default public chat");
            }
        }

        img = view.findViewById(R.id.dialog_info_image);
        TextView title = view.findViewById(R.id.dialog_info_title);

        if (dialogUri.equals(""))
            Picasso.get().load(R.drawable.chat_plaseholder).fit().into(img);
        else
            Picasso.get().load(dialogUri).fit().into(img);

        title.setText(dialogName);

        progressDialogInit();
        setAdapter();
        setDB();
    }

    private void progressDialogInit() {
        pd = new ProgressDialog(context);
        pd.setIcon(android.R.drawable.ic_dialog_info);
        pd.setTitle("Uploading...");
        pd.setMessage("Progress:");
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMax(100);
    }

    private void setAdapter() {
        recycler.setLayoutManager(new LinearLayoutManager(context));
        adapter = new DialogInfoAdapter(new DialogInfoAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(ItemUserList itemUserList) {
                AddUserRealm usr = realm.where(AddUserRealm.class).equalTo("userId", itemUserList.userId).findFirst();
                Bundle bundle = new Bundle();
                bundle.putString("userId", itemUserList.userId);
                bundle.putString("userName", itemUserList.userName);
                bundle.putString("userUri", itemUserList.avaUrl);
                bundle.putLong("lastOnline", usr.lastOnline);
                navController.navigate(R.id.action_dialogInfoFragment_to_userInfoFragment, bundle);
            }
        });
        recycler.setAdapter(adapter);
    }

    private void setDB() {
        final RealmResults<DialogUserRealm> usersId = realm.where(DialogUserRealm.class).equalTo("dialogId", dialogId).findAll();

        ArrayList<ItemUserList> list = new ArrayList<>();
        for (DialogUserRealm user : usersId) {
            AddUserRealm tmp = (realm.where(AddUserRealm.class).equalTo("userId", user.getUserId()).findFirst());
            list.add(new ItemUserList(tmp.userName, tmp.userId, tmp.avaUrl));
        }
        adapter.updateList(list);

        usersId.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<DialogUserRealm>>() {
            @Override
            public void onChange(RealmResults<DialogUserRealm> dialogUserRealms, OrderedCollectionChangeSet changeSet) {
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        adapter.deleteItem(j);
                    }
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        AddUserRealm usr = realm.where(AddUserRealm.class).equalTo("userId", dialogUserRealms.get(j).getUserId()).findFirst();
                        adapter.addItem(new ItemUserList(usr.userName, usr.userId, usr.avaUrl, false));
                    }
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    for (int j = range.startIndex; j < range.length + range.startIndex; j++) {
                        AddUserRealm usr = realm.where(AddUserRealm.class).equalTo("userId", dialogUserRealms.get(j).getUserId()).findFirst();
                        adapter.updateItem(new ItemUserList(usr.userName, usr.userId, usr.avaUrl, false));
                    }
                }
            }
        });
    }

    private AlertDialog createDialog(String cryptionType) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setMessage("Dialog info");
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
                    final StorageReference ref = firebaseStorage.getReference("dialogs/" + dialogId + "/ava");
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
                                    firebaseDatabase.getReference("messages/chatHolder/").child(dialogId).child(dialogId).child("avaUrl").setValue(downloadUrl.toString());
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
        adapter.clearItems();
        realm.close();
        super.onDestroyView();
    }
}
