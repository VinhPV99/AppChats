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
import vn.itplus.vinhpv.appchats.Model.User;
import vn.itplus.vinhpv.appchats.R;

public class UsersFragment extends Fragment {
    RecyclerView recyclerView;
    UserAdapter userAdapter;
    List<User> userList;


    public UsersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_, container, false);

        recyclerView = view.findViewById(R.id.users_recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        userList = new ArrayList<>();

        getAllUser();

        return view;
    }

    private void getAllUser() {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users));
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);

                    if (!user.getUid().equals(firebaseUser.getUid())) {
                        userList.add(user);
                    }
                    userAdapter = new UserAdapter(getContext(), userList, false, false);
                    recyclerView.setAdapter(userAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchGetAllUser(String query) {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_users));
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);

                    assert user != null;
                    if (!user.getUid().equals(firebaseUser.getUid())) {
                        if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                                user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                            userList.add(user);
                        }

                    }
                    userAdapter = new UserAdapter(getContext(), userList, false, false);
                    recyclerView.setAdapter(userAdapter);
                }
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
            public boolean onQueryTextSubmit(String query) {
                // called when user press search button from keyboard
                // if search query is not empty then search
                if (!TextUtils.isEmpty(query.trim())) {
                    searchGetAllUser(query);
                } else {
                    // search text empty,get all users
                    getAllUser();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // called whenever user press any single letter
                if (!TextUtils.isEmpty(query.trim())) {
                    searchGetAllUser(query);
                } else {
                    // search text empty,get all users
                    getAllUser();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }


}