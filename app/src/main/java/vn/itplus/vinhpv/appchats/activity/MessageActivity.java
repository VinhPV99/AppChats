package vn.itplus.vinhpv.appchats.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import vn.itplus.vinhpv.appchats.Adapter.MessageAdapter;
import vn.itplus.vinhpv.appchats.Model.Chat;
import vn.itplus.vinhpv.appchats.R;

public class MessageActivity extends AppCompatActivity {
    TextView username,mStatus;
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
    String userid;
    String hisImage;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        recyclerView = findViewById(R.id.recycler_view);
        username = findViewById(R.id.tvChat);
        imgChat = findViewById(R.id.imgChat);
        sendBtn= findViewById(R.id.btn_send);
        msg_editText = findViewById(R.id.text_send);
        mStatus = findViewById(R.id.online_status);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        intent = getIntent();
        userid= intent.getStringExtra("uid");

        // firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase =FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference("Users");
        fuser = FirebaseAuth.getInstance().getCurrentUser();


        // search user to get that user's info
        Query query = reference.orderByChild("uid").equalTo(userid);
        // search user to get that user's info
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // check until required ifo is received
                for(DataSnapshot ds: snapshot.getChildren()){
                    // get data
                    String name=""+ds.child("name").getValue();
                    hisImage =""+ds.child("image").getValue();
                    String onLineStatus = ""+ds.child("onlineStatus").getValue();
                    // set data
                    username.setText(name);
                    if(onLineStatus.equals("online")){
                        mStatus.setText(onLineStatus);
                    }
                    else{
                        // convert timestamp to proper time date
                        // convert time stamp to dd/mm/yyyy hh:mm am/pm
                        Calendar cal=Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(Long.parseLong(onLineStatus));
                        String dateTime= DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();
                        mStatus.setText(dateTime);
                    }
                    try{
                        // image received,set it to imageview in toolbar
                        Picasso.get().load(hisImage).placeholder(R.mipmap.avatar).into(imgChat);
                    }
                    catch(Exception e){
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

    private void SeenMessage(){
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {
                for(DataSnapshot snapshot1: snapshot.getChildren()){
                    Chat chat = snapshot1.getValue(Chat.class);
                    if(chat.getReceiver().equals(myUid)&& chat.getSender().equals(userid)){
                        HashMap<String,Object> hasSeenHashMap= new HashMap<>();
                        hasSeenHashMap.put("isseen",true);
                        snapshot1.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull  DatabaseError error) {

            }
        });
    }

    private void sendMessage( String message) {

      DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String,Object>hashMap = new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",userid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("isseen",false);

        databaseReference.child("Chats").push().setValue(hashMap);
        msg_editText.setText("");

        final DatabaseReference chatRef =FirebaseDatabase.getInstance().getReference("ChatList")
                .child(fuser.getUid()).child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            myUid = user.getUid();
        }else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void readMessage(){
        mChat= new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {
                mChat.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    Chat chat  =snapshot1.getValue(Chat.class);

                    if(chat.getReceiver().equals(myUid)&& chat.getSender().equals(userid)||
                            chat.getReceiver().equals(userid)&& chat.getSender().equals(myUid)){
                        mChat.add(chat);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this,mChat,hisImage);
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void CheckStatus(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String ,Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus",status);
        reference.updateChildren(hashMap);
    }
    private void setStatus(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String ,Object> hashMap = new HashMap<>();
        hashMap.put("status",status);
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
        String timestamp= String.valueOf(System.currentTimeMillis());
        CheckStatus(timestamp);
        setStatus("offline");
    }

}