package vn.itplus.vinhpv.appchats.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import vn.itplus.vinhpv.appchats.MessageActivity;
import vn.itplus.vinhpv.appchats.Model.User;
import vn.itplus.vinhpv.appchats.R;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.MyHolder>{
    Context context;
    List<User>userList;
    private boolean isChat;

    public AdapterUser(Context context, List<User> userList, boolean isChat) {
        this.context = context;
        this.userList = userList;
        this.isChat = isChat;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull  ViewGroup viewGroup, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.row_user, viewGroup,false);


        return new AdapterUser.MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull  MyHolder holder, int position) {
        User userName = userList.get(position);
        User userEmail = userList.get(position);

        holder.mNameTv.setText(userName.getName());
        holder.mEmailTv.setText(userEmail.getEmail());
        if(userName.getImage().equals("")){
            holder.mAvatarTv.setImageResource(R.mipmap.avatar);
        }else {
            Glide.with(context).load(userName.getImage())
                    .into(holder.mAvatarTv);
        }

        if(isChat){
            if (userName.getStatus().equals("online")){
                holder.imageViewON.setVisibility(View.VISIBLE);
                holder.imageViewOFF.setVisibility(View.INVISIBLE);
            }else {
                holder.imageViewOFF.setVisibility(View.VISIBLE);
                holder.imageViewON.setVisibility(View.INVISIBLE);
            }
        }else {
            holder.imageViewON.setVisibility(View.INVISIBLE);
            holder.imageViewOFF.setVisibility(View.INVISIBLE);
        }

        holder.mAvatarTv.setOnClickListener((v)->{
            Intent intent = new Intent(context, MessageActivity.class);
            intent.putExtra("uid",userName.getUid());
            context.startActivity(intent);
        });


    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{

        ImageView mAvatarTv,imageViewON,imageViewOFF;
        TextView mNameTv,mEmailTv;

        public MyHolder(@NonNull  View itemView) {
            super(itemView);

            mAvatarTv = itemView.findViewById(R.id.avatarTv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);
            imageViewON = itemView.findViewById(R.id.statusimageOn);
            imageViewOFF = itemView.findViewById(R.id.statusimageOff);
        }
    }
}
