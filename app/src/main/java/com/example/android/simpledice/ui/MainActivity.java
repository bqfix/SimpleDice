package com.example.android.simpledice.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.simpledice.utils.Constants;
import com.example.android.simpledice.utils.DiceResults;
import com.example.android.simpledice.utils.DiceRoll;
import com.example.android.simpledice.R;
import com.example.android.simpledice.utils.DiceValidity;
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
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler, FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute {

    //View variables
    private EditText mCommandInputEditText;
    private MaterialButton mRollButton;
    private TextView mResultsNameTextView;
    private TextView mResultsTotalTextView;
    private TextView mResultsDescripTextView;
    private RecyclerView mFavoriteRecyclerView;
    private FavoriteDiceRollAdapter mFavoriteDiceRollAdapter;
    private MaterialButton mAllFavoritesButton;
    private MaterialButton mHelpButton;
    private DKeyboard mDKeyboard;
    private MaterialButton mClearButton;
    private AdView mAdView;

    private List<DiceRoll> mDiceRolls;

    //InputConnection for custom keyboard
    private InputConnection mCommandInputConnection;

    //Firebase variables
    private String mUserID;
    //Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mBaseDatabaseReference;
    private ChildEventListener mFavoriteChildEventListener;
    //Auth
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();

        assignViews();

        setupAds();

        setOnClickListeners();

        setupFavoriteRecyclerView();

        setupEditTextAndKeyboard();

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseFavoritesReadListener();
    }

    /**
     * A helper method that assigns all of the views to their initial values in onCreate
     */
    private void assignViews() {
        mCommandInputEditText = findViewById(R.id.command_input_et);
        mRollButton = findViewById(R.id.roll_button);
        mResultsNameTextView = findViewById(R.id.results_name_tv);
        mResultsTotalTextView = findViewById(R.id.results_total_tv);
        mResultsDescripTextView = findViewById(R.id.results_descrip_tv);
        mFavoriteRecyclerView = findViewById(R.id.main_favorite_rv);
        mAllFavoritesButton = findViewById(R.id.favorites_button);
        mHelpButton = findViewById(R.id.main_help_button);
        mDKeyboard = (DKeyboard) findViewById(R.id.d_keyboard);
        mClearButton = findViewById(R.id.clear_button);
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
     * A helper method to hide the system keyboard
     *
     * @param view used to get the window token (passed in from listener)
     */
    private void hideSystemKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * A helper method to show the custom keyboard
     */
    private void showCustomKeyboard() {
        if (mDKeyboard.getVisibility() == View.GONE) {
            mDKeyboard.executeEnterAnimation();
        }
    }

    /**
     * A helper method to hide the custom keyboard
     */
    private void hideCustomKeyboard() {
        if (mDKeyboard.getVisibility() == View.VISIBLE) {
            mDKeyboard.executeExitAnimation();
        }
    }

    /**
     * Override of FavoriteDiceRoll click method for RecyclerView
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
     * Helper method to setup FavoriteRecyclerView, should only be called once in onCreate
     */
    private void setupFavoriteRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mFavoriteRecyclerView.setLayoutManager(layoutManager);

        mFavoriteDiceRollAdapter = new FavoriteDiceRollAdapter(this, this);
        mFavoriteRecyclerView.setAdapter(mFavoriteDiceRollAdapter);

        mFavoriteRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (mDiceRolls == null) {
            mDiceRolls = new ArrayList<>();
        }
        mFavoriteDiceRollAdapter.setFavoriteDiceRolls(mDiceRolls);
    }

    /**
     * Helper method to set click listeners to various views, should only be called once in onCreate
     */
    private void setOnClickListeners() {
        mAllFavoritesButton.setOnClickListener(new View.OnClickListener() { //Click listener to launch FavoritesActivity
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
                startActivity(intent);
            }
        });

        mRollButton.setOnClickListener(new View.OnClickListener() { //Click listener to check the validity of a roll, and execute it if it is acceptable
            @Override
            public void onClick(View v) {
                String formula = mCommandInputEditText.getText().toString();
                DiceValidity diceValidity = Utils.isValidDiceRoll(MainActivity.this, formula); //Get a boolean of whether the
                if (diceValidity.isValid()) {
                    //If formula is okay, make a new nameless DiceRoll for display in the results text
                    DiceRoll diceRoll = new DiceRoll(formula, diceValidity.hasOverHundredDice());

                    new RollAsyncTask(MainActivity.this).execute(diceRoll);

                    //Hide keyboards
                    hideSystemKeyboard(v);
                    hideCustomKeyboard();
                } else {
                    Toast.makeText(MainActivity.this, diceValidity.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setPositiveButton(R.string.help_dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //No special action needed, simply dismisses the dialog.
                    }
                })
                        .setMessage(R.string.help_formula_advice)
                        .setTitle(R.string.help_header);
                builder.show();
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommandInputEditText.setText("");
            }
        });
    }

    /**
     * A helper method to load the most recent dice results into the results views.
     * Called in onStart, as it is cheaper than using a listener, since any new rolls occuring while this is the foreground activity will be updated directly.
     */
    private void loadMostRecentDiceResults() {
        DiceResults diceResults = Utils.retrieveLatestDiceResults(this);
        setDataToResultsViews(diceResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_about_menu):
                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case (R.id.action_history):
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
                return true;
            case (R.id.action_sign_out):
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.sign_out_dialog_title)
                        .setMessage(R.string.sign_out_dialog_message)
                        .setPositiveButton(R.string.sign_out_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AuthUI.getInstance().signOut(MainActivity.this);
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
     * A helper method to setup the EditText and Keyboard, to be called once in onCreate
     */
    private void setupEditTextAndKeyboard() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { //Use custom keyboard if the Android version is over 21 (this is when showSoftInputOnFocus was implemented)
            mCommandInputEditText.setShowSoftInputOnFocus(false);

            mCommandInputConnection = mCommandInputEditText.onCreateInputConnection(new EditorInfo());
            mDKeyboard.setInputConnection(mCommandInputConnection);

            mCommandInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() { //FocusChange listener to show custom keyboard, and minimize keyboard when clicking outside of EditText
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showCustomKeyboard();
                    } else {
                        hideCustomKeyboard();
                    }
                }
            });

            mCommandInputEditText.setOnClickListener(new View.OnClickListener() { //Necessary to make the custom keyboard visible if focus has not been lost, but the keyboard is minimized
                @Override
                public void onClick(View v) {
                    if (mDKeyboard.getVisibility() != View.VISIBLE) {
                        showCustomKeyboard();
                    }
                }
            });
        } else { //Use basic system keyboard
            mCommandInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() { //FocusChange listener to minimize keyboard when clicking outside of EditText
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSystemKeyboard(v);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mDKeyboard.getVisibility() == View.VISIBLE) { //Override to hide custom keyboard if visible when back pressed
            hideCustomKeyboard();
            mCommandInputEditText.clearFocus();
            return;
        }
        super.onBackPressed();
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private void initializeFirebase() {
        //Auth setup
        mUserID = Constants.FIREBASE_ANONYMOUS;
        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) { //Signed in
                    onSignedInInitialize(user.getUid());
                } else { //Signed out, so launch sign in activity
                    onSignedOutCleanup();
                    startActivityForResult(AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            Constants.REQUEST_CODE_SIGN_IN);
                }
            }
        };

        //Database setup
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
        mUserID = Constants.FIREBASE_ANONYMOUS;
        detachDatabaseFavoritesReadListener();

        //Blank Favorites
        mDiceRolls = new ArrayList<>();
        mFavoriteDiceRollAdapter.setFavoriteDiceRolls(mDiceRolls);

        //Blank Results(by blanking SharedPreferences)
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(getString(R.string.dice_results_name_key), "");
        editor.putString(getString(R.string.dice_results_descrip_key), "");
        editor.putLong(getString(R.string.dice_results_total_key), 0);
        editor.putLong(getString(R.string.dice_results_date_key), 0);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_SIGN_IN) { //Special logic for results coming from FirebaseUI sign-in
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.sign_in_success, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) { //User canceled, finish(); to close app
                finish();
            }
        }
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
                    Utils.updateAllWidgets(MainActivity.this, mDiceRolls);
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
                            Utils.updateAllWidgets(MainActivity.this, mDiceRolls);
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
            mBaseDatabaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(mUserID).addChildEventListener(mFavoriteChildEventListener);
        }
    }

    /**
     * A helper method to clear the database listener
     */
    private void detachDatabaseFavoritesReadListener() {
        if (mFavoriteChildEventListener != null) {
            mBaseDatabaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(mUserID).removeEventListener(mFavoriteChildEventListener);
            mFavoriteChildEventListener = null;
        }
    }

    /**
     * Override, for any given diceResults that occur from rolling a DiceRoll
     *
     * @param diceResults to use
     */
    @Override
    public void handleRollResult(DiceResults diceResults) {
        diceResults.saveToSharedPreferences(this);
        setDataToResultsViews(diceResults);
        diceResults.saveToFirebaseHistory(mBaseDatabaseReference, mUserID);
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private void setupAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
}
