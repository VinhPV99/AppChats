package vn.itplus.vinhpv.appchats;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {
    EditText EmailET,PasswordET;
    Button LoginBtn,RegisterBtn;
    TextView mRecoverPassTv;

    private FirebaseAuth mAuth;

    ProgressDialog pg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        LoginBtn = findViewById(R.id.buttonLogin);
        RegisterBtn = findViewById(R.id.buttonRegisterLG);
        EmailET = findViewById(R.id.userLoginEdittext);
        PasswordET = findViewById(R.id.PassLoginEdittext);
        mRecoverPassTv = findViewById(R.id.recoverPassTv);

        RegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this,Register.class));
                finish();
            }
        });

        LoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = EmailET.getText().toString().trim();
                String pass = PasswordET.getText().toString().trim();
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    EmailET.setError("Email không hợp lệ !!!");
                    EmailET.setFocusable(true);
                }
                else {
                    loginUser(email,pass);
                }
            }
        });
        mRecoverPassTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        pg = new ProgressDialog(this);
    }

    private void showRecoverPasswordDialog() {
        AlertDialog.Builder showRecoverPass = new AlertDialog.Builder(this);
        showRecoverPass.setTitle("Đặt lại mật khẩu");

        LinearLayout linearLayout = new LinearLayout(this);

        final EditText emailEt = new EditText(this);
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setMinEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);
        showRecoverPass.setView(linearLayout);

        showRecoverPass.setPositiveButton("Gửi", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = emailEt.getText().toString().trim();
                setRecoverPass(email);
            }
        });

        showRecoverPass.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        showRecoverPass.create().show();
    }

    private void setRecoverPass(String email) {
        pg.setMessage("Đang Gửi...");
        pg.show();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        pg.dismiss();
                        if(task.isSuccessful()){
                            Toast.makeText(Login.this,"Đã Gửi tới email",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(Login.this,"Gửi thất bại",Toast.LENGTH_SHORT).show();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull  Exception e) {
                pg.dismiss();
            }
        });
    }

    private void loginUser(String email, String pass) {
        pg.setMessage("Đang đăng nhập...");
        pg.show();
        mAuth.signInWithEmailAndPassword(email,pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            pg.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(Login.this,"Đăng nhập thành công",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this, Dashboard.class));
                            finish();
                        }else {
                            pg.dismiss();
                            Toast.makeText(Login.this,"Đăng nhập thất bại!!",Toast.LENGTH_LONG).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pg.dismiss();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}