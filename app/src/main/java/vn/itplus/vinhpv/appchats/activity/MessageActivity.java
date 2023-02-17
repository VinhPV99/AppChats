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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.itplus.vinhpv.appchats.Adapter.MessageAdapter;
import vn.itplus.vinhpv.appchats.Model.Chat;
import vn.itplus.vinhpv.appchats.Model.User;
import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.notifications.Data;
import vn.itplus.vinhpv.appchats.notifications.Sender;
import vn.itplus.vinhpv.appchats.notifications.Token;

/** Activity Nhắn tin*/
public class MessageActivity extends AppCompatActivity {
    TextView username, mStatus;
    ImageView imgChat;
    EditText msg_editText;
    ImageButton sendBtn,attachBtn;
    RecyclerView recyclerView;

    FirebaseUser fuser;
    MessageAdapter messageAdapter;
    List<Chat> mChat;
    ValueEventListener seenListener;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;
    String myUid;
    String hisUid;
    String hisImage;
    Intent intent;

    String nameUser;
    //volley request queue for notification
    private RequestQueue requestQueue;
    private boolean notify = false;

    // arrays of permissions to be requested
    String cameraPermissions[];
    String storagePermissions[];
    Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        recyclerView = findViewById(R.id.recycler_view);
        username = findViewById(R.id.tvChat);
        imgChat = findViewById(R.id.imgChat);
        sendBtn = findViewById(R.id.btn_send);
        attachBtn = findViewById(R.id.attachBtn);
        msg_editText = findViewById(R.id.text_send);
        mStatus = findViewById(R.id.online_status);

        // init arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};;

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        intent = getIntent();
        hisUid = intent.getStringExtra(getString(R.string.key_uid));

        // firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference(getString(R.string.path_users));
        fuser = FirebaseAuth.getInstance().getCurrentUser();


        // search user to get that user's info
        Query query = reference.orderByChild(getString(R.string.key_uid)).equalTo(hisUid);

        // search user to get that user's info
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // check until required ifo is received
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // get data
                    nameUser = "" + ds.child(getString(R.string.key_name)).getValue();
                    hisImage = "" + ds.child(getString(R.string.key_image)).getValue();
                    String onLineStatus = "" + ds.child(getString(R.string.key_online_status)).getValue();
                    // set data
                    username.setText(nameUser);
                    if (onLineStatus.equals(getString(R.string.online))) {
                        mStatus.setText(onLineStatus);
                    } else {
                        // convert timestamp to proper time date
                        // convert time stamp to dd/mm/yyyy hh:mm am/pm
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(Long.parseLong(onLineStatus));
                        String dateTime = DateFormat.format(getString(R.string.format_date), cal).toString();
                        mStatus.setText(dateTime);
                    }
                    try {
                        // image received,set it to imageview in toolbar
                        Picasso.get().load(hisImage).placeholder(R.mipmap.avatar).into(imgChat);
                    } catch (Exception e) {
                        // there is exception getting picture,set default picture
                        Picasso.get().load(R.mipmap.avatar).into(imgChat);
                    }
                }
                readMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                String msg = msg_editText.getText().toString().trim();
                if (TextUtils.isEmpty(msg)) {
                    Toast.makeText(MessageActivity.this, getString(R.string.content_null), Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(msg);
                }

            }
        });
        SeenMessage();

        attachBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // show image pick dialog
                showImageDialog();
            }
        });
    }
    private void showImageDialog() {
        String option[] = {getString(R.string.camera), getString(R.string.library),getString(R.string.cancel)};

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
        imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
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
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        // request runtime storage permission
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
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
                        Toast.makeText(this, getString(R.string.pemissions_denied_camera_toast), Toast.LENGTH_LONG).show();
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
                imageUri = data.getData();
                // set to imageview
                sendImageMessage(imageUri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // image is picked from camera,get uri of image
                sendImageMessage(imageUri);
            }
        }
    }

    private void SeenMessage() {
        reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_chat));
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put(getString(R.string.key_seen), true);
                        snapshot1.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(final String message) {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
//                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();
                        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(getString(R.string.path_tokens))
                                .child(hisUid);

                        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) {
                                    chatRef.child(getString(R.string.key_token)).setValue(token);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(getString(R.string.sender), myUid);
        hashMap.put(getString(R.string.receiver), hisUid);
        hashMap.put(getString(R.string.message), message);
        hashMap.put(getString(R.string.key_timestamp), timestamp);
        hashMap.put(getString(R.string.key_type), getString(R.string.value_text));
        hashMap.put(getString(R.string.key_seen), false);

        databaseReference.child(getString(R.string.path_chat)).push().setValue(hashMap);
        msg_editText.setText("");

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(getString(R.string.path_chat_list))
                .child(fuser.getUid()).child(hisUid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef.child(getString(R.string.key_id_chat)).setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        final DatabaseReference database = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users)).child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (notify) {
                    sendNotification(myUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void sendImageMessage(Uri image_rui) {
        notify = true;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.sending_image));
        progressDialog.show();

        String timestamp = ""+ System.currentTimeMillis();
        String filePathAndName = FILE_PATH_IMAGE+ timestamp;

//        get bitmap from image uri
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image_rui);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            byte[]data = baos.toByteArray(); // convert image to byte
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.dismiss();
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uriTask.isSuccessful());
                    String downloadUri = uriTask.getResult().toString();
                    if(uriTask.isSuccessful()){
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put(getString(R.string.sender), myUid);
                        hashMap.put(getString(R.string.receiver), hisUid);
                        hashMap.put(getString(R.string.message), downloadUri);
                        hashMap.put(getString(R.string.key_timestamp), timestamp);
                        hashMap.put(getString(R.string.key_type), getString(R.string.key_image));
                        hashMap.put(getString(R.string.key_seen), false);

                        databaseReference.child(getString(R.string.path_chat)).push().setValue(hashMap);

                        final DatabaseReference database = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users)).child(myUid);
                        database.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                User user = snapshot.getValue(User.class);
                                if (notify) {
                                    sendNotification(myUid, user.getName(), getString(R.string.sent_you_a_photo));
                                }
                                notify = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(getString(R.string.path_chat_list))
                                .child(fuser.getUid()).child(hisUid);

                        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) {
                                    chatRef.child(getString(R.string.key_id_chat)).setValue(hisUid);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(final String myUid,final String name,final String message) {
        DatabaseReference allToken = FirebaseDatabase.getInstance().getReference(getString(R.string.path_tokens));
        Query query = allToken.orderByKey().equalTo(myUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(hisUid,  message, name, hisUid, R.mipmap.logo1);
                    Sender sender = new Sender(data, token.getToken());

                    try {
                        JSONObject senderJSON = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(API_SENT_NOTIFICATION, senderJSON,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {

                                    }
                                }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<>();
                                headers.put(CONTENT_TYPE,CONTENT_TYPE_VALUE);
                                headers.put(AUTHORIZATION,AUTHORIZATION_VALUE);

                                return headers;
                            }
                        };

                        requestQueue.add(jsonObjectRequest);

                } catch(JSONException e){
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onCancelled (@NonNull DatabaseError error){

        }
    });
}

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void readMessage() {
        mChat = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_chat));
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);

                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                        mChat.add(chat);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, hisImage);
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void CheckStatus(String status) {
        reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users)).child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(getString(R.string.key_online_status), status);
        reference.updateChildren(hashMap);
    }

    private void setStatus(String status) {
        reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users)).child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(getString(R.string.status), status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        CheckStatus(getString(R.string.online));
        setStatus(getString(R.string.online));
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckStatus(getString(R.string.online));
        setStatus(getString(R.string.online));
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        // get timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        CheckStatus(timestamp);
        setStatus(getString(R.string.offline));
    }

}