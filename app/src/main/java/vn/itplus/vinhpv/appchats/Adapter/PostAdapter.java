package vn.itplus.vinhpv.appchats.Adapter;

import static android.view.View.GONE;

import static vn.itplus.vinhpv.appchats.Utils.Constant.*;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import vn.itplus.vinhpv.appchats.Model.Post;
import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.activity.AddPostActivity;
import vn.itplus.vinhpv.appchats.activity.CommentDetailActivity;
import vn.itplus.vinhpv.appchats.activity.ThereProfileActivity;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyHolder> {

    Context context;
    List<Post> postList;
    String myUid;
    ProgressDialog mProgressDialog;
    private DatabaseReference likesRef;
    private DatabaseReference postsRef;

    boolean mProcessLike = false;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child(context.getString(R.string.path_like));
        postsRef = FirebaseDatabase.getInstance().getReference().child(context.getString(R.string.path_posts));
    }

    @androidx.annotation.NonNull
    @Override
    public MyHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);
        mProgressDialog = new ProgressDialog(context);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        // get data
        String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        String pId = postList.get(position).getpId();
        String pTitle = postList.get(position).getpTitle();
        String pDescription = postList.get(position).getpDescr();
        String pImage = postList.get(position).getpImage();
        String pLikes = postList.get(position).getpLikes();
        String pComments = postList.get(position).getpComments();
        String pTimeStamp = postList.get(position).getpTime();
        // convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format(context.getString(R.string.format_date), calendar).toString();

        // set data
        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        if (pTitle == null) {
            holder.pTitleTv.setVisibility(GONE);
        } else {
            holder.pTitleTv.setText(pTitle);
        }
        if (pDescription == null) {
            holder.pDescriptionTv.setVisibility(GONE);
        } else {
            holder.pDescriptionTv.setText(pDescription);
        }
        holder.pLikesTv.setText(" "+pLikes + " Like");
        holder.pCommentsTv.setText(" "+pComments + " Comment");
        setLikes(holder,pId);
        // set user dp
        try {
            Picasso.get().load(uDp).placeholder(R.mipmap.avatar).into(holder.uPictureIv);
        } catch (Exception e) {
        }
        // set post image
        // if there is no image i.e. pImage.equals("no Image")then hide ImageView
        if (pImage.equals(context.getString(R.string.no_image))) {
            // hide imageview
            holder.pImageIv.setVisibility(GONE);
        } else {
            holder.pImageIv.setVisibility(View.VISIBLE);
            try {
                Picasso.get().load(pImage).into(holder.pImageIv);
            } catch (Exception e) {
            }
        }
        // handle button clicks,
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage);
            }
        });

        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int pLikes = Integer.parseInt(postList.get(position).getpLikes());
                mProcessLike = true;
                // get id of the post clicked
                final String postIde = postList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mProcessLike) {
                            if (dataSnapshot.child(postIde).hasChild(myUid)) {
                                // already liked,so remove like
                                postsRef.child(postIde).child(context.getString(R.string.key_plikes)).setValue("" + (pLikes - 1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            }else {
                                // not liked,like it
                                postsRef.child(postIde).child(context.getString(R.string.key_plikes)).setValue(""+(pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue(R.string.liked);// set any value
                                mProcessLike=false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        });

        holder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             Intent intent = new Intent(context, CommentDetailActivity.class);
             intent.putExtra(context.getString(R.string.key_post_id),pId);
             context.startActivity(intent);
            }
        });

        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // will implement later
                BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    shareTextOnly(pTitle,pDescription);
                }else{
                    Bitmap bitmap = (Bitmap) bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pDescription,bitmap);
                }
            }
        });

        holder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra(context.getString(R.string.key_uid), uid);
                context.startActivity(intent);
            }
        });
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        String shareBody = pTitle + "\n"+ pDescription;
//        Uri uid = saveImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,context.getString(R.string.subject_here));
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        intent.setType(TYPE_IMAGE_PNG);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(),context.getString(R.string.images));
        Uri uri = null;
        try{
            imageFolder.mkdir();
            File file = new File(imageFolder,"shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context,AUTHORITY,file);
        }catch(Exception e){

        }
        return uri;
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        String shareBody = pTitle +"\n"+ pDescription;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(TYPE_TEXT_PLAIN);
        intent.putExtra(Intent.EXTRA_SUBJECT,context.getString(R.string.subject_here));
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
    }

    private void setLikes(MyHolder holder,final String pId) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if(snapshot.child(pId).hasChild(myUid)){
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    holder.likeBtn.setText("Like");
                    holder.likeBtn.setTextColor(R.color.color_liked);
                }else{
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like,0,0,0);
                    holder.likeBtn.setText("Like");
                    holder.likeBtn.setTextColor(R.color.color_text_post);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, String pId, String pImage) {

        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);
        if (uid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, R.string.delete);
            popupMenu.getMenu().add(Menu.NONE, 1, 0, R.string.edit);
        }
        popupMenu.getMenu().add(Menu.NONE, 2, 0, R.string.view_detail);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0) {
                    //delete post click
                    deletePost(pId, pImage);
                } else if (id == 1) {
                    //edit post click
                    Intent intent = new Intent(context, AddPostActivity.class);
                    intent.putExtra(context.getString(R.string.key), context.getString(R.string.edit_post));
                    intent.putExtra(context.getString(R.string.edit_post_id), pId);
                    context.startActivity(intent);
                } else if (id == 2) {
                    Intent intent = new Intent(context, CommentDetailActivity.class);
                    intent.putExtra(context.getString(R.string.key_post_id),pId);
                    context.startActivity(intent);
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void deletePost(String pId, String pImage) {
        if (pImage.equals(context.getString(R.string.no_image))) {
            deleteWithoutImage(pId);
        } else {
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithImage(String pId, String pImage) {
        mProgressDialog.setMessage(context.getString(R.string.deleting));

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Query query = FirebaseDatabase.getInstance().getReference(context.getString(R.string.path_posts)).orderByChild(context.getString(R.string.key_pid)).equalTo(pId);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                        }
                        mProgressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Exception e) {

            }
        });
    }

    private void deleteWithoutImage(String pId) {
        Query query = FirebaseDatabase.getInstance().getReference(context.getString(R.string.path_posts)).orderByChild(context.getString(R.string.key_pid)).equalTo(pId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                mProgressDialog.dismiss();
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // view holder class
    class MyHolder extends RecyclerView.ViewHolder {
        // views from row_post.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv,pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            // init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}
