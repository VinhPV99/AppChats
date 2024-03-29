package vn.itplus.vinhpv.appchats.Fragment;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;

import static vn.itplus.vinhpv.appchats.Utils.Constant.*;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vn.itplus.vinhpv.appchats.Adapter.PostAdapter;
import vn.itplus.vinhpv.appchats.Model.Post;
import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.activity.AddPostActivity;
import vn.itplus.vinhpv.appchats.activity.MainActivity;

public class ProfileFragment extends Fragment {

    // arrays of permissions to be requested
    String cameraPermissions[];
    String storagePermissions[];

    List<Post> mPostList;
    PostAdapter mPostAdapter;
    String mUid;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    StorageReference storageReference;
    // path where images of user profile and cover will be stored


    FloatingActionButton mAddPostButton;
    ProgressDialog pd;
    RecyclerView mPostRecyclerView;
    ImageView avatarTv, coverTV;
    TextView nameTv, emailTv, phoneTv;
    ImageButton mEditInfo;
    String profileCover;
    Uri imageUri;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(getString(R.string.path_users));
        storageReference = getInstance().getReference();// firebase storage reference

        // init arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mAddPostButton = view.findViewById(R.id.fab);
        pd = new ProgressDialog(getActivity());
        avatarTv = view.findViewById(R.id.avatarTv);
        coverTV = view.findViewById(R.id.coverTV);
        nameTv = view.findViewById(R.id.nameTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        emailTv = view.findViewById(R.id.emailTv);
        mEditInfo = view.findViewById(R.id.edit_profile);
        mPostRecyclerView = view.findViewById(R.id.postProfile);
        queryData();

        avatarTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.setMessage(getString(R.string.update_avatar));
                profileCover = getString(R.string.key_image);
                showImageDialog();
            }
        });

        nameTv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showEditInfoDialog();
            }
        });
        //edit info
        mEditInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        //fab buttom click
        mAddPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //add post
                startActivity(new Intent(getActivity(), AddPostActivity.class));
            }
        });

        mPostList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();
        return view;
    }

    private void queryData() {
        Query query = databaseReference.orderByChild(getString(R.string.key_email)).equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get data
                    String name = "" + ds.child(getString(R.string.key_name)).getValue();
                    String email = "" + ds.child(getString(R.string.key_email)).getValue();
                    String phone = "" + ds.child(getString(R.string.phone)).getValue();
                    String image = "" + ds.child(getString(R.string.key_image)).getValue();
                    String cover = "" + ds.child(getString(R.string.cover)).getValue();

                    //setData
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        Picasso.get().load(image).into(avatarTv);
                    } catch (Exception e) {
                        Picasso.get().load(R.mipmap.avatar).into(avatarTv);
                    }
                    try {
                        Picasso.get().load(cover).into(coverTV);
                    } catch (Exception e) {

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMyPosts() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        mPostRecyclerView.setLayoutManager(layoutManager);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
        Query query = databaseReference.orderByChild(getString(R.string.key_uid)).equalTo(mUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mPostList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Post modelPost = ds.getValue(Post.class);
                    mPostList.add(modelPost);
                    mPostAdapter = new PostAdapter(getActivity(), mPostList);
                    mPostRecyclerView.setAdapter(mPostAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchMyPosts(String search) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        mPostRecyclerView.setLayoutManager(layoutManager);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
        Query query = databaseReference.orderByChild(getString(R.string.key_uid)).equalTo(mUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Post modelPost = ds.getValue(Post.class);
                    if (modelPost.getpTitle().toLowerCase().contains(search.toLowerCase()) ||
                            modelPost.getpDescr().toLowerCase().contains(search.toLowerCase())) {
                        mPostList.add(modelPost);
                    }

                    mPostAdapter = new PostAdapter(getActivity(), mPostList);
                    mPostRecyclerView.setAdapter(mPostAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showEditInfoDialog() {
        String option[] = { getString(R.string.update_name), getString(R.string.update_phone)};
        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle(getString(R.string.update_info));
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                 if (which == 0) {
                    //edit name
                    pd.setMessage(getString(R.string.update_name));
                    showNamePhoneUpdate(getString(R.string.key_name));
                } else if (which == 1) {
                    //edit phone
                    pd.setMessage(getString(R.string.update_phone));
                    showNamePhoneUpdate(getString(R.string.phone));
                }
            }
        });
        showEdit.create().show();
    }

    private void showEditProfileDialog() {
        String option[] = {getString(R.string.update_avatar), getString(R.string.cover_photo_update), getString(R.string.update_name),getString(R.string.update_phone)};
        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle(getString(R.string.update_info));
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    //edit img
                    pd.setMessage(getString(R.string.update_avatar));
                    profileCover = getString(R.string.key_image);
                    showImageDialog();

                } else if (which == 1) {
                    //edit cover
                    pd.setMessage(getString(R.string.cover_photo_update));
                    profileCover = getString(R.string.cover);
                    showImageDialog();
                } else if (which == 2) {
                    //edit name
                    pd.setMessage(getString(R.string.update_name));
                    showNamePhoneUpdate(getString(R.string.key_name));
                } else if (which == 3) {
                    //edit phone
                    pd.setMessage(getString(R.string.update_phone));
                    showNamePhoneUpdate(getString(R.string.phone));
                }
            }
        });
        showEdit.create().show();
    }

    private void showNamePhoneUpdate(String key) {

        AlertDialog.Builder updateNP = new AlertDialog.Builder(getActivity());
        updateNP.setTitle(getString(R.string.update) + key);

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);
        EditText editText = new EditText(getActivity());
        editText.setHint(getString(R.string.input) + key);
        linearLayout.addView(editText);

        updateNP.setView(linearLayout);
        updateNP.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String value = editText.getText().toString().trim();
                if (!TextUtils.isEmpty(value)) {
                    pd.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(key, value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    pd.dismiss();
                                    Toast.makeText(getActivity(),getString(R.string.update_successful) , Toast
                                            .LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                }
                            });

                    if (key.equals(getString(R.string.key_name))) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
                        Query query = databaseReference.orderByChild(getString(R.string.key_uid)).equalTo(mUid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child(getString(R.string.key_uname)).setValue(value);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // update name in current users comments on posts
                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    String child = ds.getKey();
                                    if (dataSnapshot.child(child).hasChild(getString(R.string.path_comments))) {
                                        String child1 = "" + dataSnapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).child(child1).child(getString(R.string.path_comments)).orderByChild(getString(R.string.key_uid)).equalTo(mUid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for(DataSnapshot ds:dataSnapshot.getChildren()){
                                                    String child=ds.getKey();
                                                    dataSnapshot.getRef().child(child).child(getString(R.string.key_uname)).setValue(value);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.select) + key, Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateNP.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        updateNP.create().show();
    }


    private void showImageDialog() {
        String option[] = {getString(R.string.camera), getString(R.string.library), getString(R.string.cancel)};

        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle(R.string.select_photo_from);
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }

                } else if (which == 1) {
                    // Gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }

                } else if (which == 2) {
                    dialog.dismiss();
                }
            }
        });
        showEdit.create().show();
    }

    private void pickFromCamera() {
        // Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.temp_pic));
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.temp_descr));
        // put image uri
        imageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkCameraPermission() {
        // check if storage permission is enabled or not
        // return true if enabled
        // return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        // request runtime storage permission
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void pickFromGallery() {
        // pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType(TYPE_IMAGE);
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private boolean checkStoragePermission() {
        // check if storage permission is enabled or not
        // return true if enabled
        // return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        // request runtime storage permission
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // picking from camera,first check if camera and storage permissions allowed or not
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        // permissions enabled
                        pickFromCamera();
                    } else {
                        // pemissions denied
                        Toast.makeText(getActivity(), getString(R.string.pemissions_denied_camera_toast), Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                // picking from gallery,first check if  storage permissions allowed or not
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        // permissions enabled
                        pickFromGallery();
                    } else {
                        // pemissions denied
                        Toast.makeText(getActivity(), getString(R.string.pemissions_denied_storage_toast), Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*This method will be called after picking image from Camera or Gallery*/
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // image is picked from gallery,get uri of image
                imageUri = data.getData();
                uploadProfileCoverPhoto(imageUri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // image is picked from camera,get uri of image
                uploadProfileCoverPhoto(imageUri);
            }
        }
    }

    private void uploadProfileCoverPhoto(Uri imageUri) {
        String filePathAndName = FILE_PATH_USER + "" + profileCover + "_" + user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // image is uploaded to storage,now get It's url and store in user's database
                        // image is uploaded to storage,now get it's url and store in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();
                        // check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()) {
                            // image uploaded
                            // add/update url in user's database
                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileCover, downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void avoid) {
                                            // url in database of user is added successfully
                                            // dismiss progress bar

                                            pd.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // error adding url in database of user
                                            // dismiss progress bar
                                            pd.dismiss();
                                        }
                                    });
                            if (profileCover.equals(getString(R.string.key_image))) {
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
                                Query query = databaseReference.orderByChild(getString(R.string.key_uid)).equalTo(mUid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            String child = ds.getKey();
                                            snapshot.getRef().child(child).child(getString(R.string.key_udp)).setValue(downloadUri.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                // update name in current users comments on posts
                                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            String child = ds.getKey();
                                            if (dataSnapshot.child(child).hasChild(getString(R.string.path_comments))) {
                                                String child1 = "" + dataSnapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).child(child1).child(getString(R.string.path_comments)).orderByChild(getString(R.string.key_uid)).equalTo(mUid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                                                            String child=ds.getKey();
                                                            dataSnapshot.getRef().child(child).child(getString(R.string.key_udp)).setValue(downloadUri.toString());
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }
                        } else {
                            // error
                            pd.dismiss();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // there were some error(s),get and show error message,dismiss progress dialog
                        pd.dismiss();
                    }
                });
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            mUid = user.getUid();
        } else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchMyPosts(query);
                } else {
                    loadMyPosts();
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    searchMyPosts(newText);
                } else {
                    loadMyPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_log_out) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}

