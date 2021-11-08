package vn.itplus.vinhpv.appchats.Fragment;
import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import vn.itplus.vinhpv.appchats.R;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;

public class Profile_Fragment extends Fragment {

    private static final int REQUEST_CODE = 101;
    private static final int REQUEST_CODE2 = 102;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;

    FloatingActionButton fab;
    ProgressDialog pd;

    ImageView avatarTv,coverTV;
    TextView nameTv,emailTv,phoneTv;

    String profileCover;
    Uri imageUri;
    private StorageTask upLoadTask;


    public Profile_Fragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser()  ;
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("Users");
        storageReference = FirebaseStorage.getInstance().getReference().child("image");
        storageReference = FirebaseStorage.getInstance().getReference().child("cover");


        fab = view.findViewById(R.id.fab);
        pd= new ProgressDialog(getActivity());
        avatarTv = view.findViewById(R.id.avatarTv);
        coverTV = view.findViewById(R.id.coverTV);
        nameTv = view.findViewById(R.id.nameTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        emailTv = view.findViewById(R.id.emailTv);

        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {

                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name = ""+ds.child("name").getValue();
                    String email = ""+ds.child("email").getValue();
                    String phone = ""+ds.child("phone").getValue();
                    String image = ""+ds.child("image").getValue();
                    String cover = ""+ds.child("cover").getValue();

                    //setData
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        Picasso.get().load(image).into(avatarTv);
                    }catch (Exception e){
                        Picasso.get().load(R.mipmap.avatar).into(avatarTv);
                    }try {
                        Picasso.get().load(cover).into(coverTV);
                    }catch (Exception e){

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull  DatabaseError error) {

            }
        });

        //fab buttom click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        return view;
    }

    private void showEditProfileDialog() {

        String option[] = {"Cập Nhật Ảnh Đại Diện","Cập Nhật Ảnh Bìa","Cập Nhật Tên","Cập Nhật Số Điện Thoại"};

        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle("Thông tin cập nhật");
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                //edit img
                    pd.setMessage("Cập Nhật Ảnh Đại Diện");
                    profileCover= "image";
                    showImageDialog();

                }else if(which == 1){
                //edit cover
                    pd.setMessage("Cập Nhật Ảnh Bìa");
                    profileCover= "cover";
                    showCoverDialog();
                }else if(which == 2){
                //edit name
                    pd.setMessage("Cập Nhật Tên");
                    showNamePhoneUpdate("name");
                }else if(which == 3){
                //edit phone
                    pd.setMessage("Cập Nhật Số Điện Thoại");
                    showNamePhoneUpdate("phone");
                }
            }
        });
        showEdit.create().show();
    }

    private void showNamePhoneUpdate(String key) {

        AlertDialog.Builder updateNP = new AlertDialog.Builder(getActivity());
        updateNP.setTitle("Cập nhật");

        LinearLayout linearLayout  = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        EditText editText = new EditText(getActivity());
        editText.setHint("Nhập...");
        linearLayout.addView(editText);

        updateNP.setView(linearLayout);
        updateNP.setPositiveButton("Cập nhật", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String value = editText.getText().toString().trim();
                if(!TextUtils.isEmpty(value)){
                    pd.show();

                    HashMap<String ,Object> result = new HashMap<>();
                    result.put(key,value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    pd.dismiss();
                                    Toast.makeText(getActivity(),"Cập nhật thành công !!!",Toast
                                            .LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                }
                            });
                }
                else {
                    Toast.makeText(getActivity(),"Chọn"+key,Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateNP.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        updateNP.create().show();
    }

    private void showImageDialog() {
        String option[] = {"Thư Viện","Hủy"};

        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle("Chọn Ảnh Từ");
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent,REQUEST_CODE);

                }else if(which==1){ }
            }
        });
        showEdit.create().show();
    }

    private void showCoverDialog() {
        String option[] = {"Thư Viện","Hủy"};

        AlertDialog.Builder showEdit = new AlertDialog.Builder(getActivity());
        showEdit.setTitle("Chọn Ảnh Từ");
        showEdit.setItems(option, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent,REQUEST_CODE2);

                }else if(which==1){
                }
            }
        });
        showEdit.create().show();
    }

    private String getFileExtention(Uri uri){
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));

    }

    private void UpLoadMyImage() {
        final ProgressDialog progressDialog= new ProgressDialog(getContext());
        progressDialog.setMessage("Đang tải...");
        progressDialog.show();
        if (imageUri != null){

            final  StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +","+ getFileExtention(imageUri));

            upLoadTask = fileReference.putFile(imageUri);
            upLoadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot,Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        String mUri =downloadUri.toString();
                        databaseReference= FirebaseDatabase.getInstance().getReference("Users")
                                .child(user.getUid());

                        HashMap<String ,Object> map =new HashMap<>();
                        map.put("image",mUri);
                        databaseReference.updateChildren(map);
                        progressDialog.dismiss();
                    }else {
                        Toast.makeText(getContext(),"Lỗi!!",Toast.LENGTH_LONG).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });
        }else {
            Toast.makeText(getContext(),"Bạn chưa chọn ảnh",Toast.LENGTH_LONG).show();
        }
    }
    private void UpLoadMyCover() {
        final ProgressDialog progressDialog= new ProgressDialog(getContext());
        progressDialog.setMessage("Đang tải...");
        progressDialog.show();
        if (imageUri != null){

            final  StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +","+ getFileExtention(imageUri));

            upLoadTask = fileReference.putFile(imageUri);
            upLoadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot,Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        String mUri =downloadUri.toString();
                        databaseReference= FirebaseDatabase.getInstance().getReference("Users")
                                .child(user.getUid());

                        HashMap<String ,Object> map =new HashMap<>();
                        map.put("cover",mUri);
                        databaseReference.updateChildren(map);
                        progressDialog.dismiss();
                    }else {
                        Toast.makeText(getContext(),"Lỗi!!",Toast.LENGTH_LONG).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });
        }else {
            Toast.makeText(getContext(),"Bạn chưa chọn ảnh",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            imageUri = data.getData();
            if (upLoadTask != null && upLoadTask.isInProgress()) {
                Toast.makeText(getContext(), "Đang tải ảnh", Toast.LENGTH_LONG).show();
            } else {
                UpLoadMyImage();
            }
        }
        if (requestCode == REQUEST_CODE2 && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            imageUri = data.getData();
            if (upLoadTask != null && upLoadTask.isInProgress()) {
                Toast.makeText(getContext(), "Đang tải ảnh", Toast.LENGTH_LONG).show();
            } else {
                UpLoadMyCover();
            }
        }
    }
}