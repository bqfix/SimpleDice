package com.example.android.simpledice.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.example.android.simpledice.utils.DiceRoll;
import com.example.android.simpledice.R;
import com.example.android.simpledice.utils.DiceValidity;
import com.example.android.simpledice.utils.Utils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddFavoriteActivity extends AppCompatActivity {

    private TextView mNameHeaderTextView;
    private EditText mNameEditText;
    private EditText mFormulaEditText;
    private InputConnection mInputConnection;
    private DKeyboard mDKeyboard;
    private AdView mAdView;

    //Firebase
    private String mUserId;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mBaseDatabaseReference;

    //Included DiceRoll variables
    private boolean editingFavorite;
    private String mPreviousName;
    private String mPreviousFormula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        assignViews();

        setupAds();

        checkForIncludedDiceRoll();

        initializeFirebase();

        setListeners();
        setupFormulaEditTextAndKeyboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.add_favorite_activity_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_done):
                saveNewFavorite();
                return true;
            case (R.id.action_sign_out):
                AlertDialog.Builder builder = new AlertDialog.Builder(AddFavoriteActivity.this);
                builder.setTitle(R.string.sign_out_dialog_title)
                        .setMessage(R.string.sign_out_dialog_message)
                        .setPositiveButton(R.string.sign_out_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AuthUI.getInstance().signOut(AddFavoriteActivity.this);
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
     * A helper method for saving a new favorite
     */
    private void saveNewFavorite() {
        String name = mNameEditText.getText().toString(); //Get name and ensure it doesn't have invalid characters
        if (name.contains(Constants.INSTANCE.getNAME_FORMULA_HUNDRED_BREAK()) || name.contains(Constants.INSTANCE.getDICEROLL_BREAK())){
            Toast.makeText(this, R.string.name_contains_invalid_characters, Toast.LENGTH_SHORT).show();
            return;
        }
        //Check that formula is valid
        String formula = mFormulaEditText.getText().toString();
        DiceValidity diceValidity = Utils.INSTANCE.isValidDiceRoll(this, formula);
        if (diceValidity.isValid()) {
            DiceRoll diceRoll = new DiceRoll(name, formula, diceValidity.getHasOverHundredDice());
            if (editingFavorite) {
                diceRoll.editSavedFirebaseFavorite(mBaseDatabaseReference, mUserId, mPreviousName, mPreviousFormula);
            } else {
                diceRoll.saveNewToFirebaseFavorites(mBaseDatabaseReference, mUserId); //Simply save
            }
            Toast.makeText(this, R.string.saved_to_firebase, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, diceValidity.getErrorMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * A helper method that assigns all of the views to their initial values in onCreate
     */
    private void assignViews() {
        mNameHeaderTextView = findViewById(R.id.name_header_tv);
        mNameEditText = findViewById(R.id.name_input_et);
        mFormulaEditText = findViewById(R.id.formula_input_et);
        mDKeyboard = (DKeyboard) findViewById(R.id.d_keyboard);
        mAdView = findViewById(R.id.banner_ad);
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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mNameHeaderTextView.setVisibility(View.GONE);
            mNameEditText.setVisibility(View.GONE);
        }
    }

    /**
     * A helper method to hide the custom keyboard
     */
    private void hideCustomKeyboard() {
        if (mDKeyboard.getVisibility() == View.VISIBLE) {
            mDKeyboard.executeExitAnimation();
        }
        mNameHeaderTextView.setVisibility(View.VISIBLE);
        mNameEditText.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method to set listeners to various views, should only be called once in onCreate
     */
    private void setListeners() {
        mNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() { //FocusChange listener to minimize keyboard when clicking outside of NameEditText
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                hideSystemKeyboard(v);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mDKeyboard.getVisibility() == View.VISIBLE) { //Override to hide custom keyboard if visible when back pressed
            hideCustomKeyboard();
            mFormulaEditText.clearFocus();
            return;
        }
        super.onBackPressed();
    }

    /**
     * A helper method to setup the Formula EditText and Keyboard, to be called once in onCreate
     */
    private void setupFormulaEditTextAndKeyboard() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { //Use custom keyboard if the Android version is over 21 (this is when showSoftInputOnFocus was implemented)
            mFormulaEditText.setShowSoftInputOnFocus(false);

            mInputConnection = mFormulaEditText.onCreateInputConnection(new EditorInfo());
            mDKeyboard.setInputConnection(mInputConnection);

            mFormulaEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() { //FocusChange listener to show custom keyboard, and minimize keyboard when clicking outside of EditText
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showCustomKeyboard();
                    } else {
                        hideCustomKeyboard();
                    }
                }
            });

            mFormulaEditText.setOnClickListener(new View.OnClickListener() { //Necessary to make the custom keyboard visible if focus has not been lost, but the keyboard is minimized
                @Override
                public void onClick(View v) {
                    if (mDKeyboard.getVisibility() != View.VISIBLE) {
                        showCustomKeyboard();
                    }
                }
            });
        } else { //Use basic system keyboard
            mFormulaEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() { //FocusChange listener to minimize keyboard when clicking outside of EditText
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSystemKeyboard(v);
                    }
                }
            });
        }
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private void initializeFirebase(){
        //Auth setup
        mFirebaseAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) { //Signed in
                    mUserId = user.getUid();
                } else { //Signed out, finish activity
                    mUserId = null;
                    finish();
                }
            }
        };

        //Database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mBaseDatabaseReference = mFirebaseDatabase.getReference();
    }

    /**
     * A helper method that checks if a DiceRoll was sent in the intent, and sets up the activity
     * for editing or adding a new DiceRoll accordingly
     */
    private void checkForIncludedDiceRoll(){
        Intent intent = getIntent();
        editingFavorite = intent.hasExtra(getString(R.string.dice_roll_parcelable_key));
        if (editingFavorite){
            setTitle(R.string.edit_favorite_activity_title);

            //Get DiceRoll and populate fields
            DiceRoll diceRoll = intent.getParcelableExtra(getString(R.string.dice_roll_parcelable_key));
            mPreviousName = diceRoll.getName();
            mPreviousFormula = diceRoll.getFormula();
            mNameEditText.setText(mPreviousName);
            mFormulaEditText.setText(mPreviousFormula);
        } else {
            setTitle(R.string.add_favorite_activity_title);
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
