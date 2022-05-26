package vn.itplus.vinhpv.appchats.Fragment;

import android.content.Intent;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import vn.itplus.vinhpv.appchats.Adapter.PostAdapter;
import vn.itplus.vinhpv.appchats.Model.Post;
import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.activity.AddPostActivity;


public class HomeFragment extends Fragment {
    // firebase auth
    FirebaseAuth firebaseAuth;

    RecyclerView mRecyclerView;
    List<Post> mPostList;
    PostAdapter mPostAdapter;
    FloatingActionButton mAddPostButton;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_, container, false);
        mAddPostButton= view.findViewById(R.id.add_post);
        // init
        firebaseAuth = FirebaseAuth.getInstance();

        // recycler view and its properties
        mRecyclerView = view.findViewById(R.id.postRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
// show newest post first,for this load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(layoutManager);
// init post list
        mPostList = new ArrayList<>();
        loadPosts();
        mAddPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AddPostActivity.class));
            }
        });
        return view;
    }

    private void loadPosts() {
        // path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        // get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mPostList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post modelPost = ds.getValue(Post.class);
                    mPostList.add(modelPost);
                    // adapter
                    mPostAdapter = new PostAdapter(getActivity(), mPostList);
                    // set adapter to recyclerview
                    mRecyclerView.setAdapter(mPostAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void seachPost(String search) {
        // path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        // get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mPostList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post modelPost = ds.getValue(Post.class);
                    if (modelPost.getpTitle().toLowerCase().contains(search.toLowerCase()) ||
                            modelPost.getpDescr().toLowerCase().contains(search.toLowerCase())){
                        mPostList.add(modelPost);
                    }

                    // adapter
                    mPostAdapter = new PostAdapter(getActivity(), mPostList);
                    // set adapter to recyclerview
                    mRecyclerView.setAdapter(mPostAdapter);
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

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        // search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!TextUtils.isEmpty(s)) {
                    seachPost(s);
                } else {
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s)) {
                    seachPost(s);
                } else {
                    loadPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }


}