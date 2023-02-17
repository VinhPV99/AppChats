package vn.itplus.vinhpv.appchats.activity;

import static vn.itplus.vinhpv.appchats.Utils.Constant.*;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import vn.itplus.vinhpv.appchats.R;
/** Vinh PV :
 Activity đăng bài viết */
public class AddPostActivity extends AppCompatActivity {
    DatabaseReference mUserDbRef;
    FirebaseAuth mFirebaseAuth;
    ActionBar mActionBar;
     /*Vinh PV: views*/
    EditText mPostTitle, mPostDescription;
    ImageView mPostImage;
    Button mUploadButton;

    /*Vinh PV: arrays of permissions to be requested*/
    String mCameraPermissions[];
    String mStoragePermissions[];
    Uri mImageUri;

    /*Vinh PV: user info*/
    String mUserName, mUserEmail, mUserId, mUserDp;

    //Vinh PV: info of post to be edit
    String mEditPostTitle, mEditPostDescription, mEditPostImage;

    //Vinh PV:  image picked will be samed in this uri
    Uri image_rui = null;

    //Vinh PV:  progress bar
    ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        mFirebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        // init views
        mPostTitle = findViewById(R.id.pTitleEt);
        mPostDescription = findViewById(R.id.pDescriptionEt);
        mPostImage = findViewById(R.id.pImageIv);
        mUploadButton = findViewById(R.id.pUploadBtn);

        mActionBar = getSupportActionBar();
        // enable back button in actionbar
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        //get data
        Intent intent = getIntent();
        String isUpdateKey = "" + intent.getStringExtra(getString(R.string.key));
        String editPostId = "" + intent.getStringExtra(getString(R.string.edit_post_id));
        if (isUpdateKey.equals(getString(R.string.edit_post))) {
            mActionBar.setTitle(R.string.update_post);
            mUploadButton.setText(R.string.update_text);
            loadPostData(editPostId);
        } else {
            mActionBar.setTitle(R.string.add_new_post);
            mUploadButton.setText(R.string.upload_text);
        }


        // init arrays of permissions
        mCameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        mStoragePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mProgressDialog = new ProgressDialog(this);


        mActionBar.setSubtitle(mUserEmail);
        // get some info of current user to include in post
        mUserDbRef = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users));
        Query query = mUserDbRef.orderByChild(getString(R.string.key_email)).equalTo(mUserEmail);
        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    mUserName = "" + ds.child(getString(R.string.key_name)).getValue();
                    mUserEmail = "" + ds.child(getString(R.string.key_email)).getValue();
                    mUserDp = "" + ds.child(getString(R.string.key_image)).getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // get image from camera/gallery on click
        mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show image pick dialog
                showImageDialog();
            }
        });

        // upload button click listener
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get data(title,description)from Edit Texts
                String title = mPostTitle.getText().toString().trim();
                String description = mPostDescription.getText().toString().trim();
                if (isUpdateKey.equals(getString(R.string.edit_post))) {
                    beginUpdate(title, description, editPostId);
                } else {
                    uploadData(title, description);
                }
                onBackPressed();
            }
        });
    }

    /*Vinh PV: Bắt đầu cập nhật */
    private void beginUpdate(String title, String description, String editPostId) {
        mPostImage.setBackground(null);
        mProgressDialog.setMessage(getString(R.string.updating_post));
        mProgressDialog.show();
        if (!mEditPostImage.equals(getString(R.string.no_image))) {
            // without image
            updateWithImage(title, description, editPostId);
        } else if(mPostImage.getDrawable() != null){
            // with image
            updateWithNowImage(title, description, editPostId);
        }else{
            updateNoImage(title, description, editPostId);
        }
    }

    /*Vinh PV: Cập nhật bài viết không có ảnh*/
    private void updateNoImage(String title, String description, String editPostId) {
        // url is recieved,upload to firbease database
        HashMap<String, Object> hashMap = new HashMap<>();
        // put post info
        hashMap.put(getString(R.string.key_uid), mUserId);
        hashMap.put(getString(R.string.key_uname), mUserName);
        hashMap.put(getString(R.string.key_uemail), mUserEmail);
        hashMap.put(getString(R.string.key_udp), mUserDp);
        hashMap.put(getString(R.string.key_plikes),"0");
        hashMap.put(getString(R.string.key_pcomment),"0");
        hashMap.put(getString(R.string.key_ptitle), title);
        hashMap.put(getString(R.string.key_pdescr), description);
        hashMap.put(getString(R.string.key_pimage), getString(R.string.no_image));
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
        ref.child(editPostId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {

                    @Override
                    public void onSuccess(Void unused) {
                        mProgressDialog.dismiss();
                    }
                }).addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {
                mProgressDialog.dismiss();
            }
        });
    }

    /*Vinh PV: Cập nhật với hình ảnh hiện tại*/
    private void updateWithNowImage(String title, String description, String editPostId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = FILE_PATH_POSTS + timestamp;
        // get image from imageview
        Bitmap bitmap = ((BitmapDrawable) mPostImage.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // image uploaded get its url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()) {
                            // url is recieved,upload to firbease database
                            HashMap<String, Object> hashMap = new HashMap<>();
                            // put post info
                            hashMap.put(getString(R.string.key_uid), mUserId);
                            hashMap.put(getString(R.string.key_uname), mUserName);
                            hashMap.put(getString(R.string.key_uemail), mUserEmail);
                            hashMap.put(getString(R.string.key_udp), mUserDp);
                            hashMap.put(getString(R.string.key_plikes),"0");
                            hashMap.put(getString(R.string.key_pcomment),"0");
                            hashMap.put(getString(R.string.key_ptitle), title);
                            hashMap.put(getString(R.string.key_pdescr), description);
                            hashMap.put(getString(R.string.key_pimage), downloadUri);
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
                            ref.child(editPostId)
                                    .updateChildren(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {

                                        @Override
                                        public void onSuccess(Void unused) {
                                            mProgressDialog.dismiss();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {

                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mProgressDialog.dismiss();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    /*Vinh PV: Cập nhật hình ảnh*/
    private void updateWithImage(String title, String description, String editPostId) {
    // post is with image,delete previous image first
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(mEditPostImage);
        mPictureRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void avoid) {
                        // image deleted,upload new image
                        // for post-image name,post-id,publish-time
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = FILE_PATH_POSTS + timestamp;
                        // get image from imageview
                        Bitmap bitmap = ((BitmapDrawable) mPostImage.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // image compress
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] data = baos.toByteArray();

                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putBytes(data)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // image uploaded get its url
                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while (!uriTask.isSuccessful()) ;
                                        String downloadUri = uriTask.getResult().toString();
                                        if (uriTask.isSuccessful()) {
                                            // url is recieved,upload to firbease database
                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            // put post info
                                            hashMap.put(getString(R.string.key_uid), mUserId);
                                            hashMap.put(getString(R.string.key_uname), mUserName);
                                            hashMap.put(getString(R.string.key_uemail), mUserEmail);
                                            hashMap.put(getString(R.string.key_udp), mUserDp);
                                            hashMap.put(getString(R.string.key_plikes),"0");
                                            hashMap.put(getString(R.string.key_pcomment),"0");
                                            hashMap.put(getString(R.string.key_ptitle), title);
                                            hashMap.put(getString(R.string.key_pdescr), description);
                                            hashMap.put(getString(R.string.key_pimage), downloadUri);
                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
                                            ref.child(editPostId)
                                                    .updateChildren(hashMap)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {

                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {

                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    mProgressDialog.dismiss();
                                                }
                                            });
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    /*Vinh PV: Load cập nhật dữ liệu*/
    private void loadPostData(String editPostId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
        Query query = databaseReference.orderByChild(getString(R.string.key_pid)).equalTo(editPostId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // get data
                    mEditPostTitle = "" + ds.child(getString(R.string.key_ptitle)).getValue();
                    mEditPostDescription = "" + ds.child(getString(R.string.key_pdescr)).getValue();
                    mEditPostImage = "" + ds.child(getString(R.string.key_pimage)).getValue();
                    // set data to views
                    mPostImage.setBackground(null);
                    mPostTitle.setText(mEditPostTitle);
                    mPostDescription.setText(mEditPostDescription);
                    // set image
                    if (!mEditPostImage.equals(getString(R.string.no_image))) {
                        try {
                            Picasso.get().load(mEditPostImage).into(mPostImage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /*Vinh PV: tải lên bài viết*/
    private void uploadData(String title, String description) {
        mProgressDialog.setMessage(getString(R.string.publishing_post));
        mProgressDialog.show();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = FILE_PATH_POSTS + timestamp;
        if (mPostImage.getDrawable() != null) {
            // get image from imageview
            Bitmap bitmap = ((BitmapDrawable) mPostImage.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();
            // post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // image is uploaded to firebase storage,now get it's url
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;
                            String downloadUri = uriTask.getResult().toString();
                            if (uriTask.isSuccessful()) {
                                // url is received upload post to firebase database
                                HashMap<Object, String> hashMap = new HashMap<>();
                                // put post info
                                hashMap.put(getString(R.string.key_uid), mUserId);
                                hashMap.put(getString(R.string.key_uname), mUserName);
                                hashMap.put(getString(R.string.key_uemail), mUserEmail);
                                hashMap.put(getString(R.string.key_udp), mUserDp);
                                hashMap.put(getString(R.string.key_plikes),"0");
                                hashMap.put(getString(R.string.key_pcomment),"0");
                                hashMap.put(getString(R.string.key_pid), timestamp);
                                hashMap.put(getString(R.string.key_ptitle), title);
                                hashMap.put(getString(R.string.key_pdescr), description);
                                hashMap.put(getString(R.string.key_pimage), downloadUri);
                                hashMap.put(getString(R.string.key_ptime), timestamp);

                                // path to store post data
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
                                ref.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(AddPostActivity.this, getString(R.string.post_published), Toast.LENGTH_SHORT).show();
                                        // reset views
                                        mPostTitle.setText("");
                                        mPostDescription.setText("");
                                        mPostImage.setImageURI(null);
                                        image_rui = null;
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // failed uploading image
                                        Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // failed uploading image
                            mProgressDialog.dismiss();
                        }
                    });
        } else {
            // post no image
            // url is received upload post to firebase database
            HashMap<Object, String> hashMap = new HashMap<>();
            // put post info
            hashMap.put(getString(R.string.key_uid), mUserId);
            hashMap.put(getString(R.string.key_uname), mUserName);
            hashMap.put(getString(R.string.key_uemail), mUserEmail);
            hashMap.put(getString(R.string.key_udp), mUserDp);
            hashMap.put(getString(R.string.key_plikes),"0");
            hashMap.put(getString(R.string.key_pcomment),"0");
            hashMap.put(getString(R.string.key_pid), timestamp);
            hashMap.put(getString(R.string.key_ptitle), title);
            hashMap.put(getString(R.string.key_pdescr), description);
            hashMap.put(getString(R.string.key_pimage), getString(R.string.no_image));
            hashMap.put(getString(R.string.key_ptime), timestamp);

            // path to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
            ref.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    mProgressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this,getString( R.string.post_published), Toast.LENGTH_SHORT).show();
                    // reset views
                    mPostTitle.setText("");
                    mPostDescription.setText("");
                    mPostImage.setImageURI(null);
                    image_rui = null;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // failed uploading image
                    mProgressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showImageDialog() {
        String option[] = {getString(R.string.camera), getString(R.string.library), getString(R.string.cancel)};

        android.app.AlertDialog.Builder showEdit = new AlertDialog.Builder(this);
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
        mImageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkCameraPermission() {
        // check if storage permission is enabled or not
        // return true if enabled
        // return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        // request runtime storage permission
        requestPermissions(mCameraPermissions, CAMERA_REQUEST_CODE);
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
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        // request runtime storage permission
        requestPermissions(mStoragePermissions, STORAGE_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
                        Toast.makeText(this,getString( R.string.pemissions_denied_camera_toast), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(this, getString(R.string.pemissions_denied_storage_toast), Toast.LENGTH_LONG).show();
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
                image_rui = data.getData();
                // set to imageview
                mPostImage.setBackground(null);
                mPostImage.setImageURI(image_rui);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // image is picked from camera,get uri of image
                mPostImage.setBackground(null);
                mPostImage.setImageURI(image_rui);
            }
        }
    }

    private void checkUserStatus() {
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user != null) {
            mUserEmail = user.getEmail();
            mUserId = user.getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();// goto previous activity
        return super.onSupportNavigateUp();
    }
}