package vn.itplus.vinhpv.appchats.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

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

public class MessageActivity extends AppCompatActivity {
    TextView username, mStatus;
    ImageView imgChat;
    EditText msg_editText;
    ImageButton sendBtn;
    RecyclerView recyclerView;

    FirebaseUser fuser;
    MessageAdapter messageAdapter;
    List<Chat> mChat;
    ValueEventListener seenListener;
//    DatabaseReference mReferenceForSeen;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        recyclerView = findViewById(R.id.recycler_view);
        username = findViewById(R.id.tvChat);
        imgChat = findViewById(R.id.imgChat);
        sendBtn = findViewById(R.id.btn_send);
        msg_editText = findViewById(R.id.text_send);
        mStatus = findViewById(R.id.online_status);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        intent = getIntent();
        hisUid = intent.getStringExtra("uid");

        // firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference("Users");
        fuser = FirebaseAuth.getInstance().getCurrentUser();


        // search user to get that user's info
        Query query = reference.orderByChild("uid").equalTo(hisUid);

        // search user to get that user's info
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // check until required ifo is received
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // get data
                    nameUser = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String onLineStatus = "" + ds.child("onlineStatus").getValue();
                    // set data
                    username.setText(nameUser);
                    if (onLineStatus.equals("online")) {
                        mStatus.setText(onLineStatus);
                    } else {
                        // convert timestamp to proper time date
                        // convert time stamp to dd/mm/yyyy hh:mm am/pm
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(Long.parseLong(onLineStatus));
                        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
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
                    Toast.makeText(MessageActivity.this, "Bạn chưa nhập nội dung...", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(msg);
                }

            }
        });
        SeenMessage();
    }

    private void SeenMessage() {
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chat chat = snapshot1.getValue(Chat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isseen", true);
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
                        Log.d("TAG", "onComplete Token: "+ token);
                        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Tokens")
                                .child(hisUid);

                        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) {
                                    chatRef.child("token").setValue(token);
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
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isseen", false);

        databaseReference.child("Chats").push().setValue(hashMap);
        msg_editText.setText("");

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(fuser.getUid()).child(hisUid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (notify) {
//                    sendNotification(userid, nameUser, message);
                    sendNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void sendNotification(final String hisUid,final String name,final String message) {
        DatabaseReference allToken = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allToken.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid,  message, name, hisUid, R.mipmap.logo1);
                    Sender sender = new Sender(data, token.getToken());

                    try {
                        JSONObject senderJSON = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJSON,
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
                                headers.put("Content-Type","application/json");
                                headers.put("Authorization","key=AAAAPNE6rOY:APA91bE-MGuKxvfCmRhiMda3NBUiHQ9wZJbeV36UcGQKiiuPufWtdoWZuu1uU2_kfZHoSYsW9_HSpIwV-0XLhzw4gq-lmsbbRwg3P3NBMTz8kU7u1dp84ajnq1OTrYB-6iW85TCcKsjw");

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
        reference = FirebaseDatabase.getInstance().getReference("Chats");
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
        reference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        reference.updateChildren(hashMap);
    }

    private void setStatus(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        CheckStatus("online");
        setStatus("online");
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckStatus("online");
        setStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        // get timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        CheckStatus(timestamp);
        setStatus("offline");
    }

}