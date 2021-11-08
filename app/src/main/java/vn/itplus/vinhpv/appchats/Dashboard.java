package vn.itplus.vinhpv.appchats;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import vn.itplus.vinhpv.appchats.Fragment.ChatList_Fragment;
import vn.itplus.vinhpv.appchats.Fragment.Profile_Fragment;
import vn.itplus.vinhpv.appchats.Fragment.Users_Fragment;
import vn.itplus.vinhpv.appchats.Model.User;

public class Dashboard extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    ActionBar actionBar;

    FirebaseUser firebaseUser;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                User user = datasnapshot.getValue(User.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // set title actionbar
        actionBar = getSupportActionBar();
        actionBar.setTitle("Dashboard");

        firebaseAuth = FirebaseAuth.getInstance();

        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        actionBar.setTitle("Cuộc Trò Chuyện");
        ChatList_Fragment fragment1 = new ChatList_Fragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content,fragment1,"");
        ft1.commit();
    }

    private  BottomNavigationView.OnNavigationItemSelectedListener selectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull  MenuItem item) {
            switch (item.getItemId()){
                case R.id.nav_home:

                    actionBar.setTitle("Cuộc Trò Chuyện");
                    ChatList_Fragment fragment1 = new ChatList_Fragment();
                    FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                    ft1.replace(R.id.content,fragment1,"");
                    ft1.commit();
                    return true;
                case R.id.nav_users:

                    actionBar.setTitle("Người Dùng");
                    Users_Fragment fragment2 = new Users_Fragment();
                    FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                    ft2.replace(R.id.content,fragment2,"");
                    ft2.commit();
                    return true;
                case R.id.nav_profile:

                    actionBar.setTitle("Trang Cá Nhân");
                   Profile_Fragment fragment3 = new Profile_Fragment();
                    FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                    ft3.replace(R.id.content,fragment3,"");
                    ft3.commit();
                    return true;
            }
            return false;
        }
    };

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //mProfileTv.setText(user.getEmail());
        }else {
            startActivity(new Intent(Dashboard.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected  void  onStart(){
        checkUserStatus();
        super.onStart();
    }
        //option Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_menu){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    //check status
    private void CheckStatus(String status){
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap<String ,Object> hashMap = new HashMap<>();
        hashMap.put("status",status);
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