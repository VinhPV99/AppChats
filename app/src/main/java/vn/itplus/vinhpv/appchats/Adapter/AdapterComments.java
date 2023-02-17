package vn.itplus.vinhpv.appchats.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import vn.itplus.vinhpv.appchats.Model.Comment;
import vn.itplus.vinhpv.appchats.R;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.MyHolder> {

    Context context;
    List<Comment> commentList;
    String myUid, postId;

    public AdapterComments(Context context, List<Comment> commentList, String myUid, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = myUid;
        this.postId = postId;
    }

    @Override
    public MyHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        // bind the row_comments.xml layout
        View view = LayoutInflater.from(context).inflate(R.layout.row_comment, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull MyHolder holder, int i) {
        // get the data
        String uid = commentList.get(i).getUid();
        String name = commentList.get(i).getuName();
        String email = commentList.get(i).getuEmail();
        String image = commentList.get(i).getuDp();
        String cid = commentList.get(i).getcId();
        String comment = commentList.get(i).getComment();
        String timestamp = commentList.get(i).getTimestamp();
        // convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTime = DateFormat.format(context.getString(R.string.format_date), calendar).toString();

        // set the data
        holder.nameTv.setText(name);
        holder.commentTv.setText(comment);
        holder.timeTv.setText(pTime);
// set user dp
        try {
            Picasso.get().load(image).placeholder(R.mipmap.avatar).into(holder.avatarIv);
        } catch (Exception e) {
        }
        holder.moreCommentsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if this comment is by currently signed in user or not
                if (myUid.equals(uid)) {
                    // my comment
                    // show delete dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                    builder.setTitle(R.string.delete);
                    builder.setMessage(R.string.ask_to_delete_comment);
                    builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteComment(cid);
                        }
                    });
                    builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                        // no my comment
                    });
                    builder.create().show();
                } else {
                }
            }
        });
    }

    private void deleteComment(String cid) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(context.getString(R.string.path_posts)).child(postId);
        ref.child(context.getString(R.string.path_comments)).child(cid).removeValue();// it will delete the comment
        // now update the comments count
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                String comments = "" + snapshot.child(context.getString(R.string.key_pcomment)).getValue();
                int newCommentVal = Integer.parseInt(comments) - 1;
                ref.child(context.getString(R.string.key_pcomment)).setValue("" + newCommentVal);
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {
        // declare views from row_comments.xml
        ImageView avatarIv;
        TextView nameTv, commentTv, timeTv;
        ImageButton moreCommentsBtn;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            commentTv = itemView.findViewById(R.id.commentTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            moreCommentsBtn = itemView.findViewById(R.id.moreCommentsBtn);
        }
    }
}
