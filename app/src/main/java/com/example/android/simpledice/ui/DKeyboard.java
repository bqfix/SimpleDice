package com.example.android.simpledice.ui;

import android.content.Context;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.android.simpledice.R;

public class DKeyboard extends ConstraintLayout implements View.OnClickListener, View.OnLongClickListener{

    private Button mOneButton, mTwoButton, mThreeButton, mFourButton,
            mFiveButton, mSixButton, mSevenButton, mEightButton,
            mNineButton, mZeroButton, mDButton, mPlusButton,
            mMinusButton, mEnterButton;
    private ImageButton mDeleteButton;
    private final int MILLIS_BETWEEN_CHARACTER_CHANGE = 50;

    private SparseArray<String> keyValues = new SparseArray<>();
    private InputConnection mInputConnection;
    private Animation.AnimationListener mExitAnimationListener;
    private Animation.AnimationListener mEnterAnimationListener;

    public DKeyboard(Context context) {
        super(context);
        init(context);
    }

    public DKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.d_keyboard, this, true);

        //Assign view
        mOneButton = (Button) findViewById(R.id.one_button);
        mTwoButton = (Button) findViewById(R.id.two_button);
        mThreeButton = (Button) findViewById(R.id.three_button);
        mFourButton = (Button) findViewById(R.id.four_button);
        mFiveButton = (Button) findViewById(R.id.five_button);
        mSixButton = (Button) findViewById(R.id.six_button);
        mSevenButton = (Button) findViewById(R.id.seven_button);
        mEightButton = (Button) findViewById(R.id.eight_button);
        mNineButton = (Button) findViewById(R.id.nine_button);
        mZeroButton = (Button) findViewById(R.id.zero_button);
        mPlusButton = (Button) findViewById(R.id.plus_button);
        mMinusButton = (Button) findViewById(R.id.minus_button);
        mDButton = (Button) findViewById(R.id.d_button);
        mDeleteButton = (ImageButton) findViewById(R.id.delete_button);
        mEnterButton = (Button) findViewById(R.id.enter_button);

        //Assign clickListeners
        mOneButton.setOnClickListener(this);
        mTwoButton.setOnClickListener(this);
        mThreeButton.setOnClickListener(this);
        mFourButton.setOnClickListener(this);
        mFiveButton.setOnClickListener(this);
        mSixButton.setOnClickListener(this);
        mSevenButton.setOnClickListener(this);
        mEightButton.setOnClickListener(this);
        mNineButton.setOnClickListener(this);
        mZeroButton.setOnClickListener(this);
        mPlusButton.setOnClickListener(this);
        mMinusButton.setOnClickListener(this);
        mDButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mEnterButton.setOnClickListener(this);

        //Assign longClickListeners to everything but Enter
        mOneButton.setOnLongClickListener(this);
        mTwoButton.setOnLongClickListener(this);
        mThreeButton.setOnLongClickListener(this);
        mFourButton.setOnLongClickListener(this);
        mFiveButton.setOnLongClickListener(this);
        mSixButton.setOnLongClickListener(this);
        mSevenButton.setOnLongClickListener(this);
        mEightButton.setOnLongClickListener(this);
        mNineButton.setOnLongClickListener(this);
        mZeroButton.setOnLongClickListener(this);
        mPlusButton.setOnLongClickListener(this);
        mMinusButton.setOnLongClickListener(this);
        mDButton.setOnLongClickListener(this);
        mDeleteButton.setOnLongClickListener(this);

        //Populate SparseArray with key value pairs
        keyValues.put(R.id.one_button, "1");
        keyValues.put(R.id.two_button, "2");
        keyValues.put(R.id.three_button, "3");
        keyValues.put(R.id.four_button, "4");
        keyValues.put(R.id.five_button, "5");
        keyValues.put(R.id.six_button, "6");
        keyValues.put(R.id.seven_button, "7");
        keyValues.put(R.id.eight_button, "8");
        keyValues.put(R.id.nine_button, "9");
        keyValues.put(R.id.zero_button, "0");
        keyValues.put(R.id.plus_button, "+");
        keyValues.put(R.id.minus_button, "-");
        keyValues.put(R.id.d_button, "d");

        //Assign the animation listeners
        mExitAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //Do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                DKeyboard.this.clearAnimation(); //Clear the animation to allow for subsequent adjusting of view's visibility
                DKeyboard.this.setVisibility(View.GONE); //Necessary, lest the view be clickable
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //Do nothing
            }
        };

        mEnterAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //Do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                DKeyboard.this.clearAnimation(); //Clear the animation to allow for subsequent adjusting of the view's visibility
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //Do nothing
            }
        };
    }

    @Override
    public void onClick(View v) {
        if (mInputConnection == null) return;

        int viewId = v.getId();
        switch (viewId) {
            case (R.id.delete_button): { //Special delete logic

                deleteText();

                break;
            }
            case(R.id.enter_button) : { //Special enter logic
                DKeyboard.this.executeExitAnimation();
                break;
            }
            default : { //Other buttons simply append the value of the button, accessed from keyValues
                String value = keyValues.get(viewId);
                mInputConnection.commitText(value, 1);
            }
        }
    }

    @Override
    public boolean onLongClick(final View v) {
        if (mInputConnection == null) return true;
        final int viewId = v.getId();
        switch(viewId) {
            case(R.id.delete_button) : { //Special delete logic, continuous delete
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (v.isPressed()){
                            deleteText();
                            handler.postDelayed(this, MILLIS_BETWEEN_CHARACTER_CHANGE);
                        }
                    }
                };
                handler.postDelayed(runnable, MILLIS_BETWEEN_CHARACTER_CHANGE);
                return true;
            }
            default: { //Otherwise, continuous insertion of character
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (v.isPressed()){
                            String value = keyValues.get(viewId);
                            mInputConnection.commitText(value, 1);
                            handler.postDelayed(this, MILLIS_BETWEEN_CHARACTER_CHANGE);
                        }
                    }
                };
                handler.postDelayed(runnable, MILLIS_BETWEEN_CHARACTER_CHANGE);
                return true;
            }
        }
    }

    private void deleteText(){
        CharSequence selectedText = mInputConnection.getSelectedText(0);

        if (TextUtils.isEmpty(selectedText)) { //If not highlighted, delete 1 before, 0 after
            mInputConnection.deleteSurroundingText(1, 0);
        } else { //Simply replace highlighted text with empty string
            mInputConnection.commitText("", 1);
        }

    }

    public void setInputConnection(InputConnection inputConnection) {
        mInputConnection = inputConnection;
    }

    public void executeExitAnimation(){
        Animation exitAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_exit);
        exitAnimation.setAnimationListener(mExitAnimationListener);
        DKeyboard.this.startAnimation(exitAnimation);
    }

    public void executeEnterAnimation(){
        DKeyboard.this.setVisibility(View.VISIBLE);
        Animation enterAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_enter);
        enterAnimation.setAnimationListener(mEnterAnimationListener);
        DKeyboard.this.startAnimation(enterAnimation);
    }
}
