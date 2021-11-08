package vn.itplus.vinhpv.appchats;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Register extends AppCompatActivity {
    EditText txtEmail,txtPass;
    Button btnRegister;
    TextView Tvlogin;

    ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        txtEmail = findViewById(R.id.emailEdittext);
        txtPass = findViewById(R.id.PassEdittext);
        btnRegister = findViewById(R.id.buttonRegister);
        Tvlogin = findViewById(R.id.TvLogin);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Đăng Ký");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        progressDialog =new ProgressDialog(this);
        progressDialog.setMessage(" Đang Đăng Ký...");

        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = txtEmail.getText().toString().trim();
                String password = txtPass.getText().toString().trim();
                //set Email hợp lệ
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    txtEmail.setError("Email bạn nhập không hợp lệ !!!");
                    txtEmail.setFocusable(true);
                }
                //set Pass hợp lệ
                else if(password.length()<6){
                    txtPass.setError("Mật khẩu ít nhất phải 6 kí tự !!!");
                    txtPass.setFocusable(true);
                }
                else {
                    registerUser(email,password);
                }
            }
        });

        Tvlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Register.this,Login.class));
            }
        });
    }

    private void registerUser(String email, String password) {
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();

                            String email = user.getEmail();
                            String uid = user.getUid();

                            HashMap<Object,String>hashMap = new HashMap<>();

                            hashMap.put("email",email);
                            hashMap.put("uid",uid);
                            hashMap.put("name","");
                            hashMap.put("phone","");
                            hashMap.put("image","");
                            hashMap.put("cover","");

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference reference = database.getReference("Users");
                            reference.child(uid).setValue(hashMap);


                            Toast.makeText(Register.this,"Đăng kí thành công"+user
                                    .getEmail(),Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Register.this, Dashboard.class));
                            finish();
                        }else {
                            progressDialog.dismiss();
                            Toast.makeText(Register.this,"Tài khoản đã tồn tại !!",Toast.LENGTH_LONG).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}