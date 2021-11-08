package vn.itplus.vinhpv.appchats.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import vn.itplus.vinhpv.appchats.Model.Chat;
import vn.itplus.vinhpv.appchats.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{
    private Context context;
    private List<Chat>mChat;
    private String imgURL;
    FirebaseUser fuser;
    String myUID;


    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;

    public MessageAdapter(Context context, List<Chat> mChat, String imgURL) {
        this.context = context;
        this.mChat = mChat;
        this.imgURL = imgURL;
    }


    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_right,
                    parent,false);
            return new ViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_left,
                    parent,false);
            return new ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {


        String chat= mChat.get(position).getMessage();
        String timestamp = mChat.get(position).getTimestamp();

        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        holder.timeTv.setText(dateTime);
        holder.show_mesage.setText(chat);

        if("".equals(imgURL)){
            holder.profile_image.setImageResource(R.mipmap.avatar);
        }else {
            Glide.with(context).load(imgURL).into(holder.profile_image);
        }
        if (position==mChat.size() -1){
            if(mChat.get(position).isIsseen()){
                holder.txt_seen.setText("Đã xem");
            }else{
                holder.txt_seen.setText("Đã nhận");
            }
        }
        else {
            holder.txt_seen.setVisibility(View.GONE);
        }

        //Delete
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Xóa, Gỡ bỏ");
                builder.setMessage("Bạn có muốn xóa tin nhắn này ??");
                builder.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);
                    }
                });
                builder.setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });

    }

    private void deleteMessage(int i) {
        myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String msgTimestamp = mChat.get(i).getTimestamp();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = databaseReference.child("timestamp").equalTo(msgTimestamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()){

                    if(snapshot1.child("sender").getValue().equals(myUID)){
                        //cách xoa msg1
                      //snapshot1.getRef().removeValue();

                        //cách xoa msg2
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("message"," Tin nhắn đã bị xóa....");
                       snapshot1.getRef().updateChildren(hashMap);


                        Toast.makeText(context,"Tin nhắn đã được xóa..",Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(context,"Tin nhắn đã bị xóa..",Toast.LENGTH_SHORT).show();
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull  DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView show_mesage;
        public ImageView profile_image;
        public TextView txt_seen,timeTv;

        RelativeLayout messageLayout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            show_mesage =itemView.findViewById(R.id.show_message);
            profile_image= itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen_status);
            timeTv = itemView.findViewById(R.id.timeTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);

        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if(mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        }else {
            return MSG_TYPE_LEFT;
        }
    }
}
