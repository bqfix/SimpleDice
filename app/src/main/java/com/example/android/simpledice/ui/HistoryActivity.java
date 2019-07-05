package com.example.android.simpledice.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.android.simpledice.utils.Constants;
import com.example.android.simpledice.utils.DiceResults;
import com.example.android.simpledice.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryResultsAdapter.HistoryResultsClickHandler {

    private TextView mResultsNameTextView;
    private TextView mResultsTotalTextView;
    private TextView mResultsDescripTextView;
    private RecyclerView mHistoryRecyclerView;
    private HistoryResultsAdapter mHistoryAdapter;
    private AdView mAdView;

    private List<DiceResults> mDiceResults;

    //Firebase
    private String mUserID;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mBaseDatabaseReference;
    private ChildEventListener mHistoryChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setTitle(R.string.history_activity_title);

        initializeFirebase();

        assignViews();

        setupAds();

        setupHistoryRecyclerView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseHistoryReadListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_activity_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_sign_out):
                AlertDialog.Builder signoutBuilder = new AlertDialog.Builder(HistoryActivity.this);
                signoutBuilder.setTitle(R.string.sign_out_dialog_title)
                        .setMessage(R.string.sign_out_dialog_message)
                        .setPositiveButton(R.string.sign_out_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AuthUI.getInstance().signOut(HistoryActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.sign_out_dialog_negative, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                signoutBuilder.show();
                return true;
            case (R.id.action_delete_all_history):
                AlertDialog.Builder deleteHistoryBuilder = new AlertDialog.Builder(HistoryActivity.this);
                deleteHistoryBuilder.setTitle(R.string.delete_all_dialog_title)
                        .setMessage(R.string.delete_all_dialog_message)
                        .setPositiveButton(R.string.delete_all_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearAllHistory();
                            }
                        })
                        .setNegativeButton(R.string.delete_all_dialog_negative, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                deleteHistoryBuilder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(DiceResults historyResults) {
        setDataToResultsViews(historyResults);
    }

    /**
     * A helper method that assigns all of the views to their initial values in onCreate
     */
    private void assignViews() {
        mResultsNameTextView = findViewById(R.id.results_name_tv);
        mResultsTotalTextView = findViewById(R.id.results_total_tv);
        mResultsDescripTextView = findViewById(R.id.results_descrip_tv);
        mHistoryRecyclerView = findViewById(R.id.history_rv);
        mAdView = findViewById(R.id.banner_ad);
    }

    /**
     * A helper method to populate the results views with data
     *
     * @param diceResults to populate the views from
     */
    private void setDataToResultsViews(DiceResults diceResults) {
        mResultsNameTextView.setText(diceResults.getName());
        mResultsTotalTextView.setText(String.valueOf(diceResults.getTotal()));
        mResultsDescripTextView.setText(diceResults.getDescrip());
    }

    /**
     * Helper method to setup HistoryRecyclerView, should only be called once in onCreate
     */
    private void setupHistoryRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mHistoryRecyclerView.setLayoutManager(layoutManager);

        mHistoryAdapter = new HistoryResultsAdapter(this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        mHistoryRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (mDiceResults == null) {
            mDiceResults = new ArrayList<>();
        }
        mHistoryAdapter.setHistoryResults(mDiceResults);
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private void initializeFirebase() {
        mUserID = Constants.INSTANCE.getFIREBASE_ANONYMOUS();
        //Auth setup
        mFirebaseAuth = FirebaseAuth.getInstance();

        //Database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mBaseDatabaseReference = mFirebaseDatabase.getReference();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) { //Signed in
                    onSignedInInitialize(user.getUid());
                } else { //Signed out, finish activity
                    onSignedOutCleanup();
                    finish();
                }
            }
        };
    }

    /**
     * A helper method for when signed into FirebaseAuth
     *
     * @param userID to set the Activity's member variable
     */
    private void onSignedInInitialize(String userID) {
        mUserID = userID;
        attachDatabaseHistoryReadListener();
    }

    /**
     * A helper method for when signed out of FirebaseAuth
     */
    private void onSignedOutCleanup() {
        mUserID = Constants.INSTANCE.getFIREBASE_ANONYMOUS();
        detachDatabaseHistoryReadListener();
    }

    /**
     * A helper method for creating the database listener that checks Firebase for DiceResults objects //TODO change to single value event listener, add onSavedInstance?
     */
    private void attachDatabaseHistoryReadListener() {
        if (mHistoryChildEventListener == null) {
            mHistoryChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    DiceResults diceResults = dataSnapshot.getValue(DiceResults.class);
                    mDiceResults.add(diceResults);
                    mHistoryAdapter.setHistoryResults(mDiceResults);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            mBaseDatabaseReference.child(Constants.INSTANCE.getFIREBASE_DATABASE_HISTORY_PATH()).child(mUserID).addChildEventListener(mHistoryChildEventListener);
        }
    }

    /**
     * A helper method to clear the database listener
     */
    private void detachDatabaseHistoryReadListener() {
        if (mHistoryChildEventListener != null) {
            mBaseDatabaseReference.child(Constants.INSTANCE.getFIREBASE_DATABASE_HISTORY_PATH()).child(mUserID).removeEventListener(mHistoryChildEventListener);
            mHistoryChildEventListener = null;
        }
    }

    /**
     * A helper method that deletes all the user's history from Firebase, and empties all views on HistoryActivity
     */
    private void clearAllHistory(){
        mBaseDatabaseReference.child(Constants.INSTANCE.getFIREBASE_DATABASE_HISTORY_PATH()).child(mUserID).setValue(null); //Delete from database

        //Reset stored values and update RecyclerView to empty
        mDiceResults = new ArrayList<>();
        mHistoryAdapter.setHistoryResults(mDiceResults);

        //Empty TextViews
        mResultsNameTextView.setText("");
        mResultsDescripTextView.setText("");
        mResultsTotalTextView.setText("");
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private void setupAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
}
