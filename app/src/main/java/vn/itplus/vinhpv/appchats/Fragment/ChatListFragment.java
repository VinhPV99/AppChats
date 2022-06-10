package vn.itplus.vinhpv.appchats.Fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import vn.itplus.vinhpv.appchats.Adapter.UserAdapter;
import vn.itplus.vinhpv.appchats.Model.Chatlist;
import vn.itplus.vinhpv.appchats.Model.User;
import vn.itplus.vinhpv.appchats.R;

public class ChatListFragment extends Fragment {
    private UserAdapter userAdapter;
    private List<User> mUser;
    FirebaseUser fuser;
    DatabaseReference reference;
    private List<Chatlist> userList;
    RecyclerView recyclerView;
    FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatlist_, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        userList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("ChatList").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Chatlist chatlist = snapshot1.getValue(Chatlist.class);
                    userList.add(chatlist);
                }
                chatlist();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return view;
    }

    private void chatlist() {
        mUser = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUser.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    for (Chatlist chatlist : userList) {
                        if (user.getUid().equals(chatlist.getId())) {
                            mUser.add(user);
                        }
                    }

                }
                userAdapter = new UserAdapter(getContext(), mUser, true, true);
                recyclerView.setAdapter(userAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchChatlist(String query) {

        mUser = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUser.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    for (Chatlist chatlist : userList) {
                        if (user.getUid().equals(chatlist.getId())) {
                            if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                                    user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                                mUser.add(user);
                            }
                        }

                    }

                }
                userAdapter = new UserAdapter(getContext(), mUser, true,true);
                recyclerView.setAdapter(userAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_log_out).setVisible(false);
        // SearchView
        MenuItem item = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        // search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                // called when user press search button from keyboard
                // if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())) {
                    searchChatlist(s);
                } else {
                    // search text empty,get all users
                    chatlist();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // called whenever user press any single letter
                if (!TextUtils.isEmpty(s.trim())) {
                    searchChatlist(s);
                } else {
                    // search text empty,get all users
                    chatlist();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }
}