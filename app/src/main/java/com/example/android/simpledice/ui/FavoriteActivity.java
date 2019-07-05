package com.example.android.simpledice.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.android.simpledice.utils.Constants;
import com.example.android.simpledice.utils.DiceResults;
import com.example.android.simpledice.utils.DiceRoll;
import com.example.android.simpledice.R;
import com.example.android.simpledice.utils.RollAsyncTask;
import com.example.android.simpledice.utils.Utils;
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

public class FavoriteActivity extends AppCompatActivity implements FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler, FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute {

    private TextView mResultsNameTextView;
    private TextView mResultsTotalTextView;
    private TextView mResultsDescripTextView;
    private RecyclerView mRecyclerView;
    private FavoriteDiceRollAdapter mFavoriteDiceRollAdapter;
    private FloatingActionButton mAddFavoriteFAB;
    private AdView mAdView;

    private boolean widgetIntentHandled = false;

    private List<DiceRoll> mDiceRolls;

    //Firebase
    private String mUserID;
    //Auth
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mBaseDatabaseReference;
    private ChildEventListener mFavoriteChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        setTitle(R.string.favorites_title);

        assignViews();

        setupAds();

        restoreSavedInstanceState(savedInstanceState);

        initializeFirebase();

        setupRecyclerView();

        mAddFavoriteFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FavoriteActivity.this, AddFavoriteActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMostRecentDiceResults();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        handleWidgetIntent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseFavoritesReadListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(getString(R.string.widget_intent_handled_boolean_key), widgetIntentHandled);
    }

    /**
     * A helper method that assigns all of the views to their initial values in onCreate
     */
    private void assignViews() {
        mResultsNameTextView = findViewById(R.id.results_name_tv);
        mResultsTotalTextView = findViewById(R.id.results_total_tv);
        mResultsDescripTextView = findViewById(R.id.results_descrip_tv);
        mRecyclerView = findViewById(R.id.favorite_rv);
        mAddFavoriteFAB = findViewById(R.id.add_favorite_fab);
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
     * Override of FavoriteDiceRoll click method
     *
     * @param favoriteDiceRoll the clicked DiceRoll to be used
     */
    @Override
    public void onItemClick(DiceRoll favoriteDiceRoll) {
        new RollAsyncTask(this).execute(favoriteDiceRoll);
    }

    @Override
    public void onDeleteClick(final DiceRoll favoriteDiceRoll) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setPositiveButton(R.string.delete_dialog_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favoriteDiceRoll.deleteDiceRoll(mBaseDatabaseReference, mUserID);
                    }
                })
                .setNegativeButton(R.string.delete_dialog_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    /**
     * Helper method to setup RecyclerView, should only be called once in onCreate
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mFavoriteDiceRollAdapter = new FavoriteDiceRollAdapter(this, this);
        mRecyclerView.setAdapter(mFavoriteDiceRollAdapter);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (mDiceRolls == null) {
            mDiceRolls = new ArrayList<>();
        }
        mFavoriteDiceRollAdapter.setFavoriteDiceRolls(mDiceRolls);
    }

    /**
     * A helper method to load the most recent dice results into the results views.
     * Called in onStart, as it is cheaper than using a listener, since any new rolls occuring while this is the foreground activity will be updated directly.
     */
    private void loadMostRecentDiceResults() {
        DiceResults diceResults = Utils.INSTANCE.retrieveLatestDiceResults(this);

        setDataToResultsViews(diceResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.favorite_activity_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_history):
                Intent intent = new Intent(FavoriteActivity.this, HistoryActivity.class);
                startActivity(intent);
                return true;
            case (R.id.action_sign_out):
                AlertDialog.Builder builder = new AlertDialog.Builder(FavoriteActivity.this);
                builder.setTitle(R.string.sign_out_dialog_title)
                        .setMessage(R.string.sign_out_dialog_message)
                        .setPositiveButton(R.string.sign_out_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AuthUI.getInstance().signOut(FavoriteActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.sign_out_dialog_negative, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private void initializeFirebase() {
        mUserID = Constants.INSTANCE.getFIREBASE_ANONYMOUS();
        //Auth setup
        mFirebaseAuth = FirebaseAuth.getInstance();

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

        //Database Setup
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mBaseDatabaseReference = mFirebaseDatabase.getReference();
    }

    /**
     * A helper method for when signed into FirebaseAuth
     *
     * @param userID to set the Activity's member variable
     */
    private void onSignedInInitialize(String userID) {
        mUserID = userID;
        attachDatabaseFavoritesReadListener();
    }

    /**
     * A helper method for when signed out of FirebaseAuth
     */
    private void onSignedOutCleanup() {
        mUserID = Constants.INSTANCE.getFIREBASE_ANONYMOUS();
        detachDatabaseFavoritesReadListener();
    }

    /**
     * A helper method for creating the database listener that checks Firebase for DiceRoll objects
     */
    private void attachDatabaseFavoritesReadListener() {
        mDiceRolls = new ArrayList<>(); //Reset mDiceRolls, or edits to the Database cause repeat data
        if (mFavoriteChildEventListener == null) {
            mFavoriteChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    DiceRoll diceRoll = dataSnapshot.getValue(DiceRoll.class);
                    mDiceRolls.add(diceRoll);
                    mFavoriteDiceRollAdapter.setFavoriteDiceRolls(mDiceRolls);
                    Utils.INSTANCE.updateAllWidgets(FavoriteActivity.this, mDiceRolls);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    DiceRoll deletedDiceRoll = dataSnapshot.getValue(DiceRoll.class);
                    for (DiceRoll diceRoll : mDiceRolls) {
                        if (deletedDiceRoll.getName().equals(diceRoll.getName()) && deletedDiceRoll.getFormula().equals(diceRoll.getFormula())) { //If the name and formula match, delete it
                            mDiceRolls.remove(diceRoll);
                            mFavoriteDiceRollAdapter.setFavoriteDiceRolls(mDiceRolls);
                            Utils.INSTANCE.updateAllWidgets(FavoriteActivity.this, mDiceRolls);
                            break; //Prevent removing more than one diceRoll
                        }
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            mBaseDatabaseReference.child(Constants.INSTANCE.getFIREBASE_DATABASE_FAVORITES_PATH()).child(mUserID).addChildEventListener(mFavoriteChildEventListener);
        }
    }

    /**
     * A helper method to clear the database listener
     */
    private void detachDatabaseFavoritesReadListener() {
        if (mFavoriteChildEventListener != null) {
            mBaseDatabaseReference.child(Constants.INSTANCE.getFIREBASE_DATABASE_FAVORITES_PATH()).child(mUserID).removeEventListener(mFavoriteChildEventListener);
            mFavoriteChildEventListener = null;
        }
    }

    /**
     * A helper method to handle Intents coming from the widget that contain a DiceRoll to be rolled
     * Called in onCreate.
     */
    private void handleWidgetIntent(){
        if (!widgetIntentHandled) {
            Intent intent = getIntent();
            String parcelableKey = getString(R.string.widget_favorites_intent_parcelable_key);
            if (intent.hasExtra(parcelableKey)) { //If available, retrieve DiceRoll
                DiceRoll diceRoll = intent.getParcelableExtra(parcelableKey);
                new RollAsyncTask(this).execute(diceRoll);
            }
            widgetIntentHandled = true;
        }
    }

    /** Override, for any given diceResults that occur from rolling a DiceRoll, up
     *
     * @param diceResults to use
     */
    @Override
    public void handleRollResult(DiceResults diceResults) {
        diceResults.saveToSharedPreferences(this);
        setDataToResultsViews(diceResults);
        diceResults.saveToFirebaseHistory(mBaseDatabaseReference, mUserID);
    }

    /** A helper method for handling saved instance states in onCreate
     *
     * @param savedInstanceState to handle
     */
    private void restoreSavedInstanceState(Bundle savedInstanceState){
        if (savedInstanceState != null && !savedInstanceState.isEmpty()){
            widgetIntentHandled = savedInstanceState.getBoolean(getString(R.string.widget_intent_handled_boolean_key), false);
        }
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private void setupAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
}
