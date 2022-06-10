package vn.itplus.vinhpv.appchats.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import vn.itplus.vinhpv.appchats.Model.User;
import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.activity.MessageActivity;
import vn.itplus.vinhpv.appchats.activity.ThereProfileActivity;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyHolder> {
    Context context;
    List<User> userList;
    private boolean isChat;
    private boolean isChatList;

    public UserAdapter(Context context, List<User> userList, boolean isChat, boolean isChatList) {
        this.context = context;
        this.userList = userList;
        this.isChat = isChat;
        this.isChatList = isChatList;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.row_user, viewGroup, false);


        return new UserAdapter.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        // get data
        String userID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        String userEmail = userList.get(position).getEmail();
// set data
        holder.mNameTv.setText(userName);
        holder.mEmailTv.setText(userEmail);
        try {
            Picasso.get().load(userImage).placeholder(R.mipmap.avatar)
                    .into(holder.mAvatarTv);
        } catch (Exception e) {
        }

        if (isChat) {
            if (userList.get(position).getStatus().equals("online")) {
                holder.imageViewON.setVisibility(View.VISIBLE);
                holder.imageViewOFF.setVisibility(View.INVISIBLE);
            } else {
                holder.imageViewOFF.setVisibility(View.VISIBLE);
                holder.imageViewON.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.imageViewON.setVisibility(View.INVISIBLE);
            holder.imageViewOFF.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener((v) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (isChatList){
                Intent intent = new Intent(context, MessageActivity.class);
                intent.putExtra("uid", userID);
                context.startActivity(intent);
            }else{
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            //profile click
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid", userID);
                            context.startActivity(intent);
                        }
                        if (which == 1) {
                            //chat click
                            Intent intent = new Intent(context, MessageActivity.class);
                            intent.putExtra("uid", userID);
                            context.startActivity(intent);
                        }
                    }
                });
            }

            builder.create().show();
        });


    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {

        ImageView mAvatarTv, imageViewON, imageViewOFF;
        TextView mNameTv, mEmailTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            mAvatarTv = itemView.findViewById(R.id.avatarTv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
            imageViewON = itemView.findViewById(R.id.statusimageOn);
            imageViewOFF = itemView.findViewById(R.id.statusimageOff);
        }
    }
}
