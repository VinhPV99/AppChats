package vn.itplus.vinhpv.appchats.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import vn.itplus.vinhpv.appchats.Model.Chat;
import vn.itplus.vinhpv.appchats.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{

    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;

    private Context context;
    private List<Chat>mChat;
    private String imgURL;
    FirebaseUser fuser;


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
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        // get data
        String chat= mChat.get(position).getMessage();
        String timestamp = mChat.get(position).getTimestamp();
        // format time stamp
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        holder.timeTv.setText(dateTime);
        holder.show_mesage.setText(chat);

        try{
            Picasso.get().load(imgURL).into(holder.profile_image);
        }
        catch(Exception e){

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
        holder.show_mesage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
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
                return true;
            }
        });


    }

    private void deleteMessage(int position) {
       String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String msgTimestamp = mChat.get(position).getTimestamp();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = databaseReference.orderByChild("timestamp").equalTo(msgTimestamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()){

                    if(snapshot1.child("sender").getValue().equals(myUID)){
                        //cách xoa msg1
                      snapshot1.getRef().removeValue();

                        //cách xoa msg2
//                        HashMap<String,Object> hashMap = new HashMap<>();
//                        hashMap.put("message"," Tin nhắn đã bị xóa....");
//                       snapshot1.getRef().updateChildren(hashMap);

                    }else {
                        Toast.makeText(context,"Bạn chỉ có thể xóa tin nhắn của mình ...",Toast.LENGTH_SHORT).show();
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

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView profile_image;
        public TextView show_mesage,txt_seen,timeTv;
        LinearLayout messageLayout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            show_mesage =itemView.findViewById(R.id.show_message);
            profile_image= itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen_status);
            timeTv = itemView.findViewById(R.id.timeTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);

        }
    }

}
