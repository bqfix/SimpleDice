package com.gmail.maxfixsoftware.simpledice.ui

import android.content.Context
import android.os.Handler
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputConnection
import androidx.constraintlayout.widget.ConstraintLayout
import com.gmail.maxfixsoftware.simpledice.R
import com.gmail.maxfixsoftware.simpledice.utils.Constants.MILLIS_BETWEEN_CHARACTER_CHANGE
import kotlinx.android.synthetic.main.d_keyboard.view.*

class DKeyboard : ConstraintLayout, View.OnClickListener, View.OnLongClickListener {

    private val keyValues = SparseArray<String>()
    private var mInputConnection: InputConnection? = null
    private var mExitAnimationListener: Animation.AnimationListener? = null
    private var mEnterAnimationListener: Animation.AnimationListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.d_keyboard, this, true)

        //Assign clickListeners
        one_button.setOnClickListener(this)
        two_button.setOnClickListener(this)
        three_button.setOnClickListener(this)
        four_button.setOnClickListener(this)
        five_button.setOnClickListener(this)
        six_button.setOnClickListener(this)
        seven_button.setOnClickListener(this)
        eight_button.setOnClickListener(this)
        nine_button.setOnClickListener(this)
        zero_button.setOnClickListener(this)
        plus_button.setOnClickListener(this)
        minus_button.setOnClickListener(this)
        d_button.setOnClickListener(this)
        delete_button.setOnClickListener(this)
        enter_button.setOnClickListener(this)

        //Assign longClickListeners to everything but Enter
        one_button.setOnLongClickListener(this)
        two_button.setOnLongClickListener(this)
        three_button.setOnLongClickListener(this)
        four_button.setOnLongClickListener(this)
        five_button.setOnLongClickListener(this)
        six_button.setOnLongClickListener(this)
        seven_button.setOnLongClickListener(this)
        eight_button.setOnLongClickListener(this)
        nine_button.setOnLongClickListener(this)
        zero_button.setOnLongClickListener(this)
        plus_button.setOnLongClickListener(this)
        minus_button.setOnLongClickListener(this)
        d_button.setOnLongClickListener(this)
        delete_button.setOnLongClickListener(this)

        //Populate SparseArray with key value pairs
        keyValues.put(R.id.one_button, "1")
        keyValues.put(R.id.two_button, "2")
        keyValues.put(R.id.three_button, "3")
        keyValues.put(R.id.four_button, "4")
        keyValues.put(R.id.five_button, "5")
        keyValues.put(R.id.six_button, "6")
        keyValues.put(R.id.seven_button, "7")
        keyValues.put(R.id.eight_button, "8")
        keyValues.put(R.id.nine_button, "9")
        keyValues.put(R.id.zero_button, "0")
        keyValues.put(R.id.plus_button, "+")
        keyValues.put(R.id.minus_button, "-")
        keyValues.put(R.id.d_button, "d")

        //Assign the animation listeners
        mExitAnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                //Do nothing
            }

            override fun onAnimationEnd(animation: Animation) {
                this@DKeyboard.clearAnimation() //Clear the animation to allow for subsequent adjusting of view's visibility
                this@DKeyboard.visibility = View.GONE //Necessary, lest the view be clickable
            }

            override fun onAnimationRepeat(animation: Animation) {
                //Do nothing
            }
        }

        mEnterAnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                //Do nothing
            }

            override fun onAnimationEnd(animation: Animation) {
                this@DKeyboard.clearAnimation() //Clear the animation to allow for subsequent adjusting of the view's visibility
            }

            override fun onAnimationRepeat(animation: Animation) {
                //Do nothing
            }
        }
    }

    override fun onClick(v: View) {
        if (mInputConnection == null) return

        val viewId = v.id
        when (viewId) {
            R.id.delete_button -> { //Special delete logic

                deleteText()
            }
            R.id.enter_button -> { //Special enter logic
                this@DKeyboard.executeExitAnimation()
            }
            else -> { //Other buttons simply append the value of the button, accessed from keyValues
                val value = keyValues.get(viewId)
                mInputConnection!!.commitText(value, 1)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (mInputConnection == null) return true
        val viewId = v.id
        when (viewId) {
            R.id.delete_button -> { //Special delete logic, continuous delete
                val handler = Handler()
                val runnable = object : Runnable {
                    override fun run() {
                        if (v.isPressed) {
                            deleteText()
                            handler.postDelayed(this, MILLIS_BETWEEN_CHARACTER_CHANGE.toLong())
                        }
                    }
                }
                handler.postDelayed(runnable, MILLIS_BETWEEN_CHARACTER_CHANGE.toLong())
                return true
            }
            else -> { //Otherwise, continuous insertion of character
                val handler = Handler()
                val runnable = object : Runnable {
                    override fun run() {
                        if (v.isPressed) {
                            val value = keyValues.get(viewId)
                            mInputConnection!!.commitText(value, 1)
                            handler.postDelayed(this, MILLIS_BETWEEN_CHARACTER_CHANGE.toLong())
                        }
                    }
                }
                handler.postDelayed(runnable, MILLIS_BETWEEN_CHARACTER_CHANGE.toLong())
                return true
            }
        }
    }

    private fun deleteText() {
        val selectedText = mInputConnection!!.getSelectedText(0)

        if (TextUtils.isEmpty(selectedText)) { //If not highlighted, delete 1 before, 0 after
            mInputConnection!!.deleteSurroundingText(1, 0)
        } else { //Simply replace highlighted text with empty string
            mInputConnection!!.commitText("", 1)
        }

    }

    fun setInputConnection(inputConnection: InputConnection) {
        mInputConnection = inputConnection
    }

    fun executeExitAnimation() {
        val exitAnimation = AnimationUtils.loadAnimation(context, R.anim.bottom_exit)
        exitAnimation.setAnimationListener(mExitAnimationListener)
        this@DKeyboard.startAnimation(exitAnimation)
    }

    fun executeEnterAnimation() {
        this@DKeyboard.visibility = View.VISIBLE
        val enterAnimation = AnimationUtils.loadAnimation(context, R.anim.bottom_enter)
        enterAnimation.setAnimationListener(mEnterAnimationListener)
        this@DKeyboard.startAnimation(enterAnimation)
    }
}
