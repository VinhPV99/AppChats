package vn.itplus.vinhpv.appchats.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import vn.itplus.vinhpv.appchats.Fragment.ChatListFragment;
import vn.itplus.vinhpv.appchats.Fragment.HomeFragment;
import vn.itplus.vinhpv.appchats.Fragment.ProfileFragment;
import vn.itplus.vinhpv.appchats.Fragment.UsersFragment;
import vn.itplus.vinhpv.appchats.R;

public class DashboardActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    ActionBar actionBar;

    FirebaseUser firebaseUser;
    DatabaseReference myRef;

    String mUid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        // set title actionbar
        actionBar = getSupportActionBar();
        actionBar.setTitle("Dashboard");

        firebaseAuth = FirebaseAuth.getInstance();

        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        actionBar.setTitle("Trang chủ");
        HomeFragment fragment = new HomeFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content, fragment, "");
        ft.commit();

        checkUserStatus();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_home:

                    actionBar.setTitle("Trang chủ");
                    HomeFragment fragment = new HomeFragment();
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.content, fragment, "");
                    ft.commit();
                    return true;

                case R.id.nav_chats:

                    actionBar.setTitle("Cuộc Trò Chuyện");
                    ChatListFragment fragment1 = new ChatListFragment();
                    FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                    ft1.replace(R.id.content, fragment1, "");
                    ft1.commit();
                    return true;
                case R.id.nav_users:

                    actionBar.setTitle("Người Dùng");
                    UsersFragment fragment2 = new UsersFragment();
                    FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                    ft2.replace(R.id.content, fragment2, "");
                    ft2.commit();
                    return true;
                case R.id.nav_profile:

                    actionBar.setTitle("Trang Cá Nhân");
                    ProfileFragment fragment3 = new ProfileFragment();
                    FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                    ft3.replace(R.id.content, fragment3, "");
                    ft3.commit();
                    return true;
            }
            return false;
        }
    };

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }else{
            mUid = user.getUid();

            SharedPreferences sharedPreferences = getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("Current_USERID",mUid);
            editor.apply();
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }


    //check status
    private void CheckStatus(String status) {
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        myRef.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        CheckStatus("offline");
    }
}