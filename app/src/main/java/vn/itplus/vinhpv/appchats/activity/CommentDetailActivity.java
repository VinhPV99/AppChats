package vn.itplus.vinhpv.appchats.activity;

import static android.view.View.GONE;

import static vn.itplus.vinhpv.appchats.Utils.Constant.*;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import vn.itplus.vinhpv.appchats.Adapter.AdapterComments;
import vn.itplus.vinhpv.appchats.Model.Comment;
import vn.itplus.vinhpv.appchats.R;

/** Vinh PV: Activity comment*/
public class CommentDetailActivity extends AppCompatActivity {

    // views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTiv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<Comment> commentList;
    AdapterComments adapterComments;

    // add comments views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;


    // to get detail of user and post
    String myUid, myEmail, myName, myDp,
            postId, pLikes, hisDp, hisName, hisUid, pImage;

    //Progress bar
    ProgressDialog progressDialog;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_detail);
        // Actionbar and its properties
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.post_detail);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // init views
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTiv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);
        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);


        // get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra(getString(R.string.key_post_id));

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();
        loadComments();
        // set subtitle of actionbar
        actionBar.setSubtitle(myName);
        // send comment button click
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });
        likeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                likePost();
            }
        });
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });
        shareBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String pTitle = pTitleTv.getText().toString().trim();
                String pDescription = pDescriptionTv.getText().toString().trim();
                // will implement later
                BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    shareTextOnly(pTitle,pDescription);
                }else{
                    Bitmap bitmap = (Bitmap) bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pDescription,bitmap);
                }
            }
        });
    }

    /* Vinh PV: Chia sẻ bài viết */
    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        String shareBody = pTitle + "\n"+ pDescription;
        Uri uid = saveImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.subject_here));
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        intent.setDataAndType(uid,TYPE_IMAGE_PNG);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    /*Vinh PV: lưu lại ảnh để chia sẻ*/
    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(),getString(R.string.images));
        Uri uri = null;
        try{
            imageFolder.mkdir();
            File file = new File(imageFolder,"shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,AUTHORITY,file);
        }catch(Exception e){

        }
        return uri;
    }

    /*Vinh PV: Chỉ chia sẻ text*/
    private void shareTextOnly(String pTitle, String pDescription) {
        String shareBody = pTitle +"\n"+ pDescription;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(TYPE_TEXT_PLAIN);
        intent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.subject_here));
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    /*Vinh PV: Load Comment*/
    private void loadComments() {
        // layout(Linear)for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        // set layout to recyclerview
        recyclerView.setLayoutManager(layoutManager);
        // init comments list
        commentList = new ArrayList<>();
        // path of the post,to get it's comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).child(postId).child(getString(R.string.path_comments));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Comment modelComment = ds.getValue(Comment.class);
                    commentList.add(modelComment);
                    // setup adapter
                    adapterComments = new AdapterComments(getApplicationContext(), commentList,myUid,postId);
                    // set adapter
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    /*Vinh PV: Show option menu*/
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showMoreOptions() {

        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);
        if (hisUid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, R.string.delete);
            popupMenu.getMenu().add(Menu.NONE, 1, 0, R.string.edit);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0) {
                    //delete post click
                    deletePost();
                } else if (id == 1) {
                    //edit post click
                    Intent intent = new Intent(CommentDetailActivity.this, AddPostActivity.class);
                    intent.putExtra(getString(R.string.key), getString(R.string.edit_post));
                    intent.putExtra(getString(R.string.edit_post_id), postId);
                    startActivity(intent);
                }

                return false;
            }
        });
        popupMenu.show();
    }

    /*Vinh PV: Xóa bài viết*/
    private void deletePost() {
        if (pImage.equals(getString(R.string.no_image))) {
            deleteWithoutImage();
        } else {
            deleteWithImage();
        }
    }
    /*Vinh PV: Xóa bài viết không có ảnh*/
    private void deleteWithoutImage() {
        Query query = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).orderByChild(getString(R.string.key_pid)).equalTo(postId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    /*Vinh PV: Xóa bài viết có ảnh*/
    private void deleteWithImage() {
        progressDialog.setMessage(getString(R.string.deleting));

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Query query = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).orderByChild(getString(R.string.key_pid)).equalTo(postId);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                        }
                        progressDialog.dismiss();
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

    /*Vinh PV: Xử lý hiển thị số like*/
    private void setLikes() {
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.path_like));
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)) {
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    likeBtn.setText("Like");
                    likeBtn.setTextColor(R.color.color_liked);
                } else {
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
                    likeBtn.setText("Like");
                    likeBtn.setTextColor(R.color.color_text_post);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    /*Vinh PV: Xử lý click like*/
    private void likePost() {
        mProcessLike = true;
        // get id of the post clicked
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.path_like));
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.path_posts));
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        // already liked,so remove like
                        postsRef.child(postId).child(getString(R.string.key_plikes)).setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;

                    } else {
                        // not liked,like it
                        postsRef.child(postId).child(getString(R.string.key_plikes)).setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue(getString(R.string.liked));// set any value
                        mProcessLike = false;

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    /*Vinh PV: comment bài viết*/
    private void postComment() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.adding_comment));
        // get data from comment edit text
        String comment = commentEt.getText().toString().trim();
        // validate
        if (TextUtils.isEmpty(comment)) {
            // no value is entered
        //  Toast.makeText(context:this,text:"Comment is empty ...",Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).child(postId).child(getString(R.string.path_comments));

        HashMap<String, Object> hashMap = new HashMap<>();
        // put info in hashmap
        hashMap.put(getString(R.string.key_cid), timestamp);
        hashMap.put(getString(R.string.key_comment), comment);
        hashMap.put(getString(R.string.key_timestamp), timestamp);
        hashMap.put(getString(R.string.key_uid), myUid);
        hashMap.put(getString(R.string.key_uemail), myEmail);
        hashMap.put(getString(R.string.key_udp), myDp);
        hashMap.put(getString(R.string.key_uname), myName);
        // put this data in db
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void avoid) {
                        progressDialog.dismiss();
                        commentEt.setText("");
                        updateCommentCount();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {

                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        progressDialog.dismiss();
                    }
                });
    }

    private void updateCommentCount() {
        mProcessComment = true;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts)).child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (mProcessComment) {
                    String comments = "" + snapshot.child(getString(R.string.key_pcomment)).getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child(getString(R.string.key_pcomment)).setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserInfo() {
        // get current user info
        Query myRef = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users));
        myRef.orderByChild(getString(R.string.key_uid)).equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myName = "" + ds.child(getString(R.string.key_name)).getValue();
                    myDp = "" + ds.child(getString(R.string.key_image)).getValue();
                    // set data
                    try {
                        //if image is received then set
                        Picasso.get().load(myDp).placeholder(R.mipmap.avatar).into(cAvatarIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.mipmap.avatar).into(cAvatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loadPostInfo() {
        // get post using the id of the post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.path_posts));
        Query query = ref.orderByChild(getString(R.string.key_pid)).equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // keep checking the posts until get the required post
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get data
                    String pTitle = "" + ds.child(getString(R.string.key_ptitle)).getValue();
                    String pDescr = "" + ds.child(getString(R.string.key_pdescr)).getValue();
                    pLikes = "" + ds.child(getString(R.string.key_plikes)).getValue();
                    String pTimeStamp = "" + ds.child(getString(R.string.key_ptime)).getValue();
                    pImage = "" + ds.child(getString(R.string.key_pimage)).getValue();
                    hisDp = "" + ds.child(getString(R.string.key_udp)).getValue();
                    hisUid = "" + ds.child(getString(R.string.key_uid)).getValue();
                    String uEmail = "" + ds.child(getString(R.string.key_uemail)).getValue();
                    hisName = "" + ds.child(getString(R.string.key_uname)).getValue();
                    String commentCount = "" + ds.child(getString(R.string.key_pcomment)).getValue();
                    // convert timestamp to dd/mm/yyyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));

                    String pTime = DateFormat.format(getString(R.string.format_date), calendar).toString();
                    // set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes + getString(R.string.path_like));
                    pTimeTiv.setText(pTime);
                    pCommentsTv.setText(commentCount + getString(R.string.path_comments));
                    uNameTv.setText(hisName);
                    if (pImage.equals(getString(R.string.no_image))) {
                        // hide imageview
                        pImageIv.setVisibility(GONE);
                    } else {
                        pImageIv.setVisibility(View.VISIBLE);
                        try {
                            Picasso.get().load(pImage).into(pImageIv);
                        } catch (Exception e) {
                        }
                    }

                    // set user image in comment part
                    try {
                        Picasso.get().load(hisDp).placeholder(R.mipmap.avatar).into(uPictureIv);
                    } catch (Exception e) {
                        Picasso.get().load(R.mipmap.avatar).into(uPictureIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();
        } else {
            // user not signed in,go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}